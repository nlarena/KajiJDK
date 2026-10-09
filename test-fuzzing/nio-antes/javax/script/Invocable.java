package javax.script;

/**
 * KajiLibrary's javax.script.Invocable -- implemented by the engine that can call what the script
 * left defined.
 *
 * <p>It is also **optional**, and it is what separates "evaluating a text" from "using the script
 * as a library": after evaluating once, the host can call by name the functions and methods that
 * remained, passing Java objects and receiving what the script returns.
 *
 * <p>The two {@code getInterface}s go one step further: they wrap the script in a Java interface,
 * so the host calls it with types, with no names in text. They return **null** -- not an exception
 * -- when the script does not have all the methods the interface asks for, which is the right
 * answer to "can it or can it not".
 */
public interface Invocable {

    /**
     * Calls the method `name` on `thiz`, which has to be an object that came out of this engine.
     *
     * @throws ScriptException if the call blows up
     * @throws NoSuchMethodException if the method does not exist
     * @throws NullPointerException if `name` or `thiz` is null
     * @throws IllegalArgumentException if `thiz` did not come out of this engine
     */
    Object invokeMethod(Object thiz, String name, Object... args)
            throws ScriptException, NoSuchMethodException;

    /**
     * Calls the global function `name`.
     *
     * @throws ScriptException if the call blows up
     * @throws NoSuchMethodException if the function does not exist
     * @throws NullPointerException if `name` is null
     */
    Object invokeFunction(String name, Object... args)
            throws ScriptException, NoSuchMethodException;

    /**
     * An implementation of `clasz` made with the script's global functions, or null if the script
     * does not have them all.
     *
     * @throws IllegalArgumentException if `clasz` is null or is not an interface
     */
    <T> T getInterface(Class<T> clasz);

    /**
     * An implementation of `clasz` made with the methods of `thiz`, or null if not all of them are
     * there.
     *
     * @throws IllegalArgumentException if `clasz` is null or is not an interface, or if `thiz` did
     *     not come out of this engine
     * @throws NullPointerException if `thiz` is null
     */
    <T> T getInterface(Object thiz, Class<T> clasz);
}
