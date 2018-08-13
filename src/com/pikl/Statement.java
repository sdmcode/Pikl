package com.pikl;

import java.util.List;


public abstract class Statement {

    interface Visitor<R> {
        R visitBlockStmt(Block stmt);
        R visitClassStmt(Class stmt);
        R visitExpressionStmt(Expression stmt);
        R visitFunctionStmt(Function stmt);
        R visitIfStmt(If stmt);
        R visitPrintStmt(Print stmt);
        R visitReturnStmt(Return stmt);
        R visitVarStmt(Var stmt);
        R visitWhileStmt(While stmt);
    }

    static class Block extends Statement {
        Block(List<Statement> statements) {
            this.statements = statements;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBlockStmt(this);
        }

        final List<Statement> statements;
    }

    static class Class extends Statement {
        Class(Token name,
              com.pikl.Expression.VariableExpression superclass,
              List<Statement.Function> methods) {
            this.name = name;
            this.superclass = superclass;
            this.methods = methods;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitClassStmt(this);
        }

        final Token name;
        final com.pikl.Expression.VariableExpression superclass;
        final List<Statement.Function> methods;
    }

    static class Expression extends Statement {
        Expression(com.pikl.Expression expression) {
            this.expression = expression;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }

        final com.pikl.Expression expression;
    }

    static class Function extends Statement {
        Function(Token name, List<Token> parameters, List<Statement> body) {
            this.name = name;
            this.parameters = parameters;
            this.body = body;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionStmt(this);
        }

        final Token name;
        final List<Token> parameters;
        final List<Statement> body;
    }

    static class If extends Statement {
        If(com.pikl.Expression condition, Statement thenBranch, Statement elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitIfStmt(this);
        }

        final com.pikl.Expression condition;
        final Statement thenBranch;
        final Statement elseBranch;
    }

    static class Print extends Statement {
        Print(com.pikl.Expression expression) {
            this.expression = expression;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }

        final com.pikl.Expression expression;
    }

    static class Return extends Statement {
        Return(Token keyword, com.pikl.Expression value) {
            this.keyword = keyword;
            this.value = value;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitReturnStmt(this);
        }

        final Token keyword;
        final com.pikl.Expression value;
    }

    static class Var extends Statement {
        Var(Token name, com.pikl.Expression initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }

        final Token name;
        final com.pikl.Expression initializer;
    }

    static class While extends Statement {
        While(com.pikl.Expression condition, Statement body) {
            this.condition = condition;
            this.body = body;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitWhileStmt(this);
        }

        final com.pikl.Expression condition;
        final Statement body;
    }

    abstract <R> R accept(Visitor<R> visitor);

}
