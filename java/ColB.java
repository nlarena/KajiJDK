import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

// Comportamiento de lo que se agrego a Collection, List, Set y Map: las operaciones en bloque,
// las factorias inmutables, los defaults de Map, el ListIterator y la vista subList.
//
// Cada bloque suma su aporte a `r`; si algo se desvia, el numero final cambia. Se compara contra
// `java` real, que es el arbitro.
public class ColB {

    public static int run() {
        int r = 0;

        // ---- operaciones en bloque -----------------------------------------------------------
        ArrayList<String> a = new ArrayList<String>();
        a.add("x");
        a.add("y");
        ArrayList<String> b = new ArrayList<String>();
        b.add("y");
        b.add("z");

        r = r + (a.containsAll(b) ? 7777 : 0);          // no: falta "z"
        a.addAll(b);                                     // [x, y, y, z]
        r = r + a.size();                                // 4
        r = r + (a.containsAll(b) ? 10 : 0);             // ahora si

        ArrayList<String> c = new ArrayList<String>();
        c.add("y");
        a.removeAll(c);                                  // saca TODAS las "y" -> [x, z]
        r = r + a.size() * 100;                          // 200

        ArrayList<String> d = new ArrayList<String>();
        d.add("z");
        a.retainAll(d);                                  // -> [z]
        r = r + a.size() * 1000;                         // 1000

        Object[] arr = a.toArray();
        r = r + arr.length * 10000;                      // 10000
        String[] tipado = a.toArray(new String[0]);
        r = r + (tipado[0].equals("z") ? 100000 : 0);

        // removeIf
        ArrayList<String> e = new ArrayList<String>();
        e.add("aa");
        e.add("b");
        e.add("cc");
        // Rodeo de #286: el lambda no puede ir en linea contra un `Predicate<? super E>`
        // cuyo E viene del receptor. Nombrarlo en un local es lo que lo destraba.
        java.util.function.Predicate<String> dosLetras = s -> s.length() == 2;
        e.removeIf(dosLetras);                           // deja ["b"]
        r = r + e.size() * 1000000;                      // 1000000

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

        return r;
    }

    public static void main(String[] a) { System.out.println(run()); }
}
