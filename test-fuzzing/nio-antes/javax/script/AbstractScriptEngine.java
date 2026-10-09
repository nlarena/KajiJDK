package javax.script;

import java.io.Reader;

/**
 * KajiLibrary's javax.script.AbstractScriptEngine -- the boring half of an engine, already written.
 *
 * <p>Of the fourteen methods of {@link ScriptEngine}, ten do not depend on the language: keeping a
 * context, looking up a `Bindings` by scope number, putting and getting variables, and reducing the
 * four easy `eval`s to the two hard ones. This class does that and leaves abstract exactly the ones
 * it cannot know: {@code eval(String,ScriptContext)}, {@code eval(Reader,ScriptContext)},
 * {@code createBindings()} and {@code getFactory()}.
 *
 * <p>The method with the real logic is {@link #getScriptContext(Bindings)}, and it is best read the
 * other way round from how it sounds: when somebody evaluates passing a loose {@link Bindings}, the
 * engine's engine scope is **not** being changed. A new, disposable context is built with that
 * `Bindings` as engine scope, the engine's global scope as it is, and the three streams copied from
 * the engine's context. That way what the script defines in that evaluation goes away with the
 * context and does not stick -- which is exactly the difference between `eval(s, bindings)` and
 * doing `setBindings` before `eval(s)`.
 *
 * <p>The {@link #context} field is `protected` and not final: a subclass can read it and change it
 * directly. It is part of the original's contract, with the good and the bad that brings.
 */
public abstract class AbstractScriptEngine implements ScriptEngine {

    /** This engine's default context. It should never be left null. */
    protected ScriptContext context;

    /** With a freshly made {@link SimpleScriptContext}. */
    public AbstractScriptEngine() {
        context = new SimpleScriptContext();
    }

    /**
     * Like the no-argument one, but with `n` as the context's engine scope.
     *
     * @throws NullPointerException if `n` is null
     */
    public AbstractScriptEngine(Bindings n) {
        this();
        if (n == null) {
            throw new NullPointerException("n is null");
        }
        context.setBindings(n, ScriptContext.ENGINE_SCOPE);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if `ctxt` is null
     */
    @Override
    public void setContext(ScriptContext ctxt) {
        if (ctxt == null) {
            throw new NullPointerException("null context");
        }
        context = ctxt;
    }

    /** {@inheritDoc} */
    @Override
    public ScriptContext getContext() {
        return context;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if `scope` is neither 100 nor 200
     */
    @Override
    public Bindings getBindings(int scope) {
        if (scope == ScriptContext.GLOBAL_SCOPE) {
            return context.getBindings(ScriptContext.GLOBAL_SCOPE);
        } else if (scope == ScriptContext.ENGINE_SCOPE) {
            return context.getBindings(ScriptContext.ENGINE_SCOPE);
        }
        throw new IllegalArgumentException("Invalid scope value.");
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if `scope` is neither 100 nor 200
     * @throws NullPointerException if `bindings` is null and `scope` is the engine one
     */
    @Override
    public void setBindings(Bindings bindings, int scope) {
        if (scope == ScriptContext.GLOBAL_SCOPE) {
            context.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
        } else if (scope == ScriptContext.ENGINE_SCOPE) {
            context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        } else {
            throw new IllegalArgumentException("Invalid scope value.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void put(String key, Object value) {
        Bindings nn = getBindings(ScriptContext.ENGINE_SCOPE);
        if (nn != null) {
            nn.put(key, value);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Object get(String key) {
        Bindings nn = getBindings(ScriptContext.ENGINE_SCOPE);
        if (nn != null) {
            return nn.get(key);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Object eval(Reader reader, Bindings bindings) throws ScriptException {
        ScriptContext ctxt = getScriptContext(bindings);
        return eval(reader, ctxt);
    }

    /** {@inheritDoc} */
    @Override
    public Object eval(String script, Bindings bindings) throws ScriptException {
        ScriptContext ctxt = getScriptContext(bindings);
        return eval(script, ctxt);
    }

    /** {@inheritDoc} */
    @Override
    public Object eval(Reader reader) throws ScriptException {
        return eval(reader, context);
    }

    /** {@inheritDoc} */
    @Override
    public Object eval(String script) throws ScriptException {
        return eval(script, context);
    }

    /**
     * A disposable context with `nn` as engine scope, for the variants of `eval` that receive a
     * {@link Bindings}.
     *
     * <p>It copies the engine's global scope (if it has one) and the three streams, but **not** the
     * engine scope: `nn` supplies that. What the script defines stays in `nn` and in the new
     * context.
     *
     * @throws NullPointerException if `nn` is null -- an `eval` with `Bindings` needs one
     */
    protected ScriptContext getScriptContext(Bindings nn) {
        SimpleScriptContext ctxt = new SimpleScriptContext();
        Bindings gs = getBindings(ScriptContext.GLOBAL_SCOPE);
        if (gs != null) {
            ctxt.setBindings(gs, ScriptContext.GLOBAL_SCOPE);
        }
        if (nn != null) {
            ctxt.setBindings(nn, ScriptContext.ENGINE_SCOPE);
        } else {
            throw new NullPointerException("Engine scope Bindings may not be null.");
        }
        ctxt.setReader(context.getReader());
        ctxt.setWriter(context.getWriter());
        ctxt.setErrorWriter(context.getErrorWriter());
        return ctxt;
    }
}
