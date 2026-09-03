package javax.script;

import java.io.Reader;

/**
 * KajiLibrary's javax.script.AbstractScriptEngine -- la mitad aburrida de un motor, ya escrita.
 *
 * <p>De los catorce metodos de {@link ScriptEngine}, diez no dependen del lenguaje: guardar un
 * contexto, buscar un `Bindings` por numero de ambito, poner y sacar variables, y reducir los
 * cuatro `eval` faciles a los dos dificiles. Esta clase hace eso y deja abstractos justo los que
 * no puede saber: {@code eval(String,ScriptContext)}, {@code eval(Reader,ScriptContext)},
 * {@code createBindings()} y {@code getFactory()}.
 *
 * <p>El metodo con la logica de verdad es {@link #getScriptContext(Bindings)}, y conviene leerlo
 * al reves de como suena: cuando alguien evalua pasando un {@link Bindings} suelto, **no** se le
 * esta cambiando el ambito de motor al motor. Se arma un contexto nuevo y descartable con ese
 * `Bindings` como ambito de motor, el global del motor tal cual esta, y los tres canales copiados
 * del contexto del motor. Asi lo que el script defina en esa evaluacion se va con el contexto y no
 * queda pegado -- que es exactamente la diferencia entre `eval(s, bindings)` y hacer
 * `setBindings` antes de `eval(s)`.
 *
 * <p>El campo {@link #context} es `protected` y no final: una subclase puede leerlo y cambiarlo
 * directo. Es parte del contrato del original, con lo bueno y lo malo que eso trae.
 */
public abstract class AbstractScriptEngine implements ScriptEngine {

    /** El contexto por defecto de este motor. Nunca deberia quedar en nulo. */
    protected ScriptContext context;

    /** Con un {@link SimpleScriptContext} recien hecho. */
    public AbstractScriptEngine() {
        context = new SimpleScriptContext();
    }

    /**
     * Igual que el sin argumentos, pero con `n` como ambito de motor del contexto.
     *
     * @throws NullPointerException si `n` es nulo
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
     * @throws NullPointerException si `ctxt` es nulo
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
     * @throws IllegalArgumentException si `scope` no es 100 ni 200
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
     * @throws IllegalArgumentException si `scope` no es 100 ni 200
     * @throws NullPointerException si `bindings` es nulo y `scope` es el de motor
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
     * Un contexto descartable con `nn` de ambito de motor, para las variantes de `eval` que
     * reciben un {@link Bindings}.
     *
     * <p>Copia el ambito global del motor (si tiene) y los tres canales, pero **no** el ambito de
     * motor: ese lo aporta `nn`. Lo que el script defina se queda en `nn` y en el contexto nuevo.
     *
     * @throws NullPointerException si `nn` es nulo -- un `eval` con `Bindings` necesita uno
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
