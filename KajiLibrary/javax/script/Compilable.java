package javax.script;

import java.io.Reader;

/**
 * KajiLibrary's javax.script.Compilable -- la implementa el motor que sabe compilar una vez.
 *
 * <p>Es **opcional**: un motor la implementa si puede guardar la forma ya analizada de un script
 * para reejecutarla sin volver a parsear. Quien hospeda pregunta con `instanceof` y usa el camino
 * rapido si esta; si no esta, evalua el texto todas las veces y funciona igual, solo que mas lento.
 *
 * <p>Lo que sale es un {@link CompiledScript}, que no guarda un mundo: se compila una vez y se
 * evalua muchas, cada vez contra el contexto o los `Bindings` que se le pasen.
 */
public interface Compilable {

    /**
     * Compila `script`.
     *
     * @throws ScriptException si el script no compila
     * @throws NullPointerException si `script` es nulo
     */
    CompiledScript compile(String script) throws ScriptException;

    /**
     * Compila lo que salga de `script`.
     *
     * @throws ScriptException si el script no compila o el lector falla
     * @throws NullPointerException si `script` es nulo
     */
    CompiledScript compile(Reader script) throws ScriptException;
}
