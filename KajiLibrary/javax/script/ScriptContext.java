package javax.script;

import java.io.Reader;
import java.io.Writer;
import java.util.List;

/**
 * KajiLibrary's javax.script.ScriptContext -- todo lo que un script ve del mundo de afuera.
 *
 * <p>Dos cosas: los **ambitos**, que son {@link Bindings} apilados y numerados, y los tres canales
 * de texto (entrada, salida, error) que el script usa cuando lee o imprime. Un motor que evalua
 * siempre lo hace contra un contexto; cambiar el contexto es cambiar el mundo sin tocar el motor.
 *
 * <p>Los ambitos se buscan **de menor a mayor numero**, y ese es todo el sentido de que sean
 * numeros y no nombres: {@link #ENGINE_SCOPE} vale 100 y {@link #GLOBAL_SCOPE} 200, asi que lo que
 * el motor definio tapa lo que puso el que hospeda, y hay lugar libre entre medio y arriba para
 * que una implementacion meta ambitos propios. {@link #getScopes()} dice cuales existen; pedir uno
 * que no esta es un {@link IllegalArgumentException}, no un nulo.
 *
 * <p>La diferencia entre los dos ambitos que define esta interfaz importa: el de motor es de un
 * solo motor y siempre existe; el global lo comparten todos los motores que salieron del mismo
 * {@link ScriptEngineManager} y **puede no estar**, en cuyo caso {@link #getBindings(int)}
 * devuelve nulo sin que eso sea un error.
 */
public interface ScriptContext {

    /**
     * El ambito del motor: lo que este motor tiene definido, y lo primero que se mira.
     *
     * <p>Vive lo que vive el motor y no lo comparte con nadie.
     */
    int ENGINE_SCOPE = 100;

    /**
     * El ambito global: lo que comparten todos los motores del mismo manager.
     *
     * <p>Se mira despues del de motor, asi que cualquier definicion del script lo tapa.
     */
    int GLOBAL_SCOPE = 200;

    /**
     * Pone `bindings` como el ambito `scope`.
     *
     * @throws IllegalArgumentException si `scope` no es un ambito de este contexto
     * @throws NullPointerException si `bindings` es nulo y el ambito no admite estar vacio
     */
    void setBindings(Bindings bindings, int scope);

    /**
     * El {@link Bindings} de ese ambito, o nulo si el ambito existe pero no tiene ninguno puesto.
     *
     * @throws IllegalArgumentException si `scope` no es un ambito de este contexto
     */
    Bindings getBindings(int scope);

    /**
     * Define `name` con `value` en ese ambito.
     *
     * @throws IllegalArgumentException si `name` es vacio o `scope` no existe
     * @throws NullPointerException si `name` es nulo
     */
    void setAttribute(String name, Object value, int scope);

    /**
     * El valor de `name` en ese ambito, o nulo.
     *
     * @throws IllegalArgumentException si `name` es vacio o `scope` no existe
     * @throws NullPointerException si `name` es nulo
     */
    Object getAttribute(String name, int scope);

    /**
     * Saca `name` de ese ambito y devuelve lo que tenia.
     *
     * @throws IllegalArgumentException si `name` es vacio o `scope` no existe
     * @throws NullPointerException si `name` es nulo
     */
    Object removeAttribute(String name, int scope);

    /**
     * El valor de `name` en el ambito de menor numero que lo tenga, o nulo si no esta en ninguno.
     *
     * @throws IllegalArgumentException si `name` es vacio
     * @throws NullPointerException si `name` es nulo
     */
    Object getAttribute(String name);

    /**
     * El numero del primer ambito que define `name`, o -1 si no lo define ninguno.
     *
     * @throws IllegalArgumentException si `name` es vacio
     * @throws NullPointerException si `name` es nulo
     */
    int getAttributesScope(String name);

    /** Donde escribe el script su salida. */
    Writer getWriter();

    /** Donde escribe el script sus errores. */
    Writer getErrorWriter();

    /** Cambia la salida. */
    void setWriter(Writer writer);

    /** Cambia la salida de errores. */
    void setErrorWriter(Writer writer);

    /** De donde lee el script su entrada. */
    Reader getReader();

    /** Cambia la entrada. */
    void setReader(Reader reader);

    /** Los numeros de todos los ambitos de este contexto, de menor a mayor. */
    List<Integer> getScopes();
}
