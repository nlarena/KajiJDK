import java.util.*;
import java.util.function.*;
public class Fk3 {
    public static int run() {
        int r = 0;
        // ---- factorias inmutables ------------------------------------------------------------
        List<String> lf = List.of("p", "q", "r");
        r = r + lf.size() * 10000000;                    // 30000000
        try {
            lf.add("s");
            r = r + 7777;
        } catch (UnsupportedOperationException ex) {
            r = r + 100000000;
        }

        Set<String> sf = Set.of("p", "q");
        r = r + sf.size();                               // 2
        r = r + (sf.contains("q") ? 10 : 0);
        try {
            Set.of("p", "p");                            // repetido -> IllegalArgumentException
            r = r + 7777;
        } catch (IllegalArgumentException ex) {
            r = r + 100;
        }

        Map<String, Integer> mf = Map.of("a", 1, "b", 2);
        r = r + mf.size() * 1000;                        // 2000
        r = r + mf.get("b").intValue() * 10000;          // 20000

        HashMap<String,Integer> m = new HashMap<String,Integer>();
        m.put("k", 5);
        r = r + m.values().size();
        return r;
    }
}
