package javax.script;

import java.io.Reader;
import java.io.Writer;
import java.util.List;

/**
 * KajiLibrary's javax.script.ScriptContext -- everything a script sees of the outside world.
 *
 * <p>Two things: the **scopes**, which are stacked and numbered {@link Bindings}, and the three
 * text streams (input, output, error) the script uses when it reads or prints. An engine that
 * evaluates always does so against a context; changing the context is changing the world without
 * touching the engine.
 *
 * <p>The scopes are searched **from lower to higher number**, and that is the whole point of their
 * being numbers and not names: {@link #ENGINE_SCOPE} is 100 and {@link #GLOBAL_SCOPE} 200, so what
 * the engine defined hides what the host put, and there is free room in between and above for an
 * implementation to put scopes of its own. {@link #getScopes()} says which exist; asking for one
 * that is not there is an {@link IllegalArgumentException}, not a null.
 *
 * <p>The difference between the two scopes this interface defines matters: the engine one belongs
 * to a single engine and always exists; the global one is shared by all the engines that came out
 * of the same {@link ScriptEngineManager} and **may be absent**, in which case {@link
 * #getBindings(int)} returns null without that being an error.
 */
public interface ScriptContext {

    /**
     * The engine scope: what this engine has defined, and the first thing looked at.
     *
     * <p>It lives as long as the engine and is not shared with anybody.
     */
    int ENGINE_SCOPE = 100;

    /**
     * The global scope: what all the engines of the same manager share.
     *
     * <p>It is looked at after the engine one, so any definition of the script hides it.
     */
    int GLOBAL_SCOPE = 200;

    /**
     * Sets `bindings` as the scope `scope`.
     *
     * @throws IllegalArgumentException if `scope` is not a scope of this context
     * @throws NullPointerException if `bindings` is null and the scope does not admit being empty
     */
    void setBindings(Bindings bindings, int scope);

    /**
     * The {@link Bindings} of that scope, or null if the scope exists but has none set.
     *
     * @throws IllegalArgumentException if `scope` is not a scope of this context
     */
    Bindings getBindings(int scope);

    /**
     * Defines `name` with `value` in that scope.
     *
     * @throws IllegalArgumentException if `name` is empty or `scope` does not exist
     * @throws NullPointerException if `name` is null
     */
    void setAttribute(String name, Object value, int scope);

    /**
     * The value of `name` in that scope, or null.
     *
     * @throws IllegalArgumentException if `name` is empty or `scope` does not exist
     * @throws NullPointerException if `name` is null
     */
    Object getAttribute(String name, int scope);

    /**
     * Removes `name` from that scope and returns what it held.
     *
     * @throws IllegalArgumentException if `name` is empty or `scope` does not exist
     * @throws NullPointerException if `name` is null
     */
    Object removeAttribute(String name, int scope);

    /**
     * The value of `name` in the lowest-numbered scope that has it, or null if it is in none.
     *
     * @throws IllegalArgumentException if `name` is empty
     * @throws NullPointerException if `name` is null
     */
    Object getAttribute(String name);

    /**
     * The number of the first scope that defines `name`, or -1 if none defines it.
     *
     * @throws IllegalArgumentException if `name` is empty
     * @throws NullPointerException if `name` is null
     */
    int getAttributesScope(String name);

    /** Where the script writes its output. */
    Writer getWriter();

    /** Where the script writes its errors. */
    Writer getErrorWriter();

    /** Changes the output. */
    void setWriter(Writer writer);

    /** Changes the error output. */
    void setErrorWriter(Writer writer);

    /** Where the script reads its input from. */
    Reader getReader();

    /** Changes the input. */
    void setReader(Reader reader);

    /** The numbers of all the scopes of this context, from lower to higher. */
    List<Integer> getScopes();
}
