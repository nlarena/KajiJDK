package javax.script;

import java.io.Reader;

/**
 * KajiLibrary's javax.script.ScriptEngine -- the one that evaluates text and returns an object.
 *
 * <p>The interface each language to be hosted implements. Everything else in the package revolves
 * around this one: the manager finds it, the factory builds it, the context tells it what it sees,
 * and the `Bindings` are where values go in and out.
 *
 * <p>The six `eval`s are two crossed decisions: **where the text comes from** ({@code String} or
 * {@link Reader}) and **against which world it is evaluated** (the context the engine already has,
 * a loose {@link Bindings}, or a whole {@link ScriptContext}). The version with `Bindings` does not
 * replace the engine's context: it builds a new one with that engine scope and leaves the rest, so
 * what the script defines there does not stick to the engine. See {@link
 * AbstractScriptEngine#getScriptContext(Bindings)}, which is where that is written.
 *
 * <p>The seven constants are reserved `Bindings` keys: the host reads them to know what it is
 * talking to, and writes {@link #FILENAME} and {@link #ARGV} for the engine to use. They all start
 * with `javax.script.` precisely so as not to clash with a script variable name.
 */
public interface ScriptEngine {

    /** Reserved key: the script's arguments, an array. */
    String ARGV = "javax.script.argv";

    /** Reserved key: the name of the file the script came from. */
    String FILENAME = "javax.script.filename";

    /** Reserved key: the engine's name. */
    String ENGINE = "javax.script.engine";

    /** Reserved key: the engine's version. */
    String ENGINE_VERSION = "javax.script.engine_version";

    /** Reserved key: the short name the engine is asked for by. */
    String NAME = "javax.script.name";

    /** Reserved key: the language's name. */
    String LANGUAGE = "javax.script.language";

    /** Reserved key: the language's version. */
    String LANGUAGE_VERSION = "javax.script.language_version";

    /**
     * Evaluates `script` against `context`.
     *
     * @throws ScriptException if the script does not compile or blows up
     * @throws NullPointerException if any argument is null
     */
    Object eval(String script, ScriptContext context) throws ScriptException;

    /**
     * Evaluates whatever comes out of `reader` against `context`.
     *
     * @throws ScriptException if the script does not compile or blows up
     * @throws NullPointerException if any argument is null
     */
    Object eval(Reader reader, ScriptContext context) throws ScriptException;

    /**
     * Evaluates `script` against the context the engine already has.
     *
     * @throws ScriptException if the script does not compile or blows up
     * @throws NullPointerException if `script` is null
     */
    Object eval(String script) throws ScriptException;

    /**
     * Evaluates whatever comes out of `reader` against the context the engine already has.
     *
     * @throws ScriptException if the script does not compile or blows up
     * @throws NullPointerException if `reader` is null
     */
    Object eval(Reader reader) throws ScriptException;

    /**
     * Evaluates `script` with `n` as engine scope, without touching the engine's context.
     *
     * @throws ScriptException if the script does not compile or blows up
     * @throws NullPointerException if any argument is null
     */
    Object eval(String script, Bindings n) throws ScriptException;

    /**
     * Evaluates whatever comes out of `reader` with `n` as engine scope.
     *
     * @throws ScriptException if the script does not compile or blows up
     * @throws NullPointerException if any argument is null
     */
    Object eval(Reader reader, Bindings n) throws ScriptException;

    /**
     * Defines `key` in the engine scope of the engine's context.
     *
     * @throws NullPointerException if `key` is null
     * @throws IllegalArgumentException if `key` is empty
     */
    void put(String key, Object value);

    /**
     * Whatever `key` is worth in the engine scope, or null.
     *
     * @throws NullPointerException if `key` is null
     * @throws IllegalArgumentException if `key` is empty
     */
    Object get(String key);

    /**
     * The {@link Bindings} of that scope, or null if it has none set.
     *
     * @throws IllegalArgumentException if `scope` is not a valid scope
     */
    Bindings getBindings(int scope);

    /**
     * Sets `bindings` as that scope.
     *
     * @throws IllegalArgumentException if `scope` is not valid
     * @throws NullPointerException if `bindings` is null and the scope does not admit it
     */
    void setBindings(Bindings bindings, int scope);

    /** An empty {@link Bindings} of whatever type this engine prefers. */
    Bindings createBindings();

    /** The context evaluated against when no other is passed. */
    ScriptContext getContext();

    /**
     * Changes that context.
     *
     * @throws NullPointerException if `context` is null
     */
    void setContext(ScriptContext context);

    /** The factory this engine came out of. */
    ScriptEngineFactory getFactory();
}
