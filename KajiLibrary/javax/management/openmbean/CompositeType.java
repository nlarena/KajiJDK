package javax.management.openmbean;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The type of a {@link CompositeData}: a set of named items, each with its open type.
 *
 * <p>It is the open equivalent of a class with fields, and the important difference from a class is
 * that <b>identity does not depend on the name but on the shape</b>: two {@code CompositeType} with
 * the same {@code typeName}, the same description and the same items are equal even if built by
 * different people. That is what lets a remote client recognize the type without sharing code.
 *
 * <p>The items are kept <b>sorted by name</b>, not in the order they were passed. It is not an
 * implementation detail: {@link #keySet} exposes it, and the {@code hashCode} the contract defines
 * is the sum of the hashes of names and types, which is also order-independent. Put another way,
 * the order of the constructor's arrays is <b>not</b> part of the type.
 */
public class CompositeType extends OpenType<CompositeData> {

    private static final long serialVersionUID = -5366242454346948798L;

    // Sorted: see the class note about why the input order does not count.
    private final Map<String, String> descriptions;
    private final Map<String, OpenType<?>> types;

    // Computed once because it is immutable and because a `CompositeType` is often used as a map
    // key --every `CompositeDataSupport` consults its own.
    private transient int hash;

    /**
     * A composite type with those items.
     *
     * <p>The three arrays go in parallel: item {@code i} is called {@code itemNames[i]}, is
     * described by {@code itemDescriptions[i]} and is of type {@code itemTypes[i]}.
     *
     * @throws OpenDataException if there are repeated names
     * @throws IllegalArgumentException if some array is null or empty, if they do not have the same
     *     length, or if some element is null or a blank string
     */
    public CompositeType(String typeName, String description, String[] itemNames,
            String[] itemDescriptions, OpenType<?>[] itemTypes) throws OpenDataException {
        super(CompositeData.class.getName(), typeName, description);

        if (itemNames == null || itemDescriptions == null || itemTypes == null) {
            throw new IllegalArgumentException("the item arrays cannot be null");
        }
        if (itemNames.length == 0) {
            throw new IllegalArgumentException("a composite type needs at least one item");
        }
        if (itemNames.length != itemDescriptions.length
                || itemNames.length != itemTypes.length) {
            throw new IllegalArgumentException(
                    "the three item arrays must have the same length");
        }

        Map<String, String> ds = new TreeMap<String, String>();
        Map<String, OpenType<?>> ts = new TreeMap<String, OpenType<?>>();
        for (int i = 0; i < itemNames.length; i++) {
            String n = itemNames[i];
            if (n == null || n.trim().length() == 0) {
                throw new IllegalArgumentException("the name of item " + i + " is blank");
            }
            n = n.trim();
            if (itemDescriptions[i] == null || itemDescriptions[i].trim().length() == 0) {
                throw new IllegalArgumentException(
                        "the description of item " + n + " is blank");
            }
            if (itemTypes[i] == null) {
                throw new IllegalArgumentException("the type of item " + n + " is null");
            }
            // A repeat is `OpenDataException` and not `IllegalArgumentException`: the JDK tells
            // them apart that way, and it makes sense -- names may come from data, nulls are a
            // caller's error.
            if (ts.containsKey(n)) {
                throw new OpenDataException("the item " + n + " is repeated");
            }
            ds.put(n, itemDescriptions[i].trim());
            ts.put(n, itemTypes[i]);
        }
        this.descriptions = Collections.unmodifiableMap(ds);
        this.types = Collections.unmodifiableMap(ts);
    }

    /**
     * Whether there is an item with that name. A null or an empty string gives {@code false}, not
     * an error.
     */
    public boolean containsKey(String itemName) {
        if (itemName == null) {
            return false;
        }
        return this.types.containsKey(itemName);
    }

    /** The description of that item, or null if it does not exist. */
    public String getDescription(String itemName) {
        if (itemName == null) {
            return null;
        }
        return this.descriptions.get(itemName);
    }

    /** The type of that item, or null if it does not exist. */
    public OpenType<?> getType(String itemName) {
        if (itemName == null) {
            return null;
        }
        return this.types.get(itemName);
    }

    /** The item names, sorted and read-only. */
    public Set<String> keySet() {
        return this.types.keySet();
    }

    /**
     * Whether {@code obj} is a {@link CompositeData} whose type is this one.
     *
     * <p>It is compared with {@link #equals} and not by identity on purpose: the data may have
     * arrived from another machine with a rebuilt {@code CompositeType}, and being the same type is
     * a matter of shape, not of object.
     */
    public boolean isValue(Object obj) {
        if (!(obj instanceof CompositeData)) {
            return false;
        }
        return this.equals(((CompositeData) obj).getCompositeType());
    }

    /** Equality by type name and items; the type's description does not count. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CompositeType)) {
            return false;
        }
        CompositeType other = (CompositeType) obj;
        if (!this.getTypeName().equals(other.getTypeName())) {
            return false;
        }
        if (!this.types.keySet().equals(other.types.keySet())) {
            return false;
        }
        for (Map.Entry<String, OpenType<?>> e : this.types.entrySet()) {
            if (!e.getValue().equals(other.types.get(e.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /** The type name plus the names and types of the items. Without the order. */
    public int hashCode() {
        if (this.hash == 0) {
            int h = this.getTypeName().hashCode();
            for (Map.Entry<String, OpenType<?>> e : this.types.entrySet()) {
                h = h + e.getKey().hashCode() + e.getValue().hashCode();
            }
            this.hash = h;
        }
        return this.hash;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CompositeType.class.getName());
        sb.append("(name=").append(this.getTypeName()).append(",items=(");
        boolean first = true;
        // It is walked in name order, which is how they are stored: two equal types print the same
        // even if they were built with the items in another order.
        Map<String, OpenType<?>> ordered = new LinkedHashMap<String, OpenType<?>>(this.types);
        for (Map.Entry<String, OpenType<?>> e : ordered.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("(itemName=").append(e.getKey());
            sb.append(",itemType=").append(e.getValue().toString()).append(")");
        }
        sb.append("))");
        return sb.toString();
    }
}
