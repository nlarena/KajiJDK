package java.util;

// An immutable Map over two parallel arrays. Package-private: it is what Map.of(...),
// Map.ofEntries(...) and Map.copyOf(...) hand back.
//
// Keys are null-checked and deduplicated at construction: Map.of("a", 1, "a", 2) is an
// IllegalArgumentException in the JDK, not a silent last-one-wins. Same reasoning as FixedSet —
// a duplicate key in a literal is a bug in the literal, and swallowing it hides it.
//
// Parallel arrays rather than an array of entries: the entries a caller can observe are built on
// demand by keySet()/get(), so materialising them up front would allocate for nothing.
final class FixedMap<K, V> implements Map<K, V> {

    private final Object[] keys;
    private final Object[] values;

    private FixedMap(Object[] keys, Object[] values) {
        this.keys = keys;
        this.values = values;
    }

    // Builds a map from `n` alternating key/value slots of `kv`, rejecting nulls and duplicate keys.
    static <K, V> FixedMap<K, V> fromPairs(Object[] kv, int n) {
        Object[] ks = new Object[n / 2];
        Object[] vs = new Object[n / 2];
        int size = 0;
        int i = 0;
        while (i < n) {
            Object k = kv[i];
            Object v = kv[i + 1];
            if (k == null || v == null) {
                throw new NullPointerException();
            }
            int j = 0;
            while (j < size) {
                if (ks[j].equals(k)) {
                    throw new IllegalArgumentException("duplicate key: " + k);
                }
                j = j + 1;
            }
            ks[size] = k;
            vs[size] = v;
            size = size + 1;
            i = i + 2;
        }
        return new FixedMap<K, V>(ks, vs);
    }

    public int size() {
        return this.keys.length;
    }

    public boolean isEmpty() {
        return this.keys.length == 0;
    }

    private int indexOfKey(Object key) {
        int i = 0;
        while (i < this.keys.length) {
            if (this.keys[i].equals(key)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    public boolean containsKey(Object key) {
        return this.indexOfKey(key) >= 0;
    }

    public boolean containsValue(Object value) {
        int i = 0;
        while (i < this.values.length) {
            if (this.values[i].equals(value)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    public V get(Object key) {
        int i = this.indexOfKey(key);
        if (i < 0) {
            return null;
        }
        return (V) this.values[i];
    }

    public Set<K> keySet() {
        return FixedSet.dedup(this.keys, this.keys.length);
    }

    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    // Map equality is by contents, whatever the other implementation is (§Map).
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Map)) {
            return false;
        }
        Map<?, ?> other = (Map<?, ?>) o;
        if (other.size() != this.keys.length) {
            return false;
        }
        int i = 0;
        while (i < this.keys.length) {
            Object v = other.get(this.keys[i]);
            if (v == null || !v.equals(this.values[i])) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // The sum of the per-entry hashes (key ^ value), order-independent, as Map specifies.
    public int hashCode() {
        int h = 0;
        int i = 0;
        while (i < this.keys.length) {
            h = h + (this.keys[i].hashCode() ^ this.values[i].hashCode());
            i = i + 1;
        }
        return h;
    }

    public String toString() {
        String s = "{";
        int i = 0;
        while (i < this.keys.length) {
            if (i > 0) {
                s = s + ", ";
            }
            s = s + this.keys[i] + "=" + this.values[i];
            i = i + 1;
        }
        return s + "}";
    }

    /**
     * This map's values.
     *
     * <p>**A deliberate divergence**, the same one `keySet()` already declares: the JDK's is a *view*
     * backed by the map; this one is a copy taken at the moment of asking. And unlike `keySet()` this
     * is a `Collection` and not a `Set`, because values **can** repeat.
     */
    public java.util.Collection<V> values() {
        java.util.ArrayList<V> out = new java.util.ArrayList<V>();
        java.util.Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            out.add(this.get(it.next()));
        }
        return out;
    }

    /**
     * This map's pairs.
     *
     * <p>The same divergence as `values()`: a copy, not a view. The pairs it returns are immutable,
     * so `setValue` on one of them throws instead of writing into the map — which is what is
     * consistent with it being a copy: writing into a pair nobody looks at would be worse than
     * refusing.
     */
    public java.util.Set<java.util.Map.Entry<K, V>> entrySet() {
        java.util.HashSet<java.util.Map.Entry<K, V>> out =
            new java.util.HashSet<java.util.Map.Entry<K, V>>();
        java.util.Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            out.add(new ViewEntry<K, V>(k, this.get(k)));
        }
        return out;
    }
}
