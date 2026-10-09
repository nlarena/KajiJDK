package java.util;

// An immutable key/value pair. Package-private: it backs Map.entry(k, v) and the entries the
// Map.of(...) factories hold, and the contract only ever promises a Map.Entry back.
//
// setValue throws rather than mutating, which is what the JDK's own Map.entry() pair does: an
// entry handed out by a factory is not a view onto anything, so "setting" it could only mean
// changing a value nobody can observe.
final class FixedEntry<K, V> implements Map.Entry<K, V> {

    private final K key;
    private final V value;

    FixedEntry(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
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

    // Two entries are equal when both halves are (§Map.Entry): the definition the JDK gives, and
    // what makes an entry usable as a set element.
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Map.Entry)) {
            return false;
        }
        Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
        return this.key.equals(other.getKey()) && this.value.equals(other.getValue());
    }

    // key.hashCode() ^ value.hashCode(), exactly as Map.Entry specifies.
    public int hashCode() {
        return this.key.hashCode() ^ this.value.hashCode();
    }

    public String toString() {
        return this.key + "=" + this.value;
    }
}
