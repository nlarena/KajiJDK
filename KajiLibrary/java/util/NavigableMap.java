package java.util;


// A sorted map with the same approximate queries as {@link NavigableSet}, in both flavours:
// each one comes as a `…Map.Entry` returning the whole mapping and a `…Key` returning just the key.
public interface NavigableMap<K, V> extends SortedMap<K, V> {

    // The entry with the greatest key strictly less than `key`, or null.
    Map.Entry<K, V> lowerEntry(K key);

    K lowerKey(K key);

    // The entry with the greatest key less than **or equal to** `key`, or null.
    Map.Entry<K, V> floorEntry(K key);

    K floorKey(K key);

    Map.Entry<K, V> ceilingEntry(K key);

    K ceilingKey(K key);

    Map.Entry<K, V> higherEntry(K key);

    K higherKey(K key);

    // A reverse-ordered view of this map.
    NavigableMap<K, V> descendingMap();

    NavigableSet<K> navigableKeySet();

    NavigableSet<K> descendingKeySet();

    // Range views, with each endpoint independently inclusive or exclusive.
    NavigableMap<K, V> subMap(K from, boolean fromInclusive, K to, boolean toInclusive);

    NavigableMap<K, V> headMap(K to, boolean inclusive);

    // Igual que en NavigableSet: `reversed()` y `descendingMap()` son lo mismo.
    default NavigableMap<K, V> reversed() {
        return this.descendingMap();
    }

    NavigableMap<K, V> tailMap(K from, boolean inclusive);
}
