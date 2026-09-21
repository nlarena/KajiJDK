package javax.management.openmbean;

import java.util.Collection;

/**
 * A composite value: named items, each of its own open type.
 *
 * <p>It is <b>read-only</b>, and that is on purpose: a {@code CompositeData} describes the state of
 * something at a given moment and travels to a remote client. If it had setters, a client might
 * believe that changing it changes the MBean on the other side, which is exactly what does not
 * happen.
 *
 * <p>Two {@code CompositeData} are equal if they have the same {@link CompositeType} and the same
 * values. The class implementing them does not count: a {@link CompositeDataSupport} can be equal
 * to any other implementation, and it has to be so that the comparison survives serialization.
 */
public interface CompositeData {

    /** The type of this value. */
    CompositeType getCompositeType();

    /**
     * The value of that item.
     *
     * @throws IllegalArgumentException if the name is null or empty
     * @throws InvalidKeyException if there is no item with that name
     */
    Object get(String key);

    /**
     * The values of those items, in the same order they were asked for.
     *
     * @throws IllegalArgumentException if the array or any of its names is null or empty
     * @throws InvalidKeyException if one is not an item of this value
     */
    Object[] getAll(String[] keys);

    /** Whether there is an item with that name. A null gives {@code false}, not an error. */
    boolean containsKey(String key);

    /** Whether any of the items has that value. */
    boolean containsValue(Object value);

    /**
     * The values, <b>in the order of the item names</b>.
     *
     * <p>That order is the one {@link CompositeType#keySet} exposes, so the value at position
     * {@code i} corresponds to the name at position {@code i} of that set. It is not the order the
     * value was built in.
     */
    Collection<?> values();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
