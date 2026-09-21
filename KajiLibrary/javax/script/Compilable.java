package javax.script;

import java.io.Reader;

/**
 * KajiLibrary's javax.script.Compilable -- implemented by the engine that can compile once.
 *
 * <p>It is **optional**: an engine implements it if it can keep the already parsed form of a script
 * to run it again without parsing again. The host asks with `instanceof` and takes the fast road if
 * it is there; if it is not, it evaluates the text every time and it works all the same, only
 * slower.
 *
 * <p>What comes out is a {@link CompiledScript}, which keeps no world: it is compiled once and
 * evaluated many times, each time against the context or the `Bindings` it is given.
 */
public interface Compilable {

    /**
     * Compiles `script`.
     *
     * @throws ScriptException if the script does not compile
     * @throws NullPointerException if `script` is null
     */
    CompiledScript compile(String script) throws ScriptException;

    /**
     * Compiles whatever comes out of `script`.
     *
     * @throws ScriptException if the script does not compile or the reader fails
     * @throws NullPointerException if `script` is null
     */
    CompiledScript compile(Reader script) throws ScriptException;
}
