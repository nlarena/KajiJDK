import java.util.Map;
public class SifProbe3<K, V> {
    K k;
    V v;
    public Map.Entry<K, V> par() {
        return Map.entry(this.k, this.v);
    }
    public static int run() { return 1; }
}
