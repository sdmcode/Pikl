package com.pikl;

import java.util.HashMap;
import java.util.Map;

class Environment {

    private final Map<String, Object> values = new HashMap<>();

    final Environment enclosing;

    void define(String name, Object value) {
        values.put(name, value);
    }

    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }

        return environment;
    }

    Object get(Token name) {

        // CHECK LOCAL SCOPE FIRST
        if (values.containsKey(name.lexeme)) {

            Object value = values.get(name.lexeme);
            if (value != null)
                return values.get(name.lexeme);


            // VARIABLE NOT INITIALISED
            throw new RuntimeError(name,
                    "Uninitialised variable '" + name.lexeme + "'.");
        }

        // IF WE DONT FIND THE VARIABLE HERE, RECURSIVELY TRY OUTER SCOPE
        if (enclosing != null)
            return enclosing.get(name);

        // VARIABLE DOESNT EXIST
        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value) {

        // CHECK LOCAL SCOPE FIRST
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        // IF WE DONT FIND THE VARIABLE HERE, RECURSIVELY TRY OUTER SCOPE
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        // VARIABLE DOESNT EXIST
        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'.");
    }

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }
}
