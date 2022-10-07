package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
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

                if (peek("=")){
                    match("=");

                    Ast.Expression expr2 = parseExpression();

                    if (!peek(";")){
                        throw new ParseException("Missing a closing semicolon", tokens.get(-1).getIndex());
                    } else {
                        return new Ast.Statement.Assignment(expr1, expr2);
                    }

                }

                else{
                    if (!peek(";")){
                        throw new ParseException("Missing a closing semicolon", tokens.get(-1).getIndex());
                    } else {
                        return new Ast.Statement.Expression(expr1);
                    }

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
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
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
        Ast.Expression comp_exp1 = parseComparisonExpression();

        while (peek("&&") || peek("||")) {
            if (match("&&"));
            else match("||");

            String logical_operation = tokens.get(-1).getLiteral();

            Ast.Expression comp_exp2 = parseComparisonExpression();

            comp_exp1 = new Ast.Expression.Binary(logical_operation, comp_exp1, comp_exp2);
        }

        return comp_exp1;


    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression add1 = parseAdditiveExpression();

        while (peek("<") || peek(">") || peek("==") || peek("!=")){
            if (match("<"));
            else if (match(">"));
            else if (match("=="));
            else match("!=");

            String comp_operation = tokens.get(-1).getLiteral();

            Ast.Expression add2 = parseAdditiveExpression();

            add1 = new Ast.Expression.Binary(comp_operation, add1, add2);
        }

        return add1;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression mult_expr1 = parseMultiplicativeExpression();

        while (peek("+") || peek("-")) {
            if (match("+"));
            else match("-");

            String additive_operation = tokens.get(-1).getLiteral();

            Ast.Expression mult_expr2 = parseMultiplicativeExpression();

            mult_expr1 = new Ast.Expression.Binary(additive_operation, mult_expr1, mult_expr2);
        }
        return mult_expr1;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression primary_expr1 = parsePrimaryExpression();

        while (peek("*") || peek("/") || peek("^")) {
            if (match("*"));
            else if (match("/"));
            else match("^");

            String mult_operation = tokens.get(-1).getLiteral();

            Ast.Expression primary_expr2 = parsePrimaryExpression();

            primary_expr1 = new Ast.Expression.Binary(mult_operation, primary_expr1, primary_expr2);
        }
        return primary_expr1;
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
            String liter = tokens.get(-1).getLiteral().replace("\'", "");
            liter = replaceEscape(liter);
            return new Ast.Expression.Literal(liter);
        }
        match(Token.Type.STRING);
        String result = tokens.get(-1).getLiteral().replace("\"", "");
        result = replaceEscape(result);

        return new Ast.Expression.Literal(result);
    }
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (peek("NIL") || peek("TRUE") || peek("FALSE") ||
                peek(Token.Type.INTEGER) || peek(Token.Type.DECIMAL) ||
                peek(Token.Type.CHARACTER) || peek(Token.Type.STRING))
        {
            return literals();
        }

        else if (match("("))
        {
            Ast.Expression.Group result = new Ast.Expression.Group(parseExpression());
            match(")");
            return result;
        }
        else if (peek(Token.Type.IDENTIFIER, "("))
        {
            match(Token.Type.IDENTIFIER, "(");
            String ident = tokens.get(-2).getLiteral();

            List<Ast.Expression> expressions = new ArrayList<Ast.Expression>();
            while (!match(")"))
            {
                expressions.add(parseExpression());
                match(",");
            }
            return new Ast.Expression.Function(ident, expressions);
        }
        else if (peek(Token.Type.IDENTIFIER, "["))
        {
            match(Token.Type.IDENTIFIER, "[");
            String listStr = tokens.get(-2).getLiteral();

            if (peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR))
            {
                String expr = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER, Token.Type.OPERATOR);
                Ast.Expression.Access result = new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), expr)), listStr);
                match(Token.Type.IDENTIFIER, "]");
                return result;
            }
        }
        else if (match(Token.Type.IDENTIFIER))
        {
            return new Ast.Expression.Access(Optional.empty(), tokens.get(-1).getLiteral());
        }
        //specify throw ParseException error here instead
        throw new UnsupportedOperationException();
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
