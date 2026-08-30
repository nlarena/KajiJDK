import java.util.*;
import java.util.function.*;
public class Md_a {
    public static int run() {
        int r = 0;
        HashMap<String,Integer> m = new HashMap<String,Integer>();
        m.put("k", 5);
        r = r + m.getOrDefault("k", 0).intValue();
        return r;
    }
}
