package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        List<Ast.Global> globals = ast.getGlobals();
        List<Ast.Function> functions = ast.getFunctions();

        for (Ast.Global global : globals) {
            visit(global);
        }
        for (Ast.Function function : functions) {
            visit(function);
        }

        return scope.lookupFunction("main", 0).invoke(new ArrayList<>());
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        if (ast.getValue().isPresent()) {
            Ast.Expression exp = ast.getValue().get();
            scope.defineVariable(ast.getName(), true, visit(exp));
        } else {
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        scope.defineFunction(ast.getName(), ast.getParameters().size(), arguments ->
        {
            try {
                scope = new Scope(scope);
                for (int i = 0; i < arguments.size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), true, arguments.get(i));
                }
                for (int i = 0; i < ast.getStatements().size(); i++) {
                    visit(ast.getStatements().get(i));
                }
            } catch (Return exception) {
                return exception.value;
            } finally {
                scope = scope.getParent();
            }
            return Environment.NIL;
        });
        return Environment.NIL;
    }




    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent())
        {
            Ast.Expression exp = ast.getValue().get();
            scope.defineVariable(ast.getName(), true, visit(exp));
        }

        else
        {
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        Ast.Expression receiver = ast.getReceiver();
        if (receiver instanceof Ast.Expression.Access){
            if (receiver != null) {
                if (((Ast.Expression.Access) receiver).getOffset().isPresent()){
                    Object offset = visit(((Ast.Expression.Access)receiver).getOffset().get()).getValue();

                    if (offset.getClass().equals(BigInteger.class)) {
                        Object list = scope.lookupVariable(((Ast.Expression.Access) receiver).getName()).getValue().getValue();

                        if (list instanceof List){
                            ((List<BigInteger>) list).set(((BigInteger) offset).intValue(), (BigInteger) visit(ast.getValue()).getValue());
                            return Environment.NIL;
                        }
                    } else {
                        throw new RuntimeException();
                    }


                }
                scope.lookupVariable(((Ast.Expression.Access) receiver).getName()).setValue(visit(ast.getValue()));
            }
        } else{
            throw new RuntimeException();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        if (requireType(Boolean.class, visit(ast.getCondition())) || !requireType(Boolean.class, visit(ast.getCondition()))){
            try {
                scope = new Scope(scope);
                Ast.Expression condition = ast.getCondition();
                if ((Boolean)visit(condition).getValue() == true){
                    for (int i = 0; i < ast.getThenStatements().size(); i++){
                        visit(ast.getThenStatements().get(i));
                    }
                } else {
                    for (int i = 0; i < ast.getElseStatements().size(); i++){
                        visit(ast.getElseStatements().get(i));
                    }
                }

            } finally {
                scope = scope.getParent();
            }
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {

        for (Ast.Statement.Case s : ast.getCases()) {
            if (s.getValue().isPresent() && visit(s.getValue().get()).getValue().equals(visit(ast.getCondition()).getValue()))
            {
                return visit(s);
            }
        }
        return visit(ast.getCases().get(ast.getCases().size() - 1));
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        scope = new Scope(scope);
        for (int i = 0; i < ast.getStatements().size(); i++){
            visit(ast.getStatements().get(i));
        }
        scope = scope.getParent();
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition())))
        {
            try
            {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            }
            finally
            {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        Environment.PlcObject returnVal = visit(ast.getValue());

        throw new Interpreter.Return(returnVal);

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null)
        {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();

        Environment.PlcObject lhs = visit(ast.getLeft());

        if (operator.equals("+")){
            Environment.PlcObject rhs = visit(ast.getRight());
            if (lhs.getValue() instanceof String || rhs.getValue() instanceof String){
                if (lhs.getValue() instanceof String && rhs.getValue() instanceof String){
                    return Environment.create(requireType(String.class, lhs) + (requireType(String.class, rhs)));
                }
                if (lhs.getValue() instanceof String && (rhs.getValue() instanceof BigInteger || rhs.getValue() instanceof BigDecimal)){
                    return Environment.create(requireType(String.class, lhs) + rhs.getValue());
                }
                if (rhs.getValue() instanceof String && (lhs.getValue() instanceof BigInteger || lhs.getValue() instanceof BigDecimal)){
                    return Environment.create(lhs.getValue() + (requireType(String.class, rhs)));
                }
                else{
                    throw new RuntimeException();
                }
            }
            else if (lhs.getValue() instanceof BigInteger){
                if (rhs.getValue() instanceof BigInteger){
                    return Environment.create(requireType(BigInteger.class, lhs).add(requireType(BigInteger.class, rhs)));
                }
                else{
                    throw new RuntimeException();
                }
            }
            else if (lhs.getValue() instanceof BigDecimal){
                if (rhs.getValue() instanceof BigDecimal){
                    return Environment.create(requireType(BigDecimal.class, lhs).add(requireType(BigDecimal.class, rhs)));
                }
                else{
                    throw new RuntimeException();
                }
            }
            else {
                throw new RuntimeException();
            }

        }
        else if (operator.equals("-")){
            if (lhs.getValue() instanceof BigInteger){
                Environment.PlcObject rhs = visit(ast.getRight());
                if (rhs.getValue() instanceof BigInteger){
                    return Environment.create(requireType(BigInteger.class, lhs).subtract(requireType(BigInteger.class, rhs)));
                }
                else{
                    throw new RuntimeException();
                }
            }
            else if (lhs.getValue() instanceof BigDecimal) {
                Environment.PlcObject rhs = visit(ast.getRight());
                if (rhs.getValue() instanceof BigDecimal) {
                    return Environment.create(requireType(BigDecimal.class, lhs).subtract(requireType(BigDecimal.class, rhs)));
                } else {
                    throw new RuntimeException();
                }
            } else {
                throw new RuntimeException();
            }
        } else if (operator.equals("*")){
            if (lhs.getValue() instanceof BigInteger){
                Environment.PlcObject rhs = visit(ast.getRight());
                if (rhs.getValue() instanceof BigInteger){
                    return Environment.create(requireType(BigInteger.class, lhs).multiply(requireType(BigInteger.class, rhs)));
                }
                else{
                    throw new RuntimeException();
                }
            }
            else if (lhs.getValue() instanceof BigDecimal) {
                Environment.PlcObject rhs = visit(ast.getRight());
                if (rhs.getValue() instanceof BigDecimal) {
                    return Environment.create(requireType(BigDecimal.class, lhs).multiply(requireType(BigDecimal.class, rhs)));
                } else {
                    throw new RuntimeException();
                }
            } else {
                throw new RuntimeException();
            }
        }
        else if (operator.equals("/")){
            if (lhs.getValue() instanceof BigInteger){
                Environment.PlcObject rhs = visit(ast.getRight());
                if ((rhs.getValue() instanceof BigInteger) && (((BigInteger) rhs.getValue()).intValue() != 0)){
                    return Environment.create(requireType(BigInteger.class, lhs).divide(requireType(BigInteger.class, rhs)));
                }
                else{
                    throw new RuntimeException();
                }
            }
            else if (lhs.getValue() instanceof BigDecimal) {
                Environment.PlcObject rhs = visit(ast.getRight());
                if ((rhs.getValue() instanceof BigDecimal) && (((BigDecimal) rhs.getValue()).doubleValue() != 0)) {
                    return Environment.create(requireType(BigDecimal.class, lhs).divide(requireType(BigDecimal.class, rhs), RoundingMode.HALF_EVEN));
                } else {
                    throw new RuntimeException();
                }
            } else {
                throw new RuntimeException();
            }
        }
        else if (operator.equals("^")){
            if (lhs.getValue() instanceof BigInteger){
                Environment.PlcObject rhs = visit(ast.getRight());
                if ((rhs.getValue() instanceof BigInteger)) {
                    if (((BigInteger) rhs.getValue()).intValue() > 0) {
                        BigInteger temp = requireType(BigInteger.class, lhs);

                        for (int i = 0; i < requireType(BigInteger.class, rhs).intValue() - 1; i++) {
                            temp = temp.multiply(requireType(BigInteger.class, lhs));
                        }

                        return Environment.create(temp);
                    } else if (((BigInteger) rhs.getValue()).intValue() < 0) {
                        BigInteger temp = BigInteger.ONE;

                        for (int i = 0; i < requireType(BigInteger.class, rhs).intValue(); i++) {
                            temp = temp.divide(requireType(BigInteger.class, lhs));
                        }

                        return Environment.create(temp);
                    } else {
                        return Environment.create(BigInteger.ONE);
                    }

                }
                else{
                    throw new RuntimeException();
                }
            } else {
                throw new RuntimeException();
            }
        }
        else if (operator.equals("==")){
            Environment.PlcObject rhs = visit(ast.getRight());

            boolean equality = Objects.equals(lhs.getValue(), rhs.getValue());

            return Environment.create(equality);
        }
        else if (operator.equals("!=")){
            Environment.PlcObject rhs = visit(ast.getRight());

            boolean equality = Objects.equals(lhs.getValue(), rhs.getValue());

            return Environment.create(!equality);
        }
        else if (operator.equals("||")){
            if (lhs.getValue() instanceof Boolean ){
                if ((Boolean)lhs.getValue() == true){
                    return Environment.create(true);
                }
                Environment.PlcObject rhs = visit(ast.getRight());
                if (rhs.getValue() instanceof Boolean){
                    if ((Boolean) rhs.getValue() == true){
                        return Environment.create(true);
                    } else {
                        return Environment.create(false);
                    }
                } else {
                    throw new RuntimeException();
                }
            } else {
                throw new RuntimeException();
            }
        } else if (operator.equals("&&")){
            if (lhs.getValue() instanceof Boolean ){
                if ((Boolean)lhs.getValue() == false){
                    return Environment.create(false);
                }
                Environment.PlcObject rhs = visit(ast.getRight());
                if (rhs.getValue() instanceof Boolean){
                    if ((Boolean) rhs.getValue() == false){
                        return Environment.create(false);
                    } else {
                        return Environment.create(true);
                    }
                } else {
                    throw new RuntimeException();
                }
            } else {
                throw new RuntimeException();
            }
        } else if (operator.equals(">")){
            if (lhs.getValue() instanceof Comparable ) {
                Environment.PlcObject rhs = visit(ast.getRight());

                if (lhs.getValue().getClass() != null && rhs.getValue().getClass() != null && rhs.getValue() instanceof Comparable) {
                    return Environment.create(((Comparable) lhs.getValue()).compareTo(rhs.getValue()) > 0);
                }
                else {
                    throw new RuntimeException();
                }
            } else {
                throw new RuntimeException();
            }
        } else if (operator.equals("<")){
            if (lhs.getValue() instanceof Comparable ) {
                Environment.PlcObject rhs = visit(ast.getRight());

                if (lhs.getValue().getClass() != null && rhs.getValue().getClass() != null && rhs.getValue() instanceof Comparable) {
                    return Environment.create(((Comparable) lhs.getValue()).compareTo(rhs.getValue()) < 0);
                }
                else {
                    throw new RuntimeException();
                }
            } else {
                throw new RuntimeException();
            }
        }

        throw new RuntimeException("Unsupported Binary Operation");
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getOffset().isPresent())
        {
            Object offset = visit(ast.getOffset().get()).getValue();

            if (!offset.getClass().equals(BigInteger.class))
            {
                throw new RuntimeException();
                //throw exception
            }
            Object astVal = scope.lookupVariable(ast.getName()).getValue().getValue();
            if (astVal instanceof List)
            {
                return Environment.create(((List<?>) astVal).get(Integer.parseInt(offset.toString())));
            }
        }
        return scope.lookupVariable(ast.getName()).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        List<Environment.PlcObject> argList = new ArrayList<>(); //make list to hold args

        for (int i = 0; i < ast.getArguments().size(); i++){ //copy over args into the new list
            argList.add(visit(ast.getArguments().get(i)));
        }

        Environment.Function f = scope.lookupFunction(ast.getName(), ast.getArguments().size()); //define new function with given name and size
        return f.invoke(argList); //invoke and return

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> values = ast.getValues();
        ArrayList<Object> newList = new ArrayList<>();

        for (int i = 0; i < values.size(); i++)
        {
            newList.add(visit(values.get(i)).getValue());
        }

        Object value = newList;
        return Environment.create(value);
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}