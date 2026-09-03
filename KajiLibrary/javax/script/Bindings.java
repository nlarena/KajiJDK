package javax.script;

import java.util.Map;

/**
 * KajiLibrary's javax.script.Bindings -- un mapa de nombres a valores, con las claves acotadas.
 *
 * <p>Es el puente entre el programa que hospeda y el script que corre: lo que el motor ve como
 * variable global sale de aca, y lo que el script deja definido vuelve por el mismo lugar. Por eso
 * la interfaz es un {@code Map<String,Object>} y no algo propio -- el que hospeda ya sabe usar un
 * mapa.
 *
 * <p>Lo unico que agrega sobre {@link Map} es una restriccion sobre las claves: tienen que ser
 * {@code String}, no nulas y no vacias. La restriccion no es decorativa. Un nombre de variable
 * vacio no existe en ningun lenguaje de scripting, y una clave que no sea texto no se puede
 * traducir a un identificador; dejarlas entrar seria posponer el error hasta adentro del motor,
 * donde ya no se sabe de donde vino. Las cinco redeclaraciones de metodos de {@code Map} que hay
 * aca no cambian ninguna firma: estan para poder documentar esas excepciones en el contrato.
 *
 * <p>El {@code put(Object,Object)} por defecto es el que exige {@link Map} despues del borrado de
 * tipos. Castea y delega en {@link #put(String,Object)}, que es la unica forma honesta de
 * atenderlo: si la clave no es un {@code String}, el que se queja es el cast.
 */
public interface Bindings extends Map<String, Object> {

    /**
     * Asocia `value` a `name`.
     *
     * @throws NullPointerException si `name` es nulo
     * @throws IllegalArgumentException si `name` es vacio
     */
    Object put(String name, Object value);

    /**
     * Copia todas las entradas de `toMerge`, cada una con las mismas reglas que
     * {@link #put(String,Object)}.
     *
     * @throws NullPointerException si `toMerge` es nulo, o si alguna clave lo es
     * @throws IllegalArgumentException si alguna clave es vacia
     */
    void putAll(Map<? extends String, ? extends Object> toMerge);

    /**
     * Si hay una entrada con esa clave.
     *
     * @throws NullPointerException si `key` es nulo
     * @throws ClassCastException si `key` no es un `String`
     * @throws IllegalArgumentException si `key` es vacio
     */
    boolean containsKey(Object key);

    /**
     * El valor asociado a `key`, o nulo.
     *
     * @throws NullPointerException si `key` es nulo
     * @throws ClassCastException si `key` no es un `String`
     * @throws IllegalArgumentException si `key` es vacio
     */
    Object get(Object key);

    /**
     * Saca la entrada de `key` y devuelve lo que tenia.
     *
     * @throws NullPointerException si `key` es nulo
     * @throws ClassCastException si `key` no es un `String`
     * @throws IllegalArgumentException si `key` es vacio
     */
    Object remove(Object key);

    /**
     * El {@code put} de {@link Map} visto con tipos borrados: castea y delega.
     *
     * @throws ClassCastException si `key` no es un `String`
     */
    default Object put(Object key, Object value) {
        return put((String) key, value);
    }
}
