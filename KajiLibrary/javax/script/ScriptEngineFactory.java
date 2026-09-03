package javax.script;

import java.util.List;

/**
 * KajiLibrary's javax.script.ScriptEngineFactory -- la ficha tecnica de un motor, y como hacerlo.
 *
 * <p>Es lo que un lenguaje publica como servicio: {@link ScriptEngineManager} carga las fabricas
 * por {@link java.util.ServiceLoader}, les pregunta sus nombres, extensiones y tipos MIME, y
 * cuando alguna coincide con lo que se pidio le manda {@link #getScriptEngine()}. El motor no se
 * construye hasta ese momento -- por eso la fabrica y el motor son dos cosas separadas.
 *
 * <p>Los tres metodos del final ({@link #getMethodCallSyntax}, {@link #getOutputStatement},
 * {@link #getProgram}) no describen: **generan codigo**. Existen para que un programa que hospeda
 * pueda armar un script sin saber en que lenguaje esta escribiendo -- pedirle a la fabrica "como
 * se llama a este metodo aca" en vez de concatenar puntos y parentesis a mano.
 */
public interface ScriptEngineFactory {

    /** El nombre completo del motor, para mostrar. */
    String getEngineName();

    /** La version del motor. */
    String getEngineVersion();

    /** Las extensiones de archivo que este motor atiende, sin el punto. */
    List<String> getExtensions();

    /** Los tipos MIME que este motor atiende. */
    List<String> getMimeTypes();

    /** Los nombres cortos con los que se puede pedir este motor. */
    List<String> getNames();

    /** El nombre del lenguaje que el motor interpreta. */
    String getLanguageName();

    /** La version del lenguaje. */
    String getLanguageVersion();

    /**
     * El valor de una propiedad de la fabrica, o nulo si no la conoce.
     *
     * <p>Las claves que toda fabrica entiende son las constantes de {@link ScriptEngine}, mas
     * `"THREADING"`, que dice si el motor se puede usar desde varios hilos.
     */
    Object getParameter(String key);

    /**
     * El texto de una llamada a metodo en este lenguaje.
     *
     * @param obj el objeto receptor
     * @param m el nombre del metodo
     * @param args los argumentos, ya como texto del lenguaje
     */
    String getMethodCallSyntax(String obj, String m, String... args);

    /** El texto de una sentencia que imprime `toDisplay`. */
    String getOutputStatement(String toDisplay);

    /** Un programa completo hecho de esas sentencias, con lo que el lenguaje pida alrededor. */
    String getProgram(String... statements);

    /**
     * Un motor nuevo de esta fabrica.
     *
     * <p>Cada llamada devuelve uno distinto: dos motores no comparten ambito de motor.
     */
    ScriptEngine getScriptEngine();
}
