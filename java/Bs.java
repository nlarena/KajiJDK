import java.util.*;
import java.util.function.*;
public class Bs {
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

        // ---- defaults de Map -----------------------------------------------------------------
        HashMap<String, Integer> m = new HashMap<String, Integer>();
        m.put("k", 5);
        r = r + m.getOrDefault("k", 0).intValue();       // 5
        r = r + m.getOrDefault("nope", 3).intValue();    // 3
        r = r + m.putIfAbsent("k", 9).intValue();        // 5, no pisa
        m.putIfAbsent("n", 4);                           // entra
        r = r + m.get("n").intValue();                   // 4

        java.util.function.BiFunction<Integer, Integer, Integer> suma =
            (v1, v2) -> Integer.valueOf(v1.intValue() + v2.intValue());
        m.merge("k", 10, suma);
        r = r + m.get("k").intValue();                   // 15

        java.util.function.Function<String, Integer> siete = key -> Integer.valueOf(7);
        m.computeIfAbsent("c", siete);
        r = r + m.get("c").intValue();                   // 7
        java.util.function.BiFunction<String, Integer, Integer> doble =
            (key, v) -> Integer.valueOf(v.intValue() * 2);
        m.computeIfPresent("c", doble);
        r = r + m.get("c").intValue();                   // 14
        java.util.function.BiFunction<String, Integer, Integer> masUno =
            (key, v) -> Integer.valueOf(v.intValue() + 1);
        m.compute("c", masUno);
        r = r + m.get("c").intValue();                   // 15

        r = r + (m.replace("c", Integer.valueOf(15), Integer.valueOf(20)) ? 1 : 0);
        r = r + m.get("c").intValue();                   // 20
        r = r + (m.remove("c", Integer.valueOf(99)) ? 7777 : 0);   // valor distinto: no borra
        r = r + (m.remove("c", Integer.valueOf(20)) ? 1 : 0);      // si borra

        r = r + m.values().size();                       // k, n -> 2
        return r;
    }
}
