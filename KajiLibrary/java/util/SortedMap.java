package java.util;

// A map whose keys are kept in order — the map counterpart of {@link SortedSet}, with the same
// range views over the keys. {@link TreeMap} is the implementation.
public interface SortedMap<K, V> extends SequencedMap<K, V> {

    // The key ordering, or null when the keys order themselves (Comparable).
    Comparator<? super K> comparator();

    // The entries whose keys fall in [from, to) — a view, not a copy.
    SortedMap<K, V> subMap(K from, K to);

    SortedMap<K, V> headMap(K to);

    SortedMap<K, V> tailMap(K from);

    K firstKey();

    K lastKey();

    /** Una **vista** de este mapa en orden inverso. Ver `SortedSet.reversed()`. */
    default SortedMap<K, V> reversed() {
        return new ReverseSortedMap<K, V>(this);
    }
}
