package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if (ast.getTypeName().isPresent()){
            print(Environment.getType(ast.getTypeName().get()).getJvmName());
        } else {
            if (ast.getValue().isPresent()) {
                print(ast.getValue().get().getType().getJvmName());
            }
        }

        print(" ");
        print(ast.getVariable().getJvmName());

        if (ast.getValue().isPresent()){
            print(" = ", ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {


        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ", ast.getValue(), ";");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getType() == Environment.Type.STRING){
            print("\"");
            print(ast.getLiteral());
            print("\"");
        } else if (ast.getType() == Environment.Type.CHARACTER){
            print("\'");
            print(ast.getLiteral());
            print("\'");
        }
        else if (ast.getType() == Environment.Type.INTEGER){
            print(ast.getLiteral());
        } else if (ast.getType() == Environment.Type.BOOLEAN){
            print(ast.getLiteral());
        } else if (ast.getType() == Environment.Type.DECIMAL){
            print(ast.getLiteral().toString());
        } else {
            throw new RuntimeException("Incompatible Literal Type");
        }

        return null;

    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        print(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if (ast.getOperator().equals("^")){
            print("Math.pow(", ast.getLeft(), " ", ast.getRight(), ")");
        }
        else {
            print(ast.getLeft());
            print(" ", ast.getOperator(), " ");
            print(ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getName());

        if (ast.getOffset().isPresent()){
            print("[");
            print(ast.getOffset());
            print("]");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName());

        print("(");

        if (ast.getArguments().size() > 0){
            for (int i = 0; i < ast.getArguments().size()-1; i++){
                print(ast.getArguments().get(i));
                print(", ");
            }
            print(ast.getArguments().get(ast.getArguments().size()-1));
        }

        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");

        if (ast.getValues().size() > 0){
            for (int i = 0; i < ast.getValues().size()-1; i++){
                print(ast.getValues().get(i));
                print(", ");
            }
            print(ast.getValues().get(ast.getValues().size()-1));
        }

        print("}");

        return null;

    }

}
