package java.util;

// Compiled with `-cp KajiLibrary` so Map binds to KajiLibrary's own (subset) type.
import java.lang.Cloneable;
import java.io.Serializable;
import java.util.Map;

// KajiLibrary's java.util.HashMap<K,V> — a hash table keyed by `hashCode`/`equals`. This
// implementation uses open addressing with linear probing over two parallel Object[] arrays
// (keys and values), doubling and rehashing past a ~50% load factor. A `null` slot in `keys`
// marks an empty bucket; removal re-inserts the trailing cluster to preserve the probe
// invariant. (Null keys are not supported, unlike the JDK.) Map has no iteration in our subset,
// so no helper class is needed.
public class HashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Serializable, Cloneable {

    /**
     * The null key lives apart from the table.
     *
     * <p>The table is open-addressed and uses <b>null as the empty-slot mark</b>, so a null key
     * cannot be stored there: it would take the slot and at the same time say it was free. And
     * `null.hashCode()` does not exist either, so there is not even a bucket to compute.
     *
     * <p>Accepting it all the same is no whim: that `HashMap` allows a null key is what tells it from
     * `Hashtable`, it is in its contract, and there is code that uses it on purpose --a configuration
     * map where null is "the default value". That is why it goes in two fields off to the side, which
     * is the usual solution for an open table.
     */
    private boolean hasNullKey = false;

    /** The null key's value; it only means anything with {@link #hasNullKey} true. */
    private V nullValue = null;

    private Object[] keys;
    private Object[] values;
    private int size;

    public HashMap() {
        this.keys = new Object[16];
        this.values = new Object[16];
        this.size = 0;
    }

    /**
     * An empty map with room for `initialCapacity` buckets.
     *
     * <p>It serves the usual purpose: if how many pairs will go in is known, sizing up front avoids
     * the rehashes of growth. Mind the name --and it is the same misunderstanding as in the JDK--:
     * `initialCapacity` is the number of **buckets**, not of pairs. With this implementation's load
     * factor (~50 %) about half of them fit before the first rehash. Whoever wants to think in pairs
     * has `newHashMap`.
     *
     * @throws IllegalArgumentException if the capacity is negative
     */
    public HashMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * The same, with a load factor.
     *
     * <p>The `loadFactor` is **validated and ignored**, and it is worth saying so plainly: this table
     * uses open addressing with linear probing and doubles past ~50 %, a threshold that is part of how
     * it is written and not a parameter. Accepting the value and not using it would be lying;
     * rejecting it would break code that compiles against the JDK and only passes the usual 0.75. It
     * is validated --a non-positive factor or NaN is an error, just as in the JDK-- and then
     * discarded, which is the only one of the three options that lies to nobody.
     *
     * @throws IllegalArgumentException if the capacity is negative or the factor is not positive
     */
    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        if (!(loadFactor > 0)) {   // negated, so NaN falls in here
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }
        int cap = 16;
        while (cap < initialCapacity) {
            cap = cap * 2;
        }
        this.keys = new Object[cap];
        this.values = new Object[cap];
        this.size = 0;
    }

    /**
     * A map sized for `numMappings` **pairs**, with no rehashes.
     *
     * <p>It is the one people meant when they wrote `new HashMap<>(n)`: that one takes buckets and
     * this one takes pairs. Java 19 added it precisely because the other was being used wrong.
     *
     * @throws IllegalArgumentException if `numMappings` is negative
     */
    public static <K, V> HashMap<K, V> newHashMap(int numMappings) {
        if (numMappings < 0) {
            throw new IllegalArgumentException("Negative number of mappings: " + numMappings);
        }
        return new HashMap<K, V>(numMappings * 2 + 1);
    }

    // It copies another map's pairs. The usual one for keeping a snapshot of somebody else's map.
    public HashMap(Map<? extends K, ? extends V> m) {
        this.keys = new Object[16];
        this.values = new Object[16];
        this.size = 0;
        this.putAll(m);
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    // The index of `key`'s bucket: the slot holding it, or the first empty slot on its
    // probe sequence if absent.
    private int slotFor(Object key) {
        int cap = this.keys.length;
        int i = key.hashCode() & (cap - 1);
        while (this.keys[i] != null) {
            if (this.keys[i].equals(key)) {
                return i;
            }
            i = (i + 1) & (cap - 1);
        }
        return i;
    }

    public V get(Object key) {
        if (key == null) {
            return this.hasNullKey ? this.nullValue : null;
        }
        return (V) this.values[this.slotFor(key)];
    }

    public boolean containsKey(Object key) {
        if (key == null) {
            return this.hasNullKey;
        }
        return this.keys[this.slotFor(key)] != null;
    }

    // A live view: removing a key from the set removes it from the map. It used to be a detached
    // `HashSet`, so `map.keySet().remove(k)` compiled, ran, and did nothing to the map -- see
    // {@link MapKeySet} for exactly how live it is, since the iterator is the one part that is not.
    public Set<K> keySet() {
        return new MapKeySet<K, V>(this);
    }

    // The live keys, as an array. It reads `keys` directly and NOT through `entrySet()`, which is
    // the whole point: `entrySet()` is built on `keySet()`, so a snapshot taken through it would
    // call straight back into the view that asked for it.
    Object[] keyArray() {
        Object[] out = new Object[this.size];
        int n = 0;
        if (this.hasNullKey && n < out.length) {
            out[n] = null;
            n = n + 1;
        }
        int i = 0;
        while (i < this.keys.length && n < out.length) {
            if (this.keys[i] != null) {
                out[n] = this.keys[i];
                n = n + 1;
            }
            i = i + 1;
        }
        return out;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        Iterator<? extends K> it = m.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            this.put(k, m.get(k));
        }
    }

    public V put(K key, V value) {
        if (key == null) {
            V old = this.nullValue;
            if (!this.hasNullKey) {
                this.hasNullKey = true;
                this.size = this.size + 1;
            }
            this.nullValue = value;
            return old;
        }
        if (this.size * 2 >= this.keys.length) {
            this.resize();
        }
        int i = this.slotFor(key);
        V old = (V) this.values[i];
        if (this.keys[i] == null) {
            this.size = this.size + 1;
        }
        this.keys[i] = key;
        this.values[i] = value;
        return old;
    }

    public V remove(Object key) {
        if (key == null) {
            if (!this.hasNullKey) {
                return null;
            }
            V old = this.nullValue;
            this.hasNullKey = false;
            this.nullValue = null;
            this.size = this.size - 1;
            return old;
        }
        int cap = this.keys.length;
        int i = this.slotFor(key);
        if (this.keys[i] == null) {
            return null;
        }
        V old = (V) this.values[i];
        this.keys[i] = null;
        this.values[i] = null;
        this.size = this.size - 1;
        // Re-insert the rest of this probe cluster so no lookup is cut short.
        int j = (i + 1) & (cap - 1);
        while (this.keys[j] != null) {
            K k = (K) this.keys[j];
            V v = (V) this.values[j];
            this.keys[j] = null;
            this.values[j] = null;
            this.size = this.size - 1;
            this.put(k, v);
            j = (j + 1) & (cap - 1);
        }
        return old;
    }

    public boolean containsValue(Object value) {
        if (this.hasNullKey) {
            if (value == null ? this.nullValue == null : value.equals(this.nullValue)) {
                return true;
            }
        }
        for (int i = 0; i < this.values.length; i++) {
            if (this.keys[i] != null) {
                Object v = this.values[i];
                if (value == null) {
                    if (v == null) {
                        return true;
                    }
                } else {
                    if (value.equals(v)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void clear() {
        for (int i = 0; i < this.keys.length; i++) {
            this.keys[i] = null;
            this.values[i] = null;
        }
        this.size = 0;
        this.hasNullKey = false;
        this.nullValue = null;
    }

    // Double the table and re-insert every live entry into the fresh, larger arrays.
    private void resize() {
        Object[] oldKeys = this.keys;
        Object[] oldValues = this.values;
        int newCap = oldKeys.length * 2;
        this.keys = new Object[newCap];
        this.values = new Object[newCap];
        // The null-key entry is not in the table, so its +1 has to be kept by hand.
        this.size = this.hasNullKey ? 1 : 0;
        for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != null) {
                this.put((K) oldKeys[i], (V) oldValues[i]);
            }
        }
    }

    /**
     * This map's values.
     *
     * <p>**A deliberate divergence**: the JDK's is a *view* backed by the map; this one is a copy
     * taken at the moment of asking. `keySet()` used to have the same divergence and no longer does.
     * And unlike `keySet()` this is a `Collection` and not a `Set`, because values **can** repeat.
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
