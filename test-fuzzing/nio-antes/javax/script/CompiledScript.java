package javax.script;

/**
 * KajiLibrary's javax.script.CompiledScript -- a script already parsed, ready to run again.
 *
 * <p>{@link Compilable#compile(String)} returns it and it keeps no world: it keeps the compiled
 * form and nothing more. The world is passed by whoever evaluates, each time, which allows
 * compiling once and running the same script against different contexts.
 *
 * <p>Of the four methods, two are abstract ({@link #eval(ScriptContext)} and {@link #getEngine()})
 * and the other two are written with them. The interesting one is {@link #eval(Bindings)}: it does
 * **not** evaluate against the engine's context with the `Bindings` put inside -- it builds a
 * temporary context with those `Bindings` as engine scope and everything else (global, input,
 * output, error) copied from the engine's context. The engine stays as it was.
 *
 * <p>A detail easy to overlook: if `bindings` is null, {@link #eval(Bindings)} evaluates directly
 * against the engine's context -- it builds nothing temporary, and then what the script defines
 * does stay in the engine.
 */
public abstract class CompiledScript {

    /** For subclasses. */
    public CompiledScript() {
    }

    /**
     * Evaluates against `context`.
     *
     * @throws ScriptException if the script blows up
     * @throws NullPointerException if `context` is null
     */
    public abstract Object eval(ScriptContext context) throws ScriptException;

    /**
     * Evaluates with `bindings` as engine scope and the rest of the world copied from the engine.
     *
     * <p>With a null `bindings` it evaluates against the engine's context as it is.
     *
     * @throws ScriptException if the script blows up
     */
    public Object eval(Bindings bindings) throws ScriptException {
        ScriptContext ctxt = getEngine().getContext();

        if (bindings != null) {
            SimpleScriptContext tempctxt = new SimpleScriptContext();
            tempctxt.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            tempctxt.setBindings(ctxt.getBindings(ScriptContext.GLOBAL_SCOPE),
                    ScriptContext.GLOBAL_SCOPE);
            tempctxt.setWriter(ctxt.getWriter());
            tempctxt.setReader(ctxt.getReader());
            tempctxt.setErrorWriter(ctxt.getErrorWriter());
            ctxt = tempctxt;
        }

        return eval(ctxt);
    }

    /**
     * Evaluates against the engine's context.
     *
     * @throws ScriptException if the script blows up
     */
    public Object eval() throws ScriptException {
        return eval(getEngine().getContext());
    }

    /** The engine it came from, against whose context it evaluates by default. */
    public abstract ScriptEngine getEngine();
}
