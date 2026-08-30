import java.util.Map;
public class SifProbe4<K, V> {
    K k;
    V v;
    public Map.Entry<K, V> par() {
        return Map.<K, V>entry(this.k, this.v);
    }
    public static int run() { return 1; }
}
