package java.util.concurrent;

import java.util.NavigableMap;
import java.util.NavigableSet;

// A navigable (sorted, range-queryable) map that is also safe for concurrent use. It adds
// no operation of its own: what it does is *narrow the return types* of every view-producing
// method of NavigableMap, so that a submap of a concurrent map is itself a concurrent map
// rather than a plain NavigableMap. Without those covariant overrides a caller who took a
// `headMap` would silently lose the atomicity guarantee the interface exists to promise.
//
// The key-set views are narrowed for the same reason, to NavigableSet.
public interface ConcurrentNavigableMap<K, V> extends ConcurrentMap<K, V>, NavigableMap<K, V> {

    // View of the portion strictly between (or inclusive of) the two bounds.
    ConcurrentNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
                                        K toKey, boolean toInclusive);

    // View of the portion below `toKey`.
    ConcurrentNavigableMap<K, V> headMap(K toKey, boolean inclusive);

    // View of the portion at or above `fromKey`.
    ConcurrentNavigableMap<K, V> tailMap(K fromKey, boolean inclusive);

    // The SortedMap-shaped forms: from inclusive, to exclusive.
    ConcurrentNavigableMap<K, V> subMap(K fromKey, K toKey);

    ConcurrentNavigableMap<K, V> headMap(K toKey);

    ConcurrentNavigableMap<K, V> tailMap(K fromKey);

    // A view of the same mappings in the opposite order.
    ConcurrentNavigableMap<K, V> descendingMap();

    // Key views. `keySet` is narrowed to NavigableSet as well, so it agrees with
    // `navigableKeySet` — they return the same thing.
    NavigableSet<K> navigableKeySet();

    NavigableSet<K> keySet();

    NavigableSet<K> descendingKeySet();
}
