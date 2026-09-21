package javax.management.openmbean;

import java.util.Collection;
import java.util.Set;

/**
 * A table of {@link CompositeData} indexed by some of their items.
 *
 * <p>What sets it apart from a {@code Map} is that <b>the key comes from the value</b>: the
 * {@link TabularType} says which items make up the index, and {@link #calculateIndex} computes it.
 * That is why {@link #put} takes the row alone, and why putting two rows with the same key is a
 * {@link KeyAlreadyExistsException} and not a replacement -- accidentally replacing a row whose key
 * you did not choose is an error, not an intention.
 *
 * <p>The keys are {@code Object[]}: an array with the values of the index items, <b>in the order of
 * {@link TabularType#getIndexNames}</b>. That order is the reason that method exists.
 */
public interface TabularData {

    /** The type of this table. */
    TabularType getTabularType();

    /**
     * The key that corresponds to that row.
     *
     * @throws NullPointerException if the row is null
     * @throws InvalidOpenTypeException if the row is not of the type this table expects
     */
    Object[] calculateIndex(CompositeData value);

    /** How many rows there are. */
    int size();

    /** Whether there is no row at all. */
    boolean isEmpty();

    /**
     * Whether there is a row with that key.
     *
     * @throws NullPointerException never: a null key or one of the wrong length gives {@code false}
     */
    boolean containsKey(Object[] key);

    /** Whether that row is in the table. A null gives {@code false}. */
    boolean containsValue(CompositeData value);

    /**
     * The row with that key, or null if there is none.
     *
     * @throws NullPointerException if the key is null
     * @throws InvalidKeyException if the key does not have as many values as there are index items,
     *     or if one is not of the right type
     */
    CompositeData get(Object[] key);

    /**
     * Adds that row.
     *
     * @throws NullPointerException if the row is null
     * @throws InvalidOpenTypeException if the row is not of the type this table expects
     * @throws KeyAlreadyExistsException if there is already a row with that key
     */
    void put(CompositeData value);

    /**
     * Removes the row with that key and returns it, or null if there was none.
     *
     * @throws NullPointerException if the key is null
     * @throws InvalidKeyException if the key is not valid for this table
     */
    CompositeData remove(Object[] key);

    /**
     * Adds all those rows.
     *
     * <p>Either they all go in or none does: if one fails, the table is left as it was. It is what
     * keeps an error halfway through from leaving the table half loaded.
     *
     * @throws InvalidOpenTypeException if some row is not of the type the table expects
     * @throws KeyAlreadyExistsException if some key is already there, or if two of the new ones
     *     match
     */
    void putAll(CompositeData[] values);

    /** Empties the table. */
    void clear();

    /** The keys, each as a {@code List} of its index values. */
    Set<?> keySet();

    /** The rows. */
    Collection<?> values();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
