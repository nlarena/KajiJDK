package javax.management.openmbean;

import java.util.Collection;

/**
 * Un valor compuesto: items con nombre, cada uno de su tipo abierto.
 *
 * <p>Es de **sólo lectura**, y eso es a propósito: un `CompositeData` describe el estado de algo en
 * un momento dado y viaja a un cliente remoto. Si tuviera setters, un cliente podría creer que
 * cambiarlo cambia el MBean del otro lado, que es exactamente lo que no pasa.
 *
 * <p>Dos `CompositeData` son iguales si tienen el mismo {@link CompositeType} y los mismos valores.
 * La clase que los implementa no cuenta: un {@link CompositeDataSupport} puede ser igual a otra
 * implementación cualquiera, y tiene que serlo para que la comparación sobreviva a la
 * serialización.
 */
public interface CompositeData {

    /** El tipo de este valor. */
    CompositeType getCompositeType();

    /**
     * El valor de ese item.
     *
     * @throws IllegalArgumentException si el nombre es nulo o vacío
     * @throws InvalidKeyException si no hay un item con ese nombre
     */
    Object get(String key);

    /**
     * Los valores de esos items, en el mismo orden en que se pidieron.
     *
     * @throws IllegalArgumentException si el arreglo o alguno de sus nombres es nulo o vacío
     * @throws InvalidKeyException si alguno no es un item de este valor
     */
    Object[] getAll(String[] keys);

    /** Si hay un item con ese nombre. Un nulo da `false`, no un error. */
    boolean containsKey(String key);

    /** Si alguno de los items tiene ese valor. */
    boolean containsValue(Object value);

    /**
     * Los valores, **en el orden de los nombres de los items**.
     *
     * <p>Ese orden es el que expone {@link CompositeType#keySet}, así que el valor de la posición
     * `i` corresponde al nombre de la posición `i` de ese conjunto. No es el orden en que se
     * construyó el valor.
     */
    Collection<?> values();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
