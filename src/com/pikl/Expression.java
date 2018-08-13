package com.pikl;

import java.util.List;

/*

    THIS CLASS GENERATES EXPRESSIONS

 */


abstract class Expression {

    interface Visitor<R> {
        R visitAssignExpr(AssignExpression expr);
        R visitBinaryExpr(BinaryExpression expr);
        R visitCallExpr(CallExpression expr);
        R visitGetExpr(GetExpression expr);
        R visitGroupingExpr(GroupExpression expr);
        R visitLiteralExpr(LiteralExpression expr);
        R visitLogicalExpr(LogicalExpression expr);
        R visitSetExpr(SetExpression expr);
        R visitSuperExpr(SuperExpression expr);
        R visitThisExpr(ThisExpression expr);
        R visitUnaryExpr(UnaryExpression expr);
        R visitVariableExpr(VariableExpression expr);
    }

    static class BinaryExpression extends Expression {
        BinaryExpression (Expression e1, Expression e2, Token t) {
            _left = e1;
            _right = e2;
            _type = t;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }

        Expression _left;
        Expression _right;
        Token _type;
    }

    static class UnaryExpression extends Expression {
        UnaryExpression(Expression e, Token t) {
            _left = e;
            _type = t;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }

        Expression _left;
        Token _type;
    }

    static class GroupExpression extends Expression {
        GroupExpression(Expression e) {
            _left = e;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }

        Expression _left;
    }

    static class LiteralExpression extends Expression {
        LiteralExpression(Object value) {
            this.value = value;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }

        final Object value;
    }

    static class AssignExpression extends Expression {
        AssignExpression(Token name, Expression value) {
            this.name = name;
            this.value = value;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitAssignExpr(this);
        }

        final Token name;
        final Expression value;
    }

    static class CallExpression extends Expression {
        CallExpression(Expression callee, Token paren, List<Expression> arguments) {
            this.callee = callee;
            this.paren = paren;
            this.arguments = arguments;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitCallExpr(this);
        }

        final Expression callee;
        final Token paren;
        final List<Expression> arguments;
    }

    static class GetExpression extends Expression {
        GetExpression(Expression object, Token name) {
            this.object = object;
            this.name = name;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGetExpr(this);
        }

        final Expression object;
        final Token name;
    }

    static class LogicalExpression extends Expression {
        LogicalExpression(Expression left, Token operator, Expression right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }

        final Expression left;
        final Token operator;
        final Expression right;
    }

    static class SetExpression extends Expression {
        SetExpression(Expression object, Token name, Expression value) {
            this.object = object;
            this.name = name;
            this.value = value;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitSetExpr(this);
        }

        final Expression object;
        final Token name;
        final Expression value;
    }

    static class SuperExpression extends Expression {
        SuperExpression(Token keyword, Token method) {
            this.keyword = keyword;
            this.method = method;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitSuperExpr(this);
        }

        final Token keyword;
        final Token method;
    }

    static class ThisExpression extends Expression {
        ThisExpression(Token keyword) {
            this.keyword = keyword;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitThisExpr(this);
        }

        final Token keyword;
    }

    static class VariableExpression extends Expression {
        VariableExpression(Token name) {
            this.name = name;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }

        final Token name;
    }

    abstract <R> R accept(Visitor<R> visitor);
}
