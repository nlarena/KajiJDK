package java.util.concurrent;

import java.util.Map;

// A {@link Map} whose updates are atomic and safe for concurrent use. The four methods it
// adds are the compare-and-act primitives: they read and write in one indivisible step, so
// callers never need an external lock to avoid a lost update.
public interface ConcurrentMap<K, V> extends Map<K, V> {

    // Insert only if the key is absent; returns the value already mapped, or null.
    V putIfAbsent(K key, V value);

    // Remove only if the key currently maps to `value`.
    boolean remove(Object key, Object value);

    // Replace only if the key currently maps to `oldValue`.
    boolean replace(K key, V oldValue, V newValue);

    // Replace only if the key is currently mapped; returns the previous value, or null.
    V replace(K key, V value);
}
