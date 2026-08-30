import java.util.*;
import java.util.function.*;
public class Md_c {
    public static int run() {
        int r = 0;
        HashMap<String,Integer> m = new HashMap<String,Integer>();
        m.put("k", 5);
        BiFunction<Integer,Integer,Integer> f = (a,b) -> Integer.valueOf(a.intValue()+b.intValue()); m.merge("k", 10, f); r = r + m.get("k").intValue();
        return r;
    }
}
