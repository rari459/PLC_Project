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
        print("public class Main {");
        newline(indent);
        indent = indent + 1;
        newline(indent);
        for (Ast.Global global : ast.getGlobals())
        {
            print(global);
            newline(indent);
        }
        print("public static void main(String[] args) {");
        newline(indent + 1);
        print("System.exit(new Main().main());");
        newline(indent);
        print("}");

        for (Ast.Function function : ast.getFunctions()) {
            indent -= 1;
            newline(indent);
            indent += 1;
            newline(indent);
            print(function);
        }
        indent -= 1;
        newline(indent);
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        String name = ast.getName();
        String typeName = Environment.getType(ast.getTypeName()).getJvmName();

        if (ast.getMutable() || ast.getValue().get() instanceof Ast.Expression.PlcList) {
            if(ast.getValue().get() instanceof Ast.Expression.PlcList)
            {
                print(typeName, "[] ", name);
            }
            else {
                print(typeName, " ", name);
            }
            if (ast.getValue().isPresent()) {
                print(" = ", ast.getValue().get());
            }
            print(";");
        } else {
            print("final ", typeName, " ", name);
            if (ast.getValue().isPresent()) {
                print(" = ", ast.getValue().get());
            }
            print(";");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        if (ast.getReturnTypeName().isPresent()) {
            String jvmType = Environment.getType(ast.getReturnTypeName().get()).getJvmName();
            print(jvmType);
        }

        print(" ", ast.getName(), "(");
        for (int i = 0; i < ast.getParameters().size(); i++)
        {
            String type = Environment.getType(ast.getParameters().get(i)).getJvmName();
            String name = ast.getParameters().get(i);
            print(type, " ", name);
            if (i != ast.getParameters().size() - 1)
            {
                print(", ");
            }
        }
        print(") {");
        if (ast.getStatements().isEmpty())
        {
            print(" }");
            newline(indent);
        }
        else {
            indent++;
            for (int i = 0; i < ast.getStatements().size(); i++)
            {
                newline(indent);
                print(ast.getStatements().get(i));
            }
            indent--;
            newline(indent);
            print("}");
        }
        return null;
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
        print("if (", ast.getCondition(), ") {");
        indent += 1;

        for (int i = 0; i < ast.getThenStatements().size(); i++){
            newline(indent);
            print(ast.getThenStatements().get(i));
        }

        indent -= 1;

        if (ast.getElseStatements().size() >0 ){
            newline(indent);
            print("} else {");
            indent += 1;

            for (int i = 0; i < ast.getElseStatements().size(); i++){
                newline(indent);
                print(ast.getElseStatements().get(i));
            }
            indent -= 1;
        }


        newline(indent);
        print("}");
        return null;

    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (", ast.getCondition(), ") {");
        indent++;
        for (int i = 0; i < ast.getCases().size() - 1; i++)
        {
            newline(indent);
            print("case ", ast.getCases().get(i));
        }
        newline(indent);
        print("default:");
        indent++;
        Ast.Statement.Case def = ast.getCases().get(ast.getCases().size() - 1);
        for (int i = 0; i < def.getStatements().size(); i++)
        {
            newline(indent);
            print(def.getStatements().get(i));
        }
        indent--;

        indent--;
        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        print(ast.getValue().get(), ":");
        indent++;
        for (int i = 0; i < ast.getStatements().size(); i++)
        {
            newline(indent);
            print(ast.getStatements().get(i));
        }
        newline(indent);
        print("break;");
        indent--;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (", ast.getCondition(), ") {");

        indent += 1;

        if (ast.getStatements().size() > 0){
            for (int i = 0; i < ast.getStatements().size(); i++){
                newline(indent);
                print(ast.getStatements().get(i));
            }
            indent -=1;
        }
        newline(indent);
        print("}");
        return null;
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
            print(ast.getOffset().get());
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
