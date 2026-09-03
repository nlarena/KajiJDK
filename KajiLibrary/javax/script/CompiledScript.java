package javax.script;

/**
 * KajiLibrary's javax.script.CompiledScript -- un script ya analizado, listo para reejecutar.
 *
 * <p>Lo devuelve {@link Compilable#compile(String)} y no guarda ningun mundo: guarda la forma
 * compilada y nada mas. El mundo se lo pasa quien evalua, cada vez, lo que permite compilar una
 * vez y correr el mismo script contra contextos distintos.
 *
 * <p>De los cuatro metodos, dos son abstractos ({@link #eval(ScriptContext)} y
 * {@link #getEngine()}) y los otros dos se escriben con ellos. El interesante es
 * {@link #eval(Bindings)}: **no** evalua contra el contexto del motor con los `Bindings` metidos
 * adentro -- arma un contexto temporal con esos `Bindings` de ambito de motor y todo lo demas
 * (global, entrada, salida, error) copiado del contexto del motor. El motor queda como estaba.
 *
 * <p>Un detalle facil de pasar por alto: si `bindings` es nulo, {@link #eval(Bindings)} evalua
 * directamente contra el contexto del motor -- no arma nada temporal, y ahi si lo que el script
 * defina queda en el motor.
 */
public abstract class CompiledScript {

    /** Para las subclases. */
    public CompiledScript() {
    }

    /**
     * Evalua contra `context`.
     *
     * @throws ScriptException si el script explota
     * @throws NullPointerException si `context` es nulo
     */
    public abstract Object eval(ScriptContext context) throws ScriptException;

    /**
     * Evalua con `bindings` de ambito de motor y el resto del mundo copiado del motor.
     *
     * <p>Con `bindings` nulo evalua contra el contexto del motor tal cual.
     *
     * @throws ScriptException si el script explota
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
     * Evalua contra el contexto del motor.
     *
     * @throws ScriptException si el script explota
     */
    public Object eval() throws ScriptException {
        return eval(getEngine().getContext());
    }

    /** El motor del que salio, y contra cuyo contexto se evalua por defecto. */
    public abstract ScriptEngine getEngine();
}
