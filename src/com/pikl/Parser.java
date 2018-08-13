package com.pikl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.pikl.TokenType.*;

public class Parser {

    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Statement> parse() {
        List<Statement> statements = new ArrayList<>();

        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Statement declaration() {
        try {
            if (match(CLASS)) return classDeclaration();
            if (match(FUN)) return function("function");
            if (match(VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Statement classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name.");

        Expression.VariableExpression superclass = null;
        if (match(LESS)) {
            consume(IDENTIFIER, "Expect superclass name.");
            superclass = new Expression.VariableExpression(previous());
        }

        consume(LEFT_BRACE, "Expect '{' before class body.");

        List<Statement.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.");

        return new Statement.Class(name, superclass, methods);
    }

    private Statement.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");

        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 8) {
                    error(peek(), "Cannot have more than 8 parameters.");
                }

                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Statement> body = block();
        return new Statement.Function(name, parameters, body);
    }

    private Statement varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        com.pikl.Expression initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Statement.Var(name, initializer);
    }

    private List<Statement> block() {
        List<Statement> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Statement ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        com.pikl.Expression condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Statement thenBranch = statement();
        Statement elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Statement.If(condition, thenBranch, elseBranch);
    }

    private Statement whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        com.pikl.Expression condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Statement body = statement();

        return new Statement.While(condition, body);
    }

    private Statement forStatement() {

        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Statement initializer;

        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        com.pikl.Expression condition = null;

        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        com.pikl.Expression increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        Statement body = statement();

        if (increment != null) {
            body = new Statement.Block(Arrays.asList(
                    body,
                    new Statement.Expression(increment)));
        }

        if (condition == null) condition = new com.pikl.Expression.LiteralExpression(true);
        body = new Statement.While(condition, body);

        if (initializer != null) {
            body = new Statement.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Statement statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Statement.Block(block());

        return expressionStatement();
    }

    private Statement returnStatement() {
        Token keyword = previous();
        Expression value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value.");
        return new Statement.Return(keyword, value);
    }

    private Statement printStatement() {
        Expression value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Statement.Print(value);
    }

    private Statement expressionStatement() {
        Expression expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Statement.Expression(expr);
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean check(TokenType tokenType) {
        if (isAtEnd()) return false;
        return peek().type == tokenType;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private com.pikl.Expression expression() {
        return assignment();
    }

    private com.pikl.Expression assignment() {
        com.pikl.Expression expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            com.pikl.Expression value = assignment();

            if (expr instanceof com.pikl.Expression.VariableExpression) {
                Token name = ((com.pikl.Expression.VariableExpression)expr).name;
                return new com.pikl.Expression.AssignExpression(name, value);
            }

            else if (expr instanceof Expression.GetExpression) {
                Expression.GetExpression get = (Expression.GetExpression)expr;
                return new Expression.SetExpression(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private com.pikl.Expression or() {
        com.pikl.Expression expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expression right = and();
            expr = new Expression.LogicalExpression(expr, operator, right);
        }

        return expr;
    }

    private Expression and() {
        Expression expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expression right = equality();
            expr = new Expression.LogicalExpression(expr, operator, right);
        }

        return expr;
    }

    private Expression comparison() {
        Expression expr = addition();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expression right = addition();
            expr = new Expression.BinaryExpression(expr,  right, operator);
        }

        return expr;
    }

    private com.pikl.Expression equality() {
        Expression expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expression right = comparison();
            expr = new Expression.BinaryExpression(expr, right, operator);
        }

        return expr;
    }

    private com.pikl.Expression addition() {
        Expression expr = multiplication();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expression right = multiplication();
            expr = new Expression.BinaryExpression(expr, right, operator);
        }

        return expr;
    }

    private com.pikl.Expression multiplication() {
        Expression expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expression right = unary();
            expr = new Expression.BinaryExpression(expr, right, operator);
        }

        return expr;
    }

    private com.pikl.Expression unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expression right = unary();
            return new Expression.UnaryExpression(right, operator);
        }

        return call();
    }

    private com.pikl.Expression call() {
        com.pikl.Expression expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            }  else if (match(DOT)) {
                Token name = consume(IDENTIFIER,
                        "Expect property name after '.'.");
                expr = new Expression.GetExpression(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private com.pikl.Expression finishCall(com.pikl.Expression callee) {
        List<com.pikl.Expression> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 8) {
                    error(peek(), "Cannot have more than 8 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new com.pikl.Expression.CallExpression(callee, paren, arguments);
    }

    private com.pikl.Expression primary() {

        // check value key words first
        if (match(FALSE)) return new Expression.LiteralExpression(false);
        if (match(TRUE)) return new Expression.LiteralExpression(true);
        if (match(NIL)) return new Expression.LiteralExpression(null);

        // check for literal values
        if (match(FNUMBER, INUMBER, STRING)) { // only matching doubles for now
            return new Expression.LiteralExpression(previous().literal);
        }

        if (match(SUPER)) {
            Token keyword = previous();
            consume(DOT, "Expect '.' after 'super'.");
            Token method = consume(IDENTIFIER,
                    "Expect superclass method name.");
            return new Expression.SuperExpression(keyword, method);
        }

        if (match(THIS)) return new Expression.ThisExpression(previous());

        // check for variable/method identifiers
        if (match(IDENTIFIER)) {
            return new com.pikl.Expression.VariableExpression(previous());
        }

        if (match(LEFT_PAREN)) {
            Expression expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expression.GroupExpression(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Main.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }

}
