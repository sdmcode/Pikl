package com.pikl;

import java.util.List;
import java.util.Map;

class PklClass implements Callable {
    final String name;

    private final Map<String, PklFunction> methods;
    final PklClass superclass;

    PklClass(String name, PklClass superclass, Map<String, PklFunction> methods) {
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
    }

    @Override
    public String toString() {
        return name;
    }

    PklFunction findMethod(PklInstance instance, String name) {

        //check local first
        if (methods.containsKey(name)) {
            return methods.get(name).bind(instance);
        }

        // check global scope if we don't find it
        if (superclass != null) {
            return superclass.findMethod(instance, name);
        }

        return null;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {

        PklInstance instance = new PklInstance(this);

        PklFunction initializer = methods.get("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override
    public int arity() {
        PklFunction initializer = methods.get("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }
}
