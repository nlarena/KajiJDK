package java.util.concurrent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// A hash map safe for concurrent use. The JDK stripes its table into independently locked
// bins so unrelated keys never contend; KajiJDK guards one plain {@link HashMap} with the
// intrinsic monitor of a private `sync` object. The *observable* contract is the same —
// every operation is atomic, and the compare-and-act methods below are indivisible — and
// on a runtime whose threads interleave between opcodes the coarse lock costs nothing real.
//
// Single-exit style throughout (finding #105).
public class ConcurrentHashMap<K, V> implements ConcurrentMap<K, V>, Serializable {

    private final Object sync = new Object();
    private final HashMap<K, V> map = new HashMap<K, V>();

    public ConcurrentHashMap() {
    }

    // The JDK sizes its table from this hint; our HashMap grows on demand, so it is only
    // an API courtesy.
    public ConcurrentHashMap(int initialCapacity) {
    }

    public int size() {
        int n;
        synchronized (sync) {
            n = map.size();
        }
        return n;
    }

    public boolean isEmpty() {
        boolean empty;
        synchronized (sync) {
            empty = map.isEmpty();
        }
        return empty;
    }

    public boolean containsKey(Object key) {
        boolean has;
        synchronized (sync) {
            has = map.containsKey(key);
        }
        return has;
    }

    public boolean containsValue(Object value) {
        boolean has;
        synchronized (sync) {
            has = map.containsValue(value);
        }
        return has;
    }

    public V get(Object key) {
        V v;
        synchronized (sync) {
            v = map.get(key);
        }
        return v;
    }

    public Set<K> keySet() {
        Set<K> ks;
        synchronized (sync) {
            ks = map.keySet();
        }
        return ks;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        synchronized (sync) {
            map.putAll(m);
        }
    }

    public V put(K key, V value) {
        V prev;
        synchronized (sync) {
            prev = map.put(key, value);
        }
        return prev;
    }

    public V remove(Object key) {
        V prev;
        synchronized (sync) {
            prev = map.remove(key);
        }
        return prev;
    }

    public void clear() {
        synchronized (sync) {
            map.clear();
        }
    }

    public V putIfAbsent(K key, V value) {
        V existing;
        synchronized (sync) {
            existing = map.get(key);
            if (existing == null) {
                map.put(key, value);
            }
        }
        return existing;
    }

    public boolean remove(Object key, Object value) {
        boolean removed;
        synchronized (sync) {
            // The receiver is bound to an `Object` local before the call: invoking a
            // method on a receiver whose static type is a *type variable* is silently
            // dropped by our javac (finding #111) — it emits the argument in place of the
            // call, so this would have branched on `value` instead of comparing.
            Object current = map.get(key);
            if (current != null && current.equals(value)) {
                map.remove(key);
                removed = true;
            } else {
                removed = false;
            }
        }
        return removed;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        boolean replaced;
        synchronized (sync) {
            Object current = map.get(key);   // Object-typed receiver — see #111 above.
            if (current != null && current.equals(oldValue)) {
                map.put(key, newValue);
                replaced = true;
            } else {
                replaced = false;
            }
        }
        return replaced;
    }

    public V replace(K key, V value) {
        V prev;
        synchronized (sync) {
            prev = map.get(key);
            if (prev != null) {
                map.put(key, value);
            }
        }
        return prev;
    }
}
