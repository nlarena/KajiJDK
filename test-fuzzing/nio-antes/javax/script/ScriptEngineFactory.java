package javax.script;

import java.util.List;

/**
 * KajiLibrary's javax.script.ScriptEngineFactory -- an engine's specification sheet, and how to
 * make one.
 *
 * <p>It is what a language publishes as a service: {@link ScriptEngineManager} loads the factories
 * through {@link java.util.ServiceLoader}, asks them their names, extensions and MIME types, and
 * when one matches what was asked for it calls {@link #getScriptEngine()}. The engine is not built
 * until that moment -- that is why the factory and the engine are two separate things.
 *
 * <p>The three methods at the end ({@link #getMethodCallSyntax}, {@link #getOutputStatement},
 * {@link #getProgram}) do not describe: they **generate code**. They exist so that a hosting
 * program can put a script together without knowing which language it is writing in -- asking the
 * factory "how is this method called here" instead of concatenating dots and parentheses by hand.
 */
public interface ScriptEngineFactory {

    /** The engine's full name, for display. */
    String getEngineName();

    /** The engine's version. */
    String getEngineVersion();

    /** The file extensions this engine serves, without the dot. */
    List<String> getExtensions();

    /** The MIME types this engine serves. */
    List<String> getMimeTypes();

    /** The short names this engine can be asked for by. */
    List<String> getNames();

    /** The name of the language the engine interprets. */
    String getLanguageName();

    /** The language's version. */
    String getLanguageVersion();

    /**
     * The value of a property of the factory, or null if it does not know it.
     *
     * <p>The keys every factory understands are the constants of {@link ScriptEngine}, plus
     * `"THREADING"`, which says whether the engine can be used from several threads.
     */
    Object getParameter(String key);

    /**
     * The text of a method call in this language.
     *
     * @param obj the receiver object
     * @param m the name of the method
     * @param args the arguments, already as text of the language
     */
    String getMethodCallSyntax(String obj, String m, String... args);

    /** The text of a statement that prints `toDisplay`. */
    String getOutputStatement(String toDisplay);

    /**
     * A complete program made of those statements, with whatever the language asks for around them.
     */
    String getProgram(String... statements);

    /**
     * A new engine from this factory.
     *
     * <p>Each call returns a different one: two engines do not share an engine scope.
     */
    ScriptEngine getScriptEngine();
}
