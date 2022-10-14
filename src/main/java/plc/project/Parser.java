package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have its own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        try {
            List<Ast.Global> globTokens = new ArrayList<>();
            List<Ast.Function> funcTokens = new ArrayList<>();

            while (tokens.has(0))
            {
                if (peek("LIST") || peek("VAR") || peek("VAL"))
                {
                    Ast.Global globalToken = parseGlobal();
                    globTokens.add(globalToken);
                }
                else if (peek("FUN"))
                {
                    Ast.Function functionToken = parseFunction();
                    funcTokens.add(functionToken);
                }
            }
            return new Ast.Source(globTokens, funcTokens);
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        try {
            if (match("LIST"))
            {
                return parseList();
            }
            if (match("VAR"))
            {
                return parseMutable();
            }
            else
            {
                match("VAL");
                return parseImmutable();
            }
        } catch (ParseException e) {
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        try
        {
            if (!peek(Token.Type.IDENTIFIER, "=", "["))
            {
                throw new ParseException("Invalid List Statement", tokens.get(-1).getIndex());
            }
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER, "=", "[");

            Ast.Expression expr1 = parseExpression();
            List<Ast.Expression> expressions = new ArrayList<>();
            expressions.add(expr1);

            while (peek(",")) {
                match(",");
                Ast.Expression argument = parseExpression();
                expressions.add(argument);
            }

            if (match("]")) {
                if (match(";"))
                {
                    //return new Ast.Global(name, true, Optional.ofNullable(expressions));
                    return new Ast.Global(name, true, Optional.of(expr1));
                }
                else
                {
                    throw new ParseException("Missing Semicolon", tokens.get(-1).getIndex());
                }
            } else {
                throw new ParseException("Missing Bracket", tokens.get(-1).getIndex());
            }


        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        try
        {
            if (!peek(Token.Type.IDENTIFIER))
            {
                throw new ParseException("Invalid List Statement", tokens.get(-1).getIndex());
            }
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);

            Ast.Expression expr = null;
            if (match("="))
            {
                expr = parseExpression();
            }

            if (!match(";"))
            {
                throw new ParseException("Missing Semicolon", tokens.get(-1).getIndex());
            }
            else
            {
                return new Ast.Global(name, true, Optional.of(expr));
            }
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        try
        {
            if (!peek(Token.Type.IDENTIFIER))
            {
                throw new ParseException("Invalid List Statement", tokens.get(-1).getIndex());
            }
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);


            if (!match("="))
            {
                throw new ParseException("Missing Equality", tokens.get(-1).getIndex());
            }

            Ast.Expression expr = parseExpression();

            if (!match(";"))
            {
                throw new ParseException("Missing Semicolon", tokens.get(-1).getIndex());
            }
            else
            {
                return new Ast.Global(name, true, Optional.of(expr));
            }
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        try
        {
            match("FUN");
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER, "(");
            List<String> func = new ArrayList<>();

            if (match(Token.Type.IDENTIFIER))
            {
                func.add(tokens.get(-1).getLiteral());
            }
            while (match(","))
            {
                if (!peek(Token.Type.IDENTIFIER))
                {
                    throw new ParseException("Trailing Comma", tokens.get(-1).getIndex());
                }
                func.add(tokens.get(0).getLiteral());
            }

            List<Ast.Statement> exp = new ArrayList<>();
            if (match(")", "DO")) {
                exp = parseBlock();
                if (!match("END"))
                {
                    throw new ParseException("No END", tokens.get(-1).getIndex());
                }
            } else {
                throw new ParseException("Missing Parenthesis", tokens.get(-1).getIndex());
            }
            return new Ast.Function(name, func, exp);
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        try
        {
            List<Ast.Statement> block = new ArrayList<>();
            while (!peek("END") && !peek("LIST") && !peek("VAR")
                    && !peek("VAL") && !peek("FUN"))
            {
                block.add(parseStatement());
            }
            return block;
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        try {
            if (peek("LET")){
                match("LET");

                return parseDeclarationStatement();
            }
            else if (peek("SWITCH")){
                match("SWITCH");

                return parseSwitchStatement();

            }
            else if (peek("IF")){
                match("IF");

                return parseIfStatement();

            }
            else if (peek("WHILE")){
                match("WHILE");

                return parseWhileStatement();

            }
            else if (peek("RETURN")){
                match("RETURN");

                return parseReturnStatement();

            } else {
                Ast.Expression expr1 = parseExpression();

                if (!match("=")) {
                    if (!match(";")) {
                        throw new ParseException("Missing a closing semicolon", tokens.get(-1).getIndex());
                    } else {
                        return new Ast.Statement.Expression(expr1);
                    }
                }
                Ast.Expression expr2 = parseExpression();

                if (!match(";")) {
                    throw new ParseException("Missing a closing semicolon", tokens.get(-1).getIndex());
                } else {
                    return new Ast.Statement.Assignment(expr1, expr2);
                }
            }

        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }

    }


    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        try
        {
            if (!peek(Token.Type.IDENTIFIER))
            {
                throw new ParseException("Invalid LET Statement", tokens.get(-1).getIndex());
            }
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);

            Ast.Expression expr = null;
            if (match("="))
            {
                expr = parseExpression();
            }

            if (!match(";"))
            {
                throw new ParseException("Missing Semicolon", tokens.get(-1).getIndex());
            }
            return new Ast.Statement.Declaration(name, Optional.of(expr));
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        try
        {
            Ast.Expression condition = parseExpression();
            if (!match("DO"))
            {
                throw new ParseException("Missing DO", tokens.get(-1).getIndex());
            }

            List<Ast.Statement> then = parseBlock();
            List<Ast.Statement> els = new ArrayList<>();
            if (match("ELSE"))
            {
                els = parseBlock();
            }

            if (!match("END"))
            {
                throw new ParseException("Missing END", tokens.get(-1).getIndex());
            }

            return new Ast.Statement.If(condition, then, els);
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        try
        {
            Ast.Expression condition = parseExpression();

            List<Ast.Statement.Case> cases = new ArrayList<>();
            while (match("CASE"))
            {
                cases.add(parseCaseStatement());
            }

            if (!match("DEFAULT"))
            {
                throw new ParseException("Missing DEFAULT", tokens.get(-1).getIndex());
            }

            cases.add(new Ast.Statement.Case(Optional.empty(), parseBlock()));
            if (!match("END"))
            {
                throw new ParseException("Missing END", tokens.get(-1).getIndex());
            }

            return new Ast.Statement.Switch(condition, cases);
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        try
        {
            List<Ast.Statement> states = new ArrayList<>();
            Ast.Expression exp = parseExpression();
            if (!match(":"))
            {
                throw new ParseException("Missing colon", tokens.get(-1).getIndex());
            }
            return new Ast.Statement.Case(Optional.of(exp), parseBlock());
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        try
        {
            Ast.Expression exp = parseExpression();
            if (!match("DO"))
            {
                throw new ParseException("Missing DO", tokens.get(-1).getIndex());
            }
            List<Ast.Statement> block = new ArrayList<>();
            while (!peek("END") && !peek("LIST") && !peek("VAR")
                    && !peek("VAL") && !peek("FUN"))
            {
                block.add(parseStatement());
            }

            if (!match("END"))
            {
                throw new ParseException("Missing END", tokens.get(-1).getIndex());
            }

            if (!match(";"))
            {
                throw new ParseException("Missing a closing semicolon", tokens.get(-1).getIndex());
            }
            return new Ast.Statement.While(exp, block);
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        try
        {
            Ast.Statement.Return ret = new Ast.Statement.Return(parseExpression());
            if (!match(";"))
            {
                throw new ParseException("Missing a closing semicolon", tokens.get(-1).getIndex());
            }
            return ret;
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        try {
            return parseLogicalExpression();
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        try {
            Ast.Expression comp_exp1 = parseComparisonExpression();

            while (peek("&&") || peek("||")) {
                if (match("&&")) ;
                else match("||");

                String logical_operation = tokens.get(-1).getLiteral();

                Ast.Expression comp_exp2 = parseComparisonExpression();

                comp_exp1 = new Ast.Expression.Binary(logical_operation, comp_exp1, comp_exp2);
            }

            return comp_exp1;
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }


    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        try {
            Ast.Expression add1 = parseAdditiveExpression();

            while (peek("<") || peek(">") || peek("==") || peek("!=")) {
                if (match("<"));
                else if (match(">"));
                else if (match("=="));
                else match("!=");

                String comp_operation = tokens.get(-1).getLiteral();

                Ast.Expression add2 = parseAdditiveExpression();

                add1 = new Ast.Expression.Binary(comp_operation, add1, add2);
            }

            return add1;

        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        try {
            Ast.Expression mult_expr1 = parseMultiplicativeExpression();

            while (peek("+") || peek("-")) {
                if (match("+")) ;
                else match("-");

                String additive_operation = tokens.get(-1).getLiteral();

                Ast.Expression mult_expr2 = parseMultiplicativeExpression();

                mult_expr1 = new Ast.Expression.Binary(additive_operation, mult_expr1, mult_expr2);

            }
            return mult_expr1;
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }

    }



    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        try {
            Ast.Expression primary_expr1 = parsePrimaryExpression();

            while (peek("*") || peek("/") || peek("^")) {
                if (match("*")) ;
                else if (match("/")) ;
                else match("^");

                String mult_operation = tokens.get(-1).getLiteral();

                Ast.Expression primary_expr2 = parsePrimaryExpression();

                primary_expr1 = new Ast.Expression.Binary(mult_operation, primary_expr1, primary_expr2);

            }
            return primary_expr1;
        } catch (ParseException e){
            throw new ParseException(e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public String replaceEscape(String lit)
    {
        lit = lit.replace("\\n", "\n");
        lit = lit.replace("\\b", "\b");
        lit = lit.replace("\\r", "\r");
        lit = lit.replace("\\t", "\t");
        lit = lit.replace("\\\\", "\\");
        lit = lit.replace("\\'", "\'");
        lit = lit.replace("\\", "\"");
        return lit;
    }
    public Ast.Expression.Literal literals()
    {
        if (match("NIL"))
        {
            return new Ast.Expression.Literal(null);
        }
        else if (match("TRUE"))
        {
            return new Ast.Expression.Literal(Boolean.TRUE);
        }
        else if (match("FALSE"))
        {
            new Ast.Expression.Literal(Boolean.FALSE);
        }
        else if (match(Token.Type.INTEGER))
        {
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.DECIMAL))
        {
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.CHARACTER))
        {
            String liter = tokens.get(-1).getLiteral();
            return new Ast.Expression.Literal(liter.charAt(1));
        }
        match(Token.Type.STRING);
        String result = tokens.get(-1).getLiteral();
        result = result.substring(1, result.length()-1);
        result = replaceEscape(result);

        return new Ast.Expression.Literal(result);
    }
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (peek("NIL") || peek("TRUE") || peek("FALSE") ||
                peek(Token.Type.INTEGER) || peek(Token.Type.DECIMAL) ||
                peek(Token.Type.CHARACTER) || peek(Token.Type.STRING)) {
            return literals();
        } else if (match(Token.Type.IDENTIFIER)) {
            String ident = tokens.get(-1).getLiteral();
            if (!peek("(") && !peek("[")) {
                return new Ast.Expression.Access(Optional.empty(), ident);
            } else {
               if (match("(")) {

                    if (!match(")")) {
                        Ast.Expression expr1 = parseExpression();
                        List<Ast.Expression> expressions = new ArrayList<>();
                        expressions.add(expr1);

                        while (peek(",")) {
                            match(",");
                            Ast.Expression argument = parseExpression();
                            expressions.add(argument);
                        }

                        if (peek(")")) {
                            match(")");
                            return new Ast.Expression.Function(ident, expressions);
                        } else {
                            throw new ParseException("Missing Parenthesis", tokens.get(-1).getIndex());
                        }
                    } else {
                        if (!tokens.get(-1).getLiteral().equals(")")) {
                            throw new ParseException("Missing Parenthesis", tokens.get(-1).getIndex());
                        } else {
                            return new Ast.Expression.Function(ident, new ArrayList<>());
                        }
                    }
                } else {
                   match("[");

                   Ast.Expression list_element = parseExpression();

                   if (!match("]")){
                       throw new ParseException("Missing Bracket", tokens.get(-1).getIndex());
                   }

                   Ast.Expression.Access result = new Ast.Expression.Access(Optional.of(list_element), ident);

                   return result;
                }

            }

        } else if (peek("(")) {
            match("(");
            Ast.Expression expr1 = parseExpression();
            if (!match(")")) {
                throw new ParseException("Missing Parenthesis", tokens.get(-1).getIndex());
            }
            return new Ast.Expression.Group(expr1);
        } else {
            throw new ParseException("Invalid Expression", tokens.get(-1).getIndex());
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++)
        {
            if (!tokens.has(i))
            {
                return false;
            }
            else if (patterns[i] instanceof Token.Type)
            {
                if (patterns[i] != tokens.get(i).getType())
                {
                    return false;
                }
            }
            else if (patterns[i] instanceof String)
            {
                if (!patterns[i].equals(tokens.get(i).getLiteral()))
                {
                    return false;
                }
            }
            else
            {
                throw new AssertionError("Invalid pattern object: " +
                        patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek)
        {
            for (int i = 0; i < patterns.length; i++)
            {
                tokens.advance();
            }
        }
        return peek;
    }
    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}