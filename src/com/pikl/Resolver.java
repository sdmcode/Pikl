package com.pikl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expression.Visitor<Void>, Statement.Visitor<Void> {

    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }

    private ClassType currentClass = ClassType.NONE;

    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visitAssignExpr(Expression.AssignExpression expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expression.BinaryExpression expr) {
        resolve(expr._left);
        resolve(expr._right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expression.CallExpression expr) {
        resolve(expr.callee);

        for (Expression argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGetExpr(Expression.GetExpression expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expression.GroupExpression expr) {
        resolve(expr._left);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expression.LiteralExpression expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expression.LogicalExpression expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expression.SetExpression expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSuperExpr(Expression.SuperExpression expr) {

        if (currentClass == ClassType.NONE) {
            Main.error(expr.keyword,
                    "Cannot use 'super' outside of a class.");
        } else if (currentClass != ClassType.SUBCLASS) {
            Main.error(expr.keyword,
                    "Cannot use 'super' in a class with no superclass.");
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitThisExpr(Expression.ThisExpression expr) {

        if (currentClass == ClassType.NONE) {
            Main.error(expr.keyword,
                    "Cannot use 'this' outside of a class.");
            return null;
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expression.UnaryExpression expr) {
        resolve(expr._left);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expression.VariableExpression expr) {
        if (!scopes.isEmpty() &&
                scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Main.error(expr.name,
                    "Cannot read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    private void resolveLocal(Expression expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }

        // Not found. Assume it is global.
    }

    @Override
    public Void visitBlockStmt(Statement.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    void resolve(List<Statement> statements) {
        for (Statement statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Statement stmt) {
        stmt.accept(this);
    }

    private void endScope() {
        scopes.pop();
    }

    @Override
    public Void visitClassStmt(Statement.Class stmt) {

        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        declare(stmt.name);

        if (stmt.superclass != null) {
            currentClass = ClassType.SUBCLASS;
            resolve(stmt.superclass);
        }

        define(stmt.name);

        if (stmt.superclass != null) {
            beginScope();
            scopes.peek().put("super", true);
        }

        beginScope();
        scopes.peek().put("this", true);

        for (Statement.Function method : stmt.methods) {
            FunctionType declaration = FunctionType.METHOD;

            if (method.name.lexeme.equals("init")) {
                declaration = FunctionType.INITIALIZER;
            }

            resolveFunction(method, declaration);
        }

        endScope();

        if (stmt.superclass != null) endScope();

        currentClass = enclosingClass;

        return null;
    }

    @Override
    public Void visitExpressionStmt(Statement.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Statement.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);

        return null;
    }

    private void resolveFunction(Statement.Function function, FunctionType type) {

        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.parameters) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();

        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitIfStmt(Statement.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Statement.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Statement.Return stmt) {

        if (currentFunction == FunctionType.NONE) {
            Main.error(stmt.keyword, "Cannot return from top-level code.");
        }

        if (stmt.value != null) {

            if (currentFunction == FunctionType.INITIALIZER) {
                Main.error(stmt.keyword,
                        "Cannot return a value from an initializer.");
            }

            resolve(stmt.value);
        }

        return null;
    }

    @Override
    public Void visitVarStmt(Statement.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    private void resolve(Expression expr) {
        expr.accept(this);
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, Boolean> scope = scopes.peek();

        if (scope.containsKey(name.lexeme)) {
            Main.error(name,
                    "Variable with this name already declared in this scope.");
        }

        scope.put(name.lexeme, false);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    @Override
    public Void visitWhileStmt(Statement.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }
}
