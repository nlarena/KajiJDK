package java.util;

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

    // A single key→value association — the unit an entry-oriented view hands back. Nested
    // in Map exactly as in the JDK (java.util.Map.Entry).
    interface Entry<K, V> {

        K getKey();

        V getValue();

        V setValue(V value);
    }
}
