import java.util.Map;
public class SifProbe {
    public static int run() {
        Map.Entry<String,String> e = Map.entry("a", "b");
        return e.getKey().length();
    }
}
