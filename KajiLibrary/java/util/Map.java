package java.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

// KajiLibrary's java.util.Map<K,V> — a set of key→value associations with unique keys. Not
// a Collection (its own root). Look up / insert / remove by key, test membership, size, and
// clear. A KajiLibrary subset (the JDK also has keySet/values/entrySet/putAll/getOrDefault/…).
// Concrete: HashMap.
public interface Map<K, V> {

    int size();

    boolean isEmpty();

    boolean containsKey(Object key);

    boolean containsValue(Object value);

    V get(Object key);

    V put(K key, V value);

    V remove(Object key);

    void clear();

    // The keys, as a Set (finding #205).
    //
    // It is here because `putAll` needs it: its argument arrives typed as the **interface** `Map`,
    // and with no way of enumerating it there is no way of copying it. It is inner JDK API, so adding
    // it does not take the library away from the reference — it brings it closer.
    //
    // The divergence used to be flat: this note said this library's were all **copies** while the
    // JDK's is a *view* backed by the map. It is per implementation now. HashMap, TreeMap, TmView and
    // GuardedMap return live views; AbstractMap, EnumMap, Hashtable, IdentityHashMap, LinkedHashMap
    // and WeakHashMap still return copies taken at the moment of asking, and each says so where it is
    // declared.
    Set<K> keySet();

    // It copies every pair of `m` into this map, overwriting the keys already there (§Map). Abstract
    // as in the JDK: each implementation knows how to walk its own, and several can do it more
    // cheaply than the generic loop.
    void putAll(Map<? extends K, ? extends V> m);


    // ---- the JDK 8+ `default`s -------------------------------------------------------------
    //
    // All of them are implemented over `keySet()`/`get()`/`put()`/`remove()`, which is the only thing
    // every implementation in this library has today. The JDK writes them over `entrySet()`; the
    // observable result is the same, and a `default`'s body is internal.

    // `key`'s value, or `defaultValue` if it is not there.
    //
    // The `containsKey` query is not redundant: a map that admits null values tells "mapped to null"
    // from "absent", and only the second case takes the default.
    default V getOrDefault(Object key, V defaultValue) {
        V v = this.get(key);
        if (v != null || this.containsKey(key)) {
            return v;
        }
        return defaultValue;
    }

    // It hands each pair to `action`.
    default void forEach(BiConsumer<? super K, ? super V> action) {
        Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            action.accept(k, this.get(k));
        }
    }

    // It replaces each value with the one `function` returns for its pair.
    default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            this.put(k, function.apply(k, this.get(k)));
        }
    }

    // It associates `value` with `key` only if there was no value; it returns the one that was
    // already there, or null.
    default V putIfAbsent(K key, V value) {
        V v = this.get(key);
        if (v == null) {
            v = this.put(key, value);
        }
        return v;
    }

    // It removes the pair only if the key is mapped **to that value**.
    default boolean remove(Object key, Object value) {
        V cur = this.get(key);
        if (cur == null && !this.containsKey(key)) {
            return false;
        }
        if (cur == null) {
            if (value != null) {
                return false;
            }
        } else if (!cur.equals(value)) {
            return false;
        }
        this.remove(key);
        return true;
    }

    // It replaces the value only if the current one is `oldValue`.
    default boolean replace(K key, V oldValue, V newValue) {
        V cur = this.get(key);
        if (cur == null && !this.containsKey(key)) {
            return false;
        }
        if (cur == null) {
            if (oldValue != null) {
                return false;
            }
        } else if (!cur.equals(oldValue)) {
            return false;
        }
        this.put(key, newValue);
        return true;
    }

    // It replaces the value only if the key was already mapped.
    default V replace(K key, V value) {
        V cur = this.get(key);
        if (cur != null || this.containsKey(key)) {
            return this.put(key, value);
        }
        return cur;
    }

    // `key`'s value; if there is none, it computes it with `mappingFunction` and stores it.
    //
    // A null result is NOT stored: the contract is "it ends up mapped or nothing is left", and storing
    // null would leave an entry `getOrDefault` cannot tell from an absence.
    default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V v = this.get(key);
        if (v != null) {
            return v;
        }
        V updated = mappingFunction.apply(key);
        if (updated != null) {
            this.put(key, updated);
        }
        return updated;
    }

    // It recomputes `key`'s value **only if it was already there**. A null result **removes** the
    // entry.
    default V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> f) {
        V v = this.get(key);
        if (v == null) {
            return null;
        }
        V updated = f.apply(key, v);
        if (updated != null) {
            this.put(key, updated);
            return updated;
        }
        this.remove(key);
        return null;
    }

    // It recomputes `key`'s value, present or not. A null result removes the entry (or creates
    // nothing).
    default V compute(K key, BiFunction<? super K, ? super V, ? extends V> f) {
        V v = this.get(key);
        V updated = f.apply(key, v);
        if (updated == null) {
            if (v != null || this.containsKey(key)) {
                this.remove(key);
            }
            return null;
        }
        this.put(key, updated);
        return updated;
    }

    // If there is no value, it stores `value`; if there is, it stores whatever `f` returns over the
    // two. A null result removes the entry. It is the accumulation operation: counting, summing,
    // concatenating.
    default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> f) {
        if (value == null) {
            throw new NullPointerException();
        }
        V v = this.get(key);
        V updated;
        if (v == null) {
            updated = value;
        } else {
            updated = f.apply(v, value);
        }
        if (updated == null) {
            this.remove(key);
        } else {
            this.put(key, updated);
        }
        return updated;
    }

    // ---- the immutable factories (JDK 9+) ---------------------------------------------------
    //
    // They return an **immutable** map, which rejects null keys and values and repeated keys. That
    // rejection is the contract's, not a decision of ours: `Map.of("a", 1, "a", 2)` is an
    // IllegalArgumentException in the JDK, and swallowing it would hide a bug in the literal.

    static <K, V> Map<K, V> of() {
        return FixedMap.fromPairs(new Object[0], 0);
    }

    static <K, V> Map<K, V> of(K k1, V v1) {
        Object[] kv = new Object[2];
        kv[0] = k1; kv[1] = v1;
        return FixedMap.fromPairs(kv, 2);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        Object[] kv = new Object[4];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2;
        return FixedMap.fromPairs(kv, 4);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        Object[] kv = new Object[6];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        return FixedMap.fromPairs(kv, 6);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        Object[] kv = new Object[8];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2;
        kv[4] = k3; kv[5] = v3; kv[6] = k4; kv[7] = v4;
        return FixedMap.fromPairs(kv, 8);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        Object[] kv = new Object[10];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        kv[6] = k4; kv[7] = v4; kv[8] = k5; kv[9] = v5;
        return FixedMap.fromPairs(kv, 10);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                               K k6, V v6) {
        Object[] kv = new Object[12];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        kv[6] = k4; kv[7] = v4; kv[8] = k5; kv[9] = v5; kv[10] = k6; kv[11] = v6;
        return FixedMap.fromPairs(kv, 12);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                               K k6, V v6, K k7, V v7) {
        Object[] kv = new Object[14];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        kv[6] = k4; kv[7] = v4; kv[8] = k5; kv[9] = v5; kv[10] = k6; kv[11] = v6;
        kv[12] = k7; kv[13] = v7;
        return FixedMap.fromPairs(kv, 14);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                               K k6, V v6, K k7, V v7, K k8, V v8) {
        Object[] kv = new Object[16];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        kv[6] = k4; kv[7] = v4; kv[8] = k5; kv[9] = v5; kv[10] = k6; kv[11] = v6;
        kv[12] = k7; kv[13] = v7; kv[14] = k8; kv[15] = v8;
        return FixedMap.fromPairs(kv, 16);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                               K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        Object[] kv = new Object[18];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        kv[6] = k4; kv[7] = v4; kv[8] = k5; kv[9] = v5; kv[10] = k6; kv[11] = v6;
        kv[12] = k7; kv[13] = v7; kv[14] = k8; kv[15] = v8; kv[16] = k9; kv[17] = v9;
        return FixedMap.fromPairs(kv, 18);
    }

    static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                               K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        Object[] kv = new Object[20];
        kv[0] = k1; kv[1] = v1; kv[2] = k2; kv[3] = v2; kv[4] = k3; kv[5] = v3;
        kv[6] = k4; kv[7] = v4; kv[8] = k5; kv[9] = v5; kv[10] = k6; kv[11] = v6;
        kv[12] = k7; kv[13] = v7; kv[14] = k8; kv[15] = v8; kv[16] = k9; kv[17] = v9;
        kv[18] = k10; kv[19] = v10;
        return FixedMap.fromPairs(kv, 20);
    }

    // A loose immutable pair, for building `ofEntries`.
    static <K, V> Map.Entry<K, V> entry(K k, V v) {
        return new FixedEntry<K, V>(k, v);
    }

    // The map of the given pairs.
    static <K, V> Map<K, V> ofEntries(Entry<? extends K, ? extends V>... entries) {
        Object[] kv = new Object[entries.length * 2];
        int i = 0;
        while (i < entries.length) {
            Entry<? extends K, ? extends V> e = entries[i];
            kv[i * 2] = e.getKey();
            kv[i * 2 + 1] = e.getValue();
            i = i + 1;
        }
        return FixedMap.fromPairs(kv, kv.length);
    }

    // An immutable copy of `map`. It is taken at the moment: later changes to the original are not
    // seen.
    static <K, V> Map<K, V> copyOf(Map<? extends K, ? extends V> map) {
        Object[] kv = new Object[map.size() * 2];
        int i = 0;
        Iterator<? extends K> it = map.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            kv[i] = k;
            kv[i + 1] = map.get(k);
            i = i + 2;
        }
        return FixedMap.fromPairs(kv, kv.length);
    }

    // A single key→value association — the unit an entry-oriented view hands back. Nested
    // in Map exactly as in the JDK (java.util.Map.Entry).
    interface Entry<K, V> {

        K getKey();

        V getValue();

        V setValue(V value);
    }
    // This map's values. A Collection and not a Set: values can repeat.
    Collection<V> values();

    // This map's pairs.
    Set<Entry<K, V>> entrySet();

}
