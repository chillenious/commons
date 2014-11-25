package com.chillenious.common.util;

import javax.script.Invocable;
import javax.script.ScriptException;

/**
 * Represents the JavaScript function with the provided function name.
 */
public class JavaScriptFn {

    private final String functionName;

    private final Invocable invocable;

    /**
     * Create.
     *
     * @param functionName name of the function
     * @param invocable    invocable (engine with evaluation applied)
     */
    public JavaScriptFn(String functionName, Invocable invocable) {
        this.functionName = functionName;
        this.invocable = invocable;
    }

    /**
     * Invoke function with the provided arguments.
     *
     * @param args arguments to supply the function call with
     * @return function invocation result
     */
    public Object invoke(Object... args) {
        try {
            return invocable.invokeFunction(functionName, args);
        } catch (ScriptException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
