package java.util;

// The pair the maps' `entrySet()` views return: immutable and **null-tolerant**.
//
// It exists because of a concrete bug: the views used to use `FixedEntry`, which rejects nulls in its
// constructor. That is correct for `Map.entry(k, v)` --the JDK rejects them there too-- but it is
// false for a view: a `HashMap` or a `TreeMap` CAN hold a null value, and with `FixedEntry` asking
// them for `entrySet()` threw `NullPointerException` instead of returning the pair. The symptom was
// baffling because the exception did not come out of where the null was but out of walking the map.
//
// The key does have to be non-null in a `TreeMap`, but that is not this class's business: the map is
// what requires it, on putting. Here whatever the map stored is accepted.
//
// `setValue` throws, as in `FixedEntry`: these views are copies, not windows, so writing into a pair
// would not reach the map and keeping quiet would be worse than refusing.
final class ViewEntry<K, V> implements Map.Entry<K, V> {

    private final K key;
    private final V value;

    ViewEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return this.key;
    }

    public V getValue() {
        return this.value;
    }

    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    // Two pairs are equal when both their halves are (§Map.Entry). With nulls: two nulls are equal to
    // each other, which is what the contract says and what `FixedEntry` could not express.
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Map.Entry)) {
            return false;
        }
        Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
        return same(this.key, other.getKey()) && same(this.value, other.getValue());
    }

    private static boolean same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    // key.hashCode() ^ value.hashCode(), with zero for null: it is exactly what `Map.Entry.hashCode()`
    // specifies.
    public int hashCode() {
        int k = this.key == null ? 0 : this.key.hashCode();
        int v = this.value == null ? 0 : this.value.hashCode();
        return k ^ v;
    }

    public String toString() {
        return this.key + "=" + this.value;
    }
}
