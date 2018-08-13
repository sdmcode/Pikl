package com.pikl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expression.Visitor<Object>, Statement.Visitor<Void> {

    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expression, Integer> locals = new HashMap<>();

    void executeBlock(List<Statement> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Statement statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private Object lookUpVariable(Token name, Expression expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    void resolve(Expression expr, int depth) {
        locals.put(expr, depth);
    }

    @Override
    public Void visitBlockStmt(Statement.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Statement.Class stmt) {

        Object superclass = null;
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass);
            if (!(superclass instanceof PklClass)) {
                throw new RuntimeError(stmt.superclass.name,
                        "Superclass must be a class.");
            }
        }

        environment.define(stmt.name.lexeme, null);

        if (stmt.superclass != null) {
            environment = new Environment(environment);
            environment.define("super", superclass);
        }

        Map<String, PklFunction> methods = new HashMap<>();
        for (Statement.Function method : stmt.methods) {
            PklFunction function = new PklFunction(method, environment,
                    method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }

        PklClass klass = new PklClass(stmt.name.lexeme, (PklClass)superclass, methods);

        if (superclass != null) {
            environment = environment.enclosing;
        }

        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Statement.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Statement.Function stmt) {
        PklFunction function = new PklFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Statement.If stmt) {
        if (isTruth(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Statement.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Statement.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Statement.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Statement.While stmt) {
        while (isTruth(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expression.AssignExpression expr) {
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }

        return value;
    }

    @Override
    public Object visitBinaryExpr(Expression.BinaryExpression expr) {

        Object left = evaluate(expr._left);
        Object right = evaluate(expr._right);

        switch (expr._type.type) {

            case BANG_EQUAL:
                return !isEqual(left, right);

            case EQUAL_EQUAL:
                return isEqual(left, right);

            case GREATER:

                checkNumberOperands(expr._type, left, right);

                if (left instanceof Integer && right instanceof Integer) {
                    return (int)left > (int)right;
                }

                return (double)left > (double)right;

            case GREATER_EQUAL:

                checkNumberOperands(expr._type, left, right);

                if (left instanceof Integer && right instanceof Integer) {
                    return (int)left >= (int)right;
                }

                return (double)left >= (double)right;

            case LESS:

                checkNumberOperands(expr._type, left, right);

                if (left instanceof Integer && right instanceof Integer) {
                    return (int)left < (int)right;
                }

                return (double)left < (double)right;

            case LESS_EQUAL:

                checkNumberOperands(expr._type, left, right);

                if (left instanceof Integer && right instanceof Integer) {
                    return (int)left <= (int)right;
                }

                return (double)left <= (double)right;

            case MINUS:

                checkNumberOperands(expr._type, left, right);

                // CHECK IF BOTH ARE INTEGER VALUES
                if (left instanceof Integer && right instanceof Integer) {
                    return (int)left - (int)right;
                }

                // OTHERWISE CAST BOTH TO DOUBLE

                return (double)left - (double)right;

            case PLUS:

                // IF THEY AREN'T NUMERICAL THEY SHOULD BE STRINGS
                if (left instanceof String || right instanceof String) {
                    return left.toString() + right.toString();
                }

                checkNumberOperands(expr._type, left, right);


                // CHECK IF BOTH ARE INTEGER VALUES
                if (left instanceof Integer && right instanceof Integer) {
                    return (int)left + (int)right;
                }

                // MAKE SURE THESE ARE BOTH AT LEAST NUMERICAL AND CAST TO DOUBLE

               // if ((left instanceof Double || left instanceof Integer) && ((right instanceof Double) || right instanceof Integer)) {

                if ((left instanceof Double && right instanceof Double)) {
                    return (double)left + (double)right;
                }

                // ANYTHING ELSE IS AN ERROR
                throw new RuntimeError(expr._type,
                        "Operands must be two numbers or two strings.");

            case SLASH:

                checkNumberOperands(expr._type, left, right);

                if (left instanceof Double && right instanceof Double)
                    return (double)left / (double)right;

                return (int)left / (int)right;

            case STAR:

                checkNumberOperands(expr._type, left, right);

                if (left instanceof Double && right instanceof Double)
                    return (double)left * (double)right;

                return (int)left * (int)right;
        }

        return null;
    }

    @Override
    public Object visitCallExpr(Expression.CallExpression expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expression argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof Callable)) {
            throw new RuntimeError(expr.paren,
                    "Can only call functions and classes.");
        }

        Callable function = (Callable)callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expression.GetExpression expr) {
        Object object = evaluate(expr.object);
        if (object instanceof PklInstance) {
            return ((PklInstance) object).get(expr.name);
        }

        throw new RuntimeError(expr.name,
                "Only instances have properties.");
    }

    @Override
    public Object visitGroupingExpr(Expression.GroupExpression expr) {
        return evaluate(expr._left);
    }

    @Override
    public Object visitLiteralExpr(Expression.LiteralExpression expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expression.LogicalExpression expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruth(left)) return left;
        } else {
            if (!isTruth(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expression.SetExpression expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof PklInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((PklInstance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitSuperExpr(Expression.SuperExpression expr) {
        int distance = locals.get(expr);
        PklClass superclass = (PklClass)environment.getAt(
                distance, "super");

        PklInstance object = (PklInstance)environment.getAt(
                distance - 1, "this");

        PklFunction method = superclass.findMethod(
                object, expr.method.lexeme);

        if (method == null) {
            throw new RuntimeError(expr.method,
                    "Undefined property '" + expr.method.lexeme + "'.");
        }

        return method;
    }

    @Override
    public Object visitThisExpr(Expression.ThisExpression expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitUnaryExpr(Expression.UnaryExpression expr) {
        Object right = evaluate(expr._left);

        switch (expr._type.type) {
            case BANG:
                return !isTruth(right);
            case MINUS:
                checkNumberOperand(expr._type, right);
                return -(double)right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitVariableExpr(Expression.VariableExpression expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object evaluate(com.pikl.Expression expr) {
        return expr.accept(this);
    }

    private Object evaluate(Statement expr) {
        return expr.accept(this);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double || operand instanceof Integer) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator,
                                     Object left, Object right) {

        // allow implicit casts ?
        if (left instanceof Double  && right instanceof Double) {
            // make sure we're not dividing by 0
            if (operator.type == TokenType.SLASH) {
                if ((double)right == 0.0)
                    throw new RuntimeError(operator, "Divide by zero error.");

                return;
            }
            return;
        }

        // check for integer values first
        if (left instanceof Integer && right instanceof Integer) {
            // make sure we're not dividing by 0
            if (operator.type == TokenType.SLASH) {
                if ((int)right == 0)
                    throw new RuntimeError(operator, "Divide by zero error.");

                return;
            }
            return;
        }

        if ((left instanceof Double || left instanceof Integer) && (right instanceof Double || right instanceof Integer)) {
            // make sure we're not dividing by 0
            if (operator.type == TokenType.SLASH) {
                if ((double)right == 0.0)
                    throw new RuntimeError(operator, "Divide by zero error.");

                return;
            }
            return;

            //   throw new RuntimeError(operator, "Type mismatch.");
        }

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isEqual(Object a, Object b) {
        // nil is only equal to nil.
        if (a == null && b == null)
            return true;

        if (a == null)
            return false;

        return a.equals(b);
    }

    private boolean isTruth(Object object) {

        if (object == null)
            return false;

        if (object instanceof Boolean)
            return (boolean)object;

        return true;
    }

    private String stringify(Object object) {

        if (object == null)
            return "nil";

        return object.toString();
    }

    private void execute(Statement stmt) {
        stmt.accept(this);
    }

    void interpret(List<Statement> statements) {
        try {
            for (Statement statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Main.runtimeError(error);
        }
    }

    void interpret(Expression expression) {
        try {
            Object value = evaluate(expression);
       //     System.out.println(stringify(value)); // this call should be replaced with a write to file
        } catch (RuntimeError error) {
            Main.runtimeError(error);
        }
    }

    Interpreter() {

        // THIS IS WHERE ANY SYSTEM / NATIVE FUNCTIONS ARE DEFINED

        globals.define("clock", new Callable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }
}
