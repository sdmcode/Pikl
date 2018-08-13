package com.pikl;

public class AstPrinter implements Expression.Visitor<String>, Statement.Visitor<String> {

    String print(Expression expr) {
        return expr.accept(this);
    }

    String print(Statement expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expression.AssignExpression expr) {
        return parenthesize2("=", expr.name.lexeme, expr.value);
    }

    @Override
    public String visitBinaryExpr(Expression.BinaryExpression expr) {
        return parenthesize(expr._type.lexeme, expr._left, expr._right);
    }

    @Override
    public String visitCallExpr(Expression.CallExpression expr) {
        return parenthesize2("call", expr.callee, expr.arguments);
    }

    @Override
    public String visitGetExpr(Expression.GetExpression expr) {
        return parenthesize2(".", expr.object, expr.name.lexeme);
    }

    @Override
    public String visitGroupingExpr(Expression.GroupExpression expr) {
        return parenthesize("group", expr._left);
    }

    @Override
    public String visitLiteralExpr(Expression.LiteralExpression expr) {

        if (expr.value == null)
            return "nil";

        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expression.LogicalExpression expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitSetExpr(Expression.SetExpression expr) {
        return parenthesize2("=", expr.object, expr.name.lexeme, expr.value);
    }

    @Override
    public String visitSuperExpr(Expression.SuperExpression expr) {
        return parenthesize2("super", expr.method);
    }

    @Override
    public String visitThisExpr(Expression.ThisExpression expr) {
        return "this";
    }

    @Override
    public String visitUnaryExpr(Expression.UnaryExpression expr) {
        return parenthesize(expr._type.lexeme, expr._left);
    }

    @Override
    public String visitVariableExpr(Expression.VariableExpression expr) {
        return expr.name.lexeme;
    }

    private String parenthesize(String name, Expression... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expression expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    private String parenthesize2(String name, Object... parts) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);

        for (Object part : parts) {
            builder.append(" ");

            if (part instanceof Expression) {
                builder.append(((Expression)part).accept(this));
//> Statements and State omit
            } else if (part instanceof Statement) {
                builder.append(((Statement) part).accept(this));
//< Statements and State omit
            } else if (part instanceof Token) {
                builder.append(((Token) part).lexeme);
            } else {
                builder.append(part);
            }
        }
        builder.append(")");

        return builder.toString();
    }

    @Override
    public String visitBlockStmt(Statement.Block stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(block ");

        for (Statement statement : stmt.statements) {
            builder.append(statement.accept(this));
        }

        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitClassStmt(Statement.Class stmt) {

        StringBuilder builder = new StringBuilder();
        builder.append("(class " + stmt.name.lexeme);
        //> Inheritance omit

        if (stmt.superclass != null) {
            builder.append(" < " + print(stmt.superclass));
        }
        //< Inheritance omit

        for (Statement.Function method : stmt.methods) {
            builder.append(" " + print(method));
        }

        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitExpressionStmt(Statement.Expression stmt) {
        return parenthesize(";", stmt.expression);
    }

    @Override
    public String visitFunctionStmt(Statement.Function stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(fun " + stmt.name.lexeme + "(");

        for (Token param : stmt.parameters) {
            if (param != stmt.parameters.get(0)) builder.append(" ");
            builder.append(param.lexeme);
        }

        builder.append(") ");

        for (Statement body : stmt.body) {
            builder.append(body.accept(this));
        }

        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitIfStmt(Statement.If stmt) {
        if (stmt.elseBranch == null) {
            return parenthesize2("if", stmt.condition, stmt.thenBranch);
        }

        return parenthesize2("if-else", stmt.condition, stmt.thenBranch,
                stmt.elseBranch);
    }

    @Override
    public String visitPrintStmt(Statement.Print stmt) {
        return parenthesize("print", stmt.expression);
    }

    @Override
    public String visitReturnStmt(Statement.Return stmt) {
        if (stmt.value == null) return "(return)";
        return parenthesize("return", stmt.value);
    }

    @Override
    public String visitVarStmt(Statement.Var stmt) {
        if (stmt.initializer == null) {
            return parenthesize2("var", stmt.name);
        }

        return parenthesize2("var", stmt.name, "=", stmt.initializer);
    }

    @Override
    public String visitWhileStmt(Statement.While stmt) {
        return parenthesize2("while", stmt.condition, stmt.body);
    }
}
