import java.util.*;
import java.util.function.*;
public class Md_d {
    public static int run() {
        int r = 0;
        HashMap<String,Integer> m = new HashMap<String,Integer>();
        m.put("k", 5);
        Function<String,Integer> g = key -> Integer.valueOf(7); m.computeIfAbsent("c", g); r = r + m.get("c").intValue();
        return r;
    }
}
