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
public class ColTest {

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
        r = r + m.entrySet().size();                     // 2
        r = r + m.keySet().size();                       // 2

        // ---- ListIterator --------------------------------------------------------------------
        ArrayList<String> li = new ArrayList<String>();
        li.add("1");
        li.add("2");
        li.add("3");
        ListIterator<String> it = li.listIterator();
        int pasos = 0;
        while (it.hasNext()) {
            it.next();
            pasos = pasos + 1;
        }
        r = r + pasos;                                   // 3
        int atras = 0;
        while (it.hasPrevious()) {
            it.previous();
            atras = atras + 1;
        }
        r = r + atras;                                   // 3

        ListIterator<String> it2 = li.listIterator();
        it2.next();
        it2.set("9");                                    // reemplaza el primero
        r = r + (li.get(0).equals("9") ? 1 : 0);
        it2.next();
        it2.remove();                                    // saca el segundo
        r = r + li.size();                               // 2

        // ---- subList es una VISTA ------------------------------------------------------------
        ArrayList<String> base = new ArrayList<String>();
        base.add("A");
        base.add("B");
        base.add("C");
        base.add("D");
        List<String> vista = base.subList(1, 3);         // [B, C]
        r = r + vista.size();                            // 2
        vista.set(0, "Z");
        r = r + (base.get(1).equals("Z") ? 1 : 0);       // escribir en la vista escribe atras
        r = r + base.lastIndexOf("D");                   // 3

        // ---- List.sort -----------------------------------------------------------------------
        ArrayList<String> orden = new ArrayList<String>();
        orden.add("c");
        orden.add("a");
        orden.add("b");
        orden.sort(null);
        r = r + (orden.get(0).equals("a") && orden.get(2).equals("c") ? 1 : 0);

        return r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
