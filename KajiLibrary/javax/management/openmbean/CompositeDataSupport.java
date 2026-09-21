package javax.management.openmbean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The implementation of {@link CompositeData}: a name-to-value map, frozen on construction.
 *
 * <p>What the constructor does and is worth keeping in mind: it <b>validates every value against
 * its type</b>. An item declared {@code SimpleType.INTEGER} that is given a {@code String} does not
 * go in, and the error comes out when building the value instead of at the other end of the
 * connection. That is all an open type buys, and that is why the constructor throws
 * {@code OpenDataException} instead of trusting.
 *
 * <p>A null <b>is</b> accepted for any item: it means "no value" and is different from the item
 * not existing. {@code containsKey} of an item with a null value returns {@code true}.
 */
public class CompositeDataSupport implements CompositeData, Serializable {

    private static final long serialVersionUID = 8003518976613702244L;

    private final CompositeType compositeType;
    // Sorted by name, like the type's items: `values()` promises that order.
    private final Map<String, Object> contents;

    /**
     * A composite value with those items.
     *
     * <p>The two arrays go in parallel.
     *
     * @throws OpenDataException if an item of the type is missing, if there is an extra one the
     *     type does not have, or if some value is not of the type its item declares
     * @throws IllegalArgumentException if the type or the arrays are null, if they do not have the
     *     same length, or if some name is blank
     */
    public CompositeDataSupport(CompositeType compositeType, String[] itemNames,
            Object[] itemValues) throws OpenDataException {
        this(compositeType, asMap(compositeType, itemNames, itemValues));
    }

    /**
     * A composite value with the items of that map.
     *
     * @throws OpenDataException if an item of the type is missing, if there is an extra one, or if
     *     some value is not of the type its item declares
     * @throws IllegalArgumentException if the type or the map are null, or if some key is blank
     */
    public CompositeDataSupport(CompositeType compositeType, Map<String, ?> items)
            throws OpenDataException {
        if (compositeType == null) {
            throw new IllegalArgumentException("the composite type cannot be null");
        }
        if (items == null) {
            throw new IllegalArgumentException("the item map cannot be null");
        }
        Set<String> expected = compositeType.keySet();
        Map<String, Object> given = new TreeMap<String, Object>();
        for (Map.Entry<String, ?> e : items.entrySet()) {
            String n = e.getKey();
            if (n == null || n.trim().length() == 0) {
                throw new IllegalArgumentException("there is a blank key");
            }
            n = n.trim();
            if (!expected.contains(n)) {
                throw new OpenDataException(
                    n + " is not an item of " + compositeType.getTypeName());
            }
            Object v = e.getValue();
            // A null always passes: it is "no value", and no `isValue` accepts it. Checking it
            // against the type would reject it, which is the opposite of what the contract defines.
            if (v != null && !compositeType.getType(n).isValue(v)) {
                throw new OpenDataException("the value of " + n + " is not of type "
                        + compositeType.getType(n).getTypeName());
            }
            given.put(n, v);
        }
        // A missing item is an error and not an implicit null. The difference matters: a composite
        // value describes something complete, and "I forgot to put this item" and "this item is
        // null" are two different things the reader could not tell apart.
        for (String n : expected) {
            if (!given.containsKey(n)) {
                throw new OpenDataException("missing item: " + n);
            }
        }
        this.compositeType = compositeType;
        this.contents = Collections.unmodifiableMap(given);
    }

    // The map is built before calling the other constructor because `this(...)` has to be the first
    // statement and the arrays' validation has to run before it.
    private static Map<String, Object> asMap(CompositeType compositeType, String[] itemNames,
            Object[] itemValues) throws OpenDataException {
        if (itemNames == null || itemValues == null) {
            throw new IllegalArgumentException("the item arrays cannot be null");
        }
        if (itemNames.length != itemValues.length) {
            throw new IllegalArgumentException(
                    "the name and value arrays must have the same length");
        }
        Map<String, Object> m = new TreeMap<String, Object>();
        for (int i = 0; i < itemNames.length; i++) {
            if (itemNames[i] == null || itemNames[i].trim().length() == 0) {
                throw new IllegalArgumentException("the name of item " + i + " is blank");
            }
            String n = itemNames[i].trim();
            if (m.containsKey(n)) {
                throw new OpenDataException("the item " + n + " is repeated");
            }
            m.put(n, itemValues[i]);
        }
        return m;
    }

    public CompositeType getCompositeType() {
        return this.compositeType;
    }

    public Object get(String key) {
        this.requireItem(key);
        return this.contents.get(key.trim());
    }

    public Object[] getAll(String[] keys) {
        if (keys == null || keys.length == 0) {
            return new Object[0];
        }
        Object[] out = new Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
            out[i] = this.get(keys[i]);
        }
        return out;
    }

    private void requireItem(String key) {
        if (key == null || key.trim().length() == 0) {
            throw new IllegalArgumentException("the item name is blank");
        }
        if (!this.contents.containsKey(key.trim())) {
            throw new InvalidKeyException(key + " is not an item of this value");
        }
    }

    public boolean containsKey(String key) {
        if (key == null) {
            return false;
        }
        return this.contents.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return this.contents.containsValue(value);
    }

    /** The values, in the order of the names. See {@link CompositeData#values}. */
    public Collection<?> values() {
        List<Object> out = new ArrayList<Object>(this.contents.values());
        return Collections.unmodifiableList(out);
    }

    /**
     * Equality by type and values, against <b>any</b> {@link CompositeData}.
     *
     * <p>The class is not compared: see the note in {@link CompositeData}.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CompositeData)) {
            return false;
        }
        CompositeData other = (CompositeData) obj;
        if (!this.compositeType.equals(other.getCompositeType())) {
            return false;
        }
        for (Map.Entry<String, Object> e : this.contents.entrySet()) {
            Object mine = e.getValue();
            Object theirs = other.get(e.getKey());
            if (mine == null ? theirs != null : !deepEquals(mine, theirs)) {
                return false;
            }
        }
        return true;
    }

    // An item may be an array, and `Object.equals` of two different arrays with the same content
    // is `false`. Comparing by content is what makes two equal composite values that travelled
    // separately recognize each other.
    private static boolean deepEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getClass().isArray() && b.getClass().isArray()) {
            if (!a.getClass().equals(b.getClass())) {
                return false;
            }
            int n = java.lang.reflect.Array.getLength(a);
            if (n != java.lang.reflect.Array.getLength(b)) {
                return false;
            }
            for (int i = 0; i < n; i++) {
                if (!deepEquals(java.lang.reflect.Array.get(a, i),
                        java.lang.reflect.Array.get(b, i))) {
                    return false;
                }
            }
            return true;
        }
        return a.equals(b);
    }

    /** The sum of the hashes of the type and of the non-null values, as the contract dictates. */
    public int hashCode() {
        int h = this.compositeType.hashCode();
        for (Object v : this.contents.values()) {
            if (v != null) {
                h = h + hashOf(v);
            }
        }
        return h;
    }

    private static int hashOf(Object v) {
        if (v.getClass().isArray()) {
            int h = 1;
            int n = java.lang.reflect.Array.getLength(v);
            for (int i = 0; i < n; i++) {
                Object e = java.lang.reflect.Array.get(v, i);
                h = 31 * h + (e == null ? 0 : hashOf(e));
            }
            return h;
        }
        return v.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CompositeDataSupport.class.getName());
        sb.append("(compositeType=").append(this.compositeType.toString());
        sb.append(",contents={");
        boolean first = true;
        for (Map.Entry<String, Object> e : this.contents.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(e.getKey()).append("=").append(String.valueOf(e.getValue()));
        }
        sb.append("})");
        return sb.toString();
    }
}
