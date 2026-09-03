package javax.script;

/**
 * KajiLibrary's javax.script.Invocable -- la implementa el motor que sabe llamar a lo que el
 * script dejo definido.
 *
 * <p>Tambien es **opcional**, y es lo que separa "evaluar un texto" de "usar el script como
 * biblioteca": despues de evaluar una vez, el que hospeda puede llamar por nombre a las funciones
 * y metodos que quedaron, pasando objetos Java y recibiendo lo que devuelva el script.
 *
 * <p>Los dos {@code getInterface} van un paso mas alla: envuelven al script en una interfaz Java,
 * asi el que hospeda lo llama con tipos, sin nombres en texto. Devuelven **nulo** -- no una
 * excepcion -- cuando el script no tiene todos los metodos que la interfaz pide, que es la
 * respuesta correcta a "se puede o no se puede".
 */
public interface Invocable {

    /**
     * Llama al metodo `name` sobre `thiz`, que tiene que ser un objeto que salio de este motor.
     *
     * @throws ScriptException si la llamada explota
     * @throws NoSuchMethodException si el metodo no existe
     * @throws NullPointerException si `name` o `thiz` son nulos
     * @throws IllegalArgumentException si `thiz` no salio de este motor
     */
    Object invokeMethod(Object thiz, String name, Object... args)
            throws ScriptException, NoSuchMethodException;

    /**
     * Llama a la funcion global `name`.
     *
     * @throws ScriptException si la llamada explota
     * @throws NoSuchMethodException si la funcion no existe
     * @throws NullPointerException si `name` es nulo
     */
    Object invokeFunction(String name, Object... args)
            throws ScriptException, NoSuchMethodException;

    /**
     * Una implementacion de `clasz` hecha con las funciones globales del script, o nulo si el
     * script no las tiene todas.
     *
     * @throws IllegalArgumentException si `clasz` es nulo o no es una interfaz
     */
    <T> T getInterface(Class<T> clasz);

    /**
     * Una implementacion de `clasz` hecha con los metodos de `thiz`, o nulo si no estan todos.
     *
     * @throws IllegalArgumentException si `clasz` es nulo o no es una interfaz, o si `thiz` no
     *     salio de este motor
     * @throws NullPointerException si `thiz` es nulo
     */
    <T> T getInterface(Object thiz, Class<T> clasz);
}
