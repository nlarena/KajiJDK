import java.util.Map;
import java.util.Set;
public class SifProbe5<K, V> implements Map<K, V> {
    public int size() { return 0; }
    public boolean isEmpty() { return true; }
    public boolean containsKey(Object k) { return false; }
    public boolean containsValue(Object v) { return false; }
    public V get(Object k) { return null; }
    public V put(K k, V v) { return null; }
    public V remove(Object k) { return null; }
    public void clear() { }
    public Set<K> keySet() { return null; }
    public void putAll(Map<? extends K, ? extends V> m) { }
    public java.util.Collection<V> values() { return null; }
    public Set<Map.Entry<K, V>> entrySet() { return null; }

    public Map.Entry<K, V> par(K k, V v) {
        return Map.entry(k, v);
    }
    public static int run() { return 1; }
}
