import java.util.HashMap;
public class GcP4 {
    public static int run() {
        int n = 0;
        int i = 0;
        while (i < 400) {
            HashMap<String,Integer> m = new HashMap<String,Integer>();
            m.put("k" + i, Integer.valueOf(i));
            n = n + m.entrySet().size() + m.values().size();
            i = i + 1;
        }
        return n;
    }
}
