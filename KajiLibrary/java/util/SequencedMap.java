package java.util;

import java.util.Map.Entry;

// A map with a defined encounter order over its entries (Java 21): LinkedHashMap, or a sorted
// map. It can be asked for its first and last entry, and reversed as a view.
//
// The entry operations are defaults so an implementation only has to provide reversed() plus
// the ordinary Map methods.
public interface SequencedMap<K, V> extends Map<K, V> {

    SequencedMap<K, V> reversed();

    default Entry<K, V> firstEntry() {
        throw new UnsupportedOperationException();
    }

    default Entry<K, V> lastEntry() {
        throw new UnsupportedOperationException();
    }

    // Remove and return the first entry, or null if the map is empty.
    default Entry<K, V> pollFirstEntry() {
        throw new UnsupportedOperationException();
    }

    default Entry<K, V> pollLastEntry() {
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
