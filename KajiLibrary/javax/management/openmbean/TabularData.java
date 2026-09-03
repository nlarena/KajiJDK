package javax.management.openmbean;

import java.util.Collection;
import java.util.Set;

/**
 * Una tabla de {@link CompositeData} indexados por algunos de sus items.
 *
 * <p>La particularidad frente a un `Map` es que **la clave sale del valor**: el {@link TabularType}
 * dice qué items forman el índice, y {@link #calculateIndex} la calcula. Por eso {@link #put} toma
 * la fila sola, y por eso poner dos filas con la misma clave es
 * {@link KeyAlreadyExistsException} y no un reemplazo -- reemplazar sin querer una fila cuya clave
 * uno no eligió es un error, no una intención.
 *
 * <p>Las claves son `Object[]`: un arreglo con los valores de los items de índice, **en el orden
 * de {@link TabularType#getIndexNames}**. Ese orden es la razón por la que ese método existe.
 */
public interface TabularData {

    /** El tipo de esta tabla. */
    TabularType getTabularType();

    /**
     * La clave que le corresponde a esa fila.
     *
     * @throws NullPointerException si la fila es nula
     * @throws InvalidOpenTypeException si la fila no es del tipo que esta tabla espera
     */
    Object[] calculateIndex(CompositeData value);

    /** Cuántas filas hay. */
    int size();

    /** Si no hay ninguna fila. */
    boolean isEmpty();

    /**
     * Si hay una fila con esa clave.
     *
     * @throws NullPointerException nunca: una clave nula o de largo equivocado da `false`
     */
    boolean containsKey(Object[] key);

    /** Si esa fila está en la tabla. Un nulo da `false`. */
    boolean containsValue(CompositeData value);

    /**
     * La fila con esa clave, o nulo si no hay.
     *
     * @throws NullPointerException si la clave es nula
     * @throws InvalidKeyException si la clave no tiene tantos valores como items de índice, o si
     *     alguno no es del tipo que corresponde
     */
    CompositeData get(Object[] key);

    /**
     * Agrega esa fila.
     *
     * @throws NullPointerException si la fila es nula
     * @throws InvalidOpenTypeException si la fila no es del tipo que esta tabla espera
     * @throws KeyAlreadyExistsException si ya hay una fila con esa clave
     */
    void put(CompositeData value);

    /**
     * Saca la fila con esa clave y la devuelve, o nulo si no había.
     *
     * @throws NullPointerException si la clave es nula
     * @throws InvalidKeyException si la clave no es válida para esta tabla
     */
    CompositeData remove(Object[] key);

    /**
     * Agrega todas esas filas.
     *
     * <p>O entran todas o no entra ninguna: si una falla, la tabla queda como estaba. Es lo que
     * hace que un error a mitad de camino no deje media tabla cargada.
     *
     * @throws InvalidOpenTypeException si alguna fila no es del tipo que la tabla espera
     * @throws KeyAlreadyExistsException si alguna clave ya está, o si dos de las nuevas coinciden
     */
    void putAll(CompositeData[] values);

    /** Vacía la tabla. */
    void clear();

    /** Las claves, cada una como una `List` de sus valores de índice. */
    Set<?> keySet();

    /** Las filas. */
    Collection<?> values();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
