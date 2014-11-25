package com.chillenious.common.util;

import com.chillenious.common.DynamicSetting;
import com.chillenious.common.Setting;
import com.chillenious.common.SettingsListener;

import javax.annotation.Nullable;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Setting that wraps a piece of JavaScript and that returns an instance
 * of {@link com.chillenious.common.util.JavaScriptFn} together with a function wrapper etc.
 * <p/>
 * This gives you an instance where the value of the setting is wrapped in a Javascript function with
 * the provided arguments as it's function arguments. For instance, if your setting's value is:
 * <code>
 * return domains < 20 ? domains * 2.0 : domains * 0.8;
 * </code>
 * then the actual function will be:
 * <code>
 * function f(domains) { return domains < 20 ? domains * 2.0 : domains * 0.8; }
 * </code>
 * <p/>
 * NOTE: you will have to use 'return' still... cannot be helped (it's JavaScript)
 */
public class JavaScriptFnSetting extends Setting<JavaScriptFn> {

    private static final long serialVersionUID = 1L;

    static final String JAVA_SCRIPT_ENGINE = "JavaScript", FUNCTION_NAME = "f";

    private final String[] arguments;

    public JavaScriptFnSetting(DynamicSetting setting,
                               String... arguments) {
        super(setting, JavaScriptFn.class);
        this.arguments = arguments;
    }

    public JavaScriptFnSetting(DynamicSetting setting,
                               SettingsListener listener,
                               String... arguments) {
        super(setting, JavaScriptFn.class, listener);
        this.arguments = arguments;
    }

    private JavaScriptFn cached = null;

    @Nullable
    @Override
    public synchronized JavaScriptFn get() {
        if (cached == null) {
            String script = getCurrentValueAsString();
            try {
                ScriptEngineManager manager = new ScriptEngineManager(null); // null to work around a JVM bug in 1.7
                ScriptEngine engine = manager.getEngineByName(JAVA_SCRIPT_ENGINE);
                engine.eval(script);
                cached = new JavaScriptFn(FUNCTION_NAME, (Invocable) engine);
            } catch (ScriptException e) {
                throw new RuntimeException(e);
            }
        }
        return cached;
    }

    @Override
    public synchronized void onChanged(
            @Nullable Object oldValue, @Nullable Object newValue) {
        cached = null;
        super.onChanged(oldValue, newValue);
    }

    @Override
    public String getCurrentValueAsString() {
        return "function " + FUNCTION_NAME + "(" +
                Strings.join(",", arguments) + ") { " +
                super.getCurrentValueAsString() + " }";
    }
}
