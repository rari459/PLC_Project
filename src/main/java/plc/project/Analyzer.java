package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        String name = ast.getName();
        if (ast.getValue().isPresent()){
            Environment.Type globalType = Environment.getType(ast.getTypeName());
            Ast.Expression val = ast.getValue().get();

            requireAssignable(val.getType(), globalType);

            visit(ast.getValue().get());
        }

        scope.defineVariable(name, name, Environment.getType(ast.getTypeName()), true, Environment.NIL);
        ast.setVariable(scope.lookupVariable(name));

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        String name = ast.getName();

        List<Environment.Type> params = new ArrayList<>();

        for (int i = 0; i < ast.getParameters().size(); i++){
            params.add(Environment.getType(ast.getParameters().get(i)));
        }

        Environment.Type functionReturn = Environment.NIL.getType();

        if (ast.getReturnTypeName().isPresent()){
            functionReturn = Environment.getType(ast.getReturnTypeName().get());
        }

        scope.defineVariable("functionReturn", "functionReturn", functionReturn, true, Environment.NIL);

        scope.defineFunction(name, name, params, functionReturn, args -> Environment.NIL);

        try{
            scope = new Scope(scope);
            for (int i = 0; i < ast.getParameters().size(); i++){
                String paramName = ast.getParameters().get(i);
                scope.defineVariable(paramName, paramName, params.get(i), true, Environment.NIL);
            }

            for (int i = 0; i < ast.getStatements().size(); i++){
                visit(ast.getStatements().get(i));
            }

            ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));
        } finally {
            scope = scope.getParent();
        }


        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());

        if (!(ast.getExpression() instanceof Ast.Expression.Function)){
            throw new RuntimeException("Need Expression.Function");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String name = ast.getName();
        if (!(ast.getValue().isPresent()) && !(ast.getTypeName().isPresent())){
            throw new RuntimeException("No value or type found in declaration");
        }
        Environment.Type type = null;
        if (ast.getTypeName().isPresent()){
            type = Environment.getType(ast.getTypeName().get());
        }
        if (ast.getValue().isPresent()){
            visit(ast.getValue().get());

            if (type != null){
                requireAssignable(ast.getValue().get().getType(), Environment.getType(ast.getTypeName().get()));
            } else {
                type = ast.getValue().get().getType();
            }

            requireAssignable(ast.getValue().get().getType(), type);

        }

        ast.setVariable(scope.defineVariable(name, name,type, true, Environment.NIL));

        return null;

    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)){
            throw new RuntimeException("Need receiver to be Expression.Access");
        }
        visit(ast.getValue());
        visit(ast.getReceiver());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        if (!(ast.getThenStatements().size() > 0)){
            throw new RuntimeException("Empty then list for If-Statement");
        }

        for (int i = 0; i < ast.getThenStatements().size(); i++){
            try {
                scope = new Scope(scope);
                visit(ast.getThenStatements().get(i));
            } finally {
                scope = scope.getParent();
            }
        }
        for (int i = 0; i < ast.getElseStatements().size(); i++){
            try {
                scope = new Scope(scope);
                visit(ast.getElseStatements().get(i));
            } finally {
                scope = scope.getParent();
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());

        Environment.Type conditionType = ast.getCondition().getType();
        int numberofCases = ast.getCases().size();
        for (int i = 0; i < numberofCases; i++){
                if (ast.getCases().get(i).getValue().isPresent()){
                    if (i == numberofCases - 1){
                        throw new RuntimeException("Default case has value");
                    }
                    visit(ast.getCases().get(i).getValue().get());
                    requireAssignable(conditionType, ast.getCases().get(i).getValue().get().getType());
                }

                try {
                    scope = new Scope(scope);
                    visit(ast.getCases().get(i));
                } finally {
                    scope = scope.getParent();
                }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        scope = new Scope(scope);

        for (int i = 0; i < ast.getStatements().size(); i++){
            visit(ast.getStatements().get(i));
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        Ast.Expression condition = ast.getCondition();

        if (condition.getType() == Environment.Type.BOOLEAN){
            scope = new Scope(scope);

            for (int i = 0; i < ast.getStatements().size(); i++){
                visit(ast.getStatements().get(i));
            }
            scope = scope.getParent();
            return null;
        }

        throw new RuntimeException("Non boolean condition");
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        Environment.Variable returnVar = scope.lookupVariable("functionReturn");
        requireAssignable(ast.getValue().getType(), returnVar.getType());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object l = ast.getLiteral();

        if (l == Environment.NIL){
            ast.setType(Environment.Type.NIL);
        } else if (l instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (l instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if (l instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else if (l instanceof BigInteger){
            BigInteger number = (BigInteger) l;

            if (number.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0 &&
                    number.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0){
                ast.setType(Environment.Type.INTEGER);
            } else {
                throw new RuntimeException("Integer Overflow");
            }
        } else if (l instanceof BigDecimal){
            BigDecimal number = (BigDecimal) l;

            if (number.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) <= 0 &&
                    number.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) >= 0){
                ast.setType(Environment.Type.DECIMAL);
            } else {
                throw new RuntimeException("Decimal Overflow");
            }
        }
        return null;

    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        Ast.Expression expr = ast.getExpression();

        if (expr instanceof Ast.Expression.Binary){
            visit(expr);
            ast.setType(ast.getExpression().getType());
            return null;
        }
        throw new RuntimeException("Invalid Group Expression, not Binary");
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        Ast.Expression l = ast.getLeft();
        Ast.Expression r = ast.getRight();
        visit(l);
        visit(r);

        String operator = ast.getOperator();

        if (operator.equals("&&") || operator.equals("||")) {
            requireAssignable(Environment.Type.BOOLEAN, l.getType());
            requireAssignable(Environment.Type.BOOLEAN, r.getType());
            requireAssignable(l.getType(), r.getType());
            ast.setType(Environment.Type.BOOLEAN);
        } else if (operator.equals("<") || operator.equals(">") || operator.equals("==")|| operator.equals("!=")){
            requireAssignable(Environment.Type.COMPARABLE, l.getType());
            requireAssignable(Environment.Type.COMPARABLE, r.getType());
            requireAssignable(l.getType(), r.getType());
            ast.setType(Environment.Type.BOOLEAN);
        } else if (operator.equals("+")){
            if (l.getType().getName().equals("String") || r.getType().getName().equals("String")){
                ast.setType(Environment.Type.STRING);
            } else if (l.getType().getName().equals("Integer")){
                requireAssignable(l.getType(), r.getType());
                ast.setType(Environment.Type.INTEGER);
            }
            else if (l.getType().getName().equals("Decimal")){
                requireAssignable(l.getType(), r.getType());
                ast.setType(Environment.Type.DECIMAL);
            } else {
                throw new RuntimeException("Invalid Type for +");
            }
        } else if (operator.equals("*") ||operator.equals("/") || operator.equals("-")){
            if (l.getType().getName().equals("Integer")){
                requireAssignable(l.getType(), r.getType());
                ast.setType(Environment.Type.INTEGER);
            }
            else if (l.getType().getName().equals("Decimal")){
                requireAssignable(l.getType(), r.getType());
                ast.setType(Environment.Type.DECIMAL);
            } else {
                throw new RuntimeException("Invalid Type for */-");
            }
        } else if (operator.equals("^")){
            if (l.getType().getName().equals("Integer")){
                requireAssignable(l.getType(), r.getType());
                ast.setType(Environment.Type.INTEGER);
            }
            else {
                throw new RuntimeException("Invalid Type for ^");
            }
        }


        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getOffset().isPresent()){
            Ast.Expression a = ast.getOffset().get();

            if (a.getType().getName().equals("Integer")){
                throw new RuntimeException("Non Integer Offset in Access");
            }
        }

        ast.setVariable(scope.lookupVariable(ast.getName()));

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function f = scope.lookupFunction(ast.getName(), ast.getArguments().size());

        List<Environment.Type> types = f.getParameterTypes();
        for (int i = 0; i < ast.getArguments().size(); i++){
            visit(ast.getArguments().get(i));
            requireAssignable(types.get(i), ast.getArguments().get(i).getType());
        }

        ast.setFunction(f);

        return null;

    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        for (Ast.Expression a : ast.getValues())
        {
            requireAssignable(ast.getType(), a.getType());
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        String t1 = target.getName();
        String t2 = type.getName();

        if (t1.equals(t2)){
            return;
        }

        if (!t1.equals(Environment.Type.NIL.getName())){
            if (t1.equals("Any")){
                return;
            }
            if (t1.equals("Comparable")){
                if (t2.equals("Integer") ||
                        t2.equals("Decimal") ||
                        t2.equals("Character") ||
                        t2.equals("String")){
                    return;
                }
            }

        }
        throw new RuntimeException("Mismatched Type Assignment");
    }

}
