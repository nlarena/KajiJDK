package java.util;


// A map with a defined encounter order over its entries (Java 21): LinkedHashMap, or a sorted
// map. It can be asked for its first and last entry, and reversed as a view.
//
// The entry operations are defaults so an implementation only has to provide reversed() plus
// the ordinary Map methods.
public interface SequencedMap<K, V> extends Map<K, V> {

    SequencedMap<K, V> reversed();

    /**
     * The keys as a sequenced set.
     *
     * <p>The three `sequenced*` are inner views, built on `keySet()`, `values()` and `entrySet()`
     * (see {@code SeqMapViews}). They used to refuse, on the grounds that this interface had no way
     * of walking the map because `Map` did not expose those three; `Map` exposes them now, so the
     * grounds are gone and so is the refusal.
     *
     * <p>The entry operations further down still refuse here, and that is a different matter: every
     * implementation in this library overrides them, so the default is never what answers.
     */
    default SequencedSet<K> sequencedKeySet() {
        return new SeqMapKeySet<K, V>(this);
    }

    /** The values, in the map's encounter order. */
    default SequencedCollection<V> sequencedValues() {
        return new SeqMapValues<K, V>(this);
    }

    /** The entries, in the map's encounter order. */
    default SequencedSet<Map.Entry<K, V>> sequencedEntrySet() {
        return new SeqMapEntrySet<K, V>(this);
    }

    default Map.Entry<K, V> firstEntry() {
        throw new UnsupportedOperationException();
    }

    default Map.Entry<K, V> lastEntry() {
        throw new UnsupportedOperationException();
    }

    // Remove and return the first entry, or null if the map is empty.
    default Map.Entry<K, V> pollFirstEntry() {
        throw new UnsupportedOperationException();
    }

    default Map.Entry<K, V> pollLastEntry() {
        throw new UnsupportedOperationException();
    }

    // Put the mapping at the front of the encounter order.
    default V putFirst(K k, V v) {
        throw new UnsupportedOperationException();
    }

    default V putLast(K k, V v) {
        throw new UnsupportedOperationException();
    }
}
