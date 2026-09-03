package javax.script;

import java.io.Reader;

/**
 * KajiLibrary's javax.script.ScriptEngine -- el que evalua texto y devuelve un objeto.
 *
 * <p>La interfaz que implementa cada lenguaje que se quiera hospedar. Todo lo demas del paquete
 * gira alrededor de esta: el manager la encuentra, la fabrica la construye, el contexto le dice
 * que ve, y los `Bindings` son por donde entran y salen los valores.
 *
 * <p>Los seis `eval` son tres decisiones cruzadas: **de donde sale el texto** ({@code String} o
 * {@link Reader}) y **contra que mundo se evalua** (el contexto que ya tiene el motor, un
 * {@link Bindings} suelto, o un {@link ScriptContext} entero). La version con `Bindings` no
 * reemplaza el contexto del motor: arma uno nuevo con ese ambito de motor y deja el resto, asi que
 * lo que el script defina ahi no queda pegado al motor. Ver
 * {@link AbstractScriptEngine#getScriptContext(Bindings)}, que es donde eso esta escrito.
 *
 * <p>Las siete constantes son claves reservadas de `Bindings`: el que hospeda las lee para saber
 * contra que esta hablando, y {@link #FILENAME} y {@link #ARGV} las escribe para que el motor las
 * use. Todas empiezan con `javax.script.` justamente para no chocar con un nombre de variable del
 * script.
 */
public interface ScriptEngine {

    /** Clave reservada: los argumentos del script, un arreglo. */
    String ARGV = "javax.script.argv";

    /** Clave reservada: el nombre del archivo del que salio el script. */
    String FILENAME = "javax.script.filename";

    /** Clave reservada: el nombre del motor. */
    String ENGINE = "javax.script.engine";

    /** Clave reservada: la version del motor. */
    String ENGINE_VERSION = "javax.script.engine_version";

    /** Clave reservada: el nombre corto con el que se pide el motor. */
    String NAME = "javax.script.name";

    /** Clave reservada: el nombre del lenguaje. */
    String LANGUAGE = "javax.script.language";

    /** Clave reservada: la version del lenguaje. */
    String LANGUAGE_VERSION = "javax.script.language_version";

    /**
     * Evalua `script` contra `context`.
     *
     * @throws ScriptException si el script no compila o explota
     * @throws NullPointerException si algun argumento es nulo
     */
    Object eval(String script, ScriptContext context) throws ScriptException;

    /**
     * Evalua lo que salga de `reader` contra `context`.
     *
     * @throws ScriptException si el script no compila o explota
     * @throws NullPointerException si algun argumento es nulo
     */
    Object eval(Reader reader, ScriptContext context) throws ScriptException;

    /**
     * Evalua `script` contra el contexto que ya tiene el motor.
     *
     * @throws ScriptException si el script no compila o explota
     * @throws NullPointerException si `script` es nulo
     */
    Object eval(String script) throws ScriptException;

    /**
     * Evalua lo que salga de `reader` contra el contexto que ya tiene el motor.
     *
     * @throws ScriptException si el script no compila o explota
     * @throws NullPointerException si `reader` es nulo
     */
    Object eval(Reader reader) throws ScriptException;

    /**
     * Evalua `script` con `n` como ambito de motor, sin tocar el contexto del motor.
     *
     * @throws ScriptException si el script no compila o explota
     * @throws NullPointerException si algun argumento es nulo
     */
    Object eval(String script, Bindings n) throws ScriptException;

    /**
     * Evalua lo que salga de `reader` con `n` como ambito de motor.
     *
     * @throws ScriptException si el script no compila o explota
     * @throws NullPointerException si algun argumento es nulo
     */
    Object eval(Reader reader, Bindings n) throws ScriptException;

    /**
     * Define `key` en el ambito de motor del contexto del motor.
     *
     * @throws NullPointerException si `key` es nulo
     * @throws IllegalArgumentException si `key` es vacio
     */
    void put(String key, Object value);

    /**
     * Lo que valga `key` en el ambito de motor, o nulo.
     *
     * @throws NullPointerException si `key` es nulo
     * @throws IllegalArgumentException si `key` es vacio
     */
    Object get(String key);

    /**
     * El {@link Bindings} de ese ambito, o nulo si no tiene ninguno puesto.
     *
     * @throws IllegalArgumentException si `scope` no es un ambito valido
     */
    Bindings getBindings(int scope);

    /**
     * Pone `bindings` como ese ambito.
     *
     * @throws IllegalArgumentException si `scope` no es valido
     * @throws NullPointerException si `bindings` es nulo y el ambito no lo admite
     */
    void setBindings(Bindings bindings, int scope);

    /** Un {@link Bindings} vacio del tipo que este motor prefiera. */
    Bindings createBindings();

    /** El contexto contra el que se evalua cuando no se pasa otro. */
    ScriptContext getContext();

    /**
     * Cambia ese contexto.
     *
     * @throws NullPointerException si `context` es nulo
     */
    void setContext(ScriptContext context);

    /** La fabrica de la que salio este motor. */
    ScriptEngineFactory getFactory();
}
