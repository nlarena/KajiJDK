// Las vistas secuenciadas e invertidas, comprobadas contra `java` real.
//
// Lo que se comprueba no es "devuelve algo del tamaño correcto" -- eso lo cumpliria una copia. Se
// comprueba que sean **vistas**: que un cambio hecho de un lado se vea del otro. Es la unica
// propiedad que distingue la implementacion correcta de la facil, y la unica que se rompe en
// silencio si alguien "optimiza" copiando.
//
// Y se comprueban los **bordes de los cortes invertidos**, que es donde estan los errores dificiles:
// al dar vuelta el orden, un limite exclusivo pasa a ser inclusivo. Una prueba que solo mirara
// tamaños dejaria pasar un elemento de mas o de menos en cada extremo.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class SeqTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    static String recorrer(Iterable<String> it) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> i = it.iterator();
        while (i.hasNext()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(i.next());
        }
        return sb.toString();
    }

    // ---- LinkedHashMap ---------------------------------------------------------------------------
    static void mapa() {
        LinkedHashMap<String, Integer> m = new LinkedHashMap<String, Integer>();
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);

        ok("a".equals(m.firstEntry().getKey()));
        ok("c".equals(m.lastEntry().getKey()));

        // putFirst sobre una clave que YA estaba la mueve, no solo cambia el valor. Es la diferencia
        // con put, y es facil de implementar mal.
        m.putFirst("c", 30);
        ok("c,a,b".equals(recorrer(m.sequencedKeySet())));
        ok(m.get("c").intValue() == 30);

        m.putLast("c", 300);
        ok("a,b,c".equals(recorrer(m.sequencedKeySet())));

        // La vista invertida.
        SequencedMap<String, Integer> r = m.reversed();
        ok("c,b,a".equals(recorrer(r.sequencedKeySet())));
        ok("c".equals(r.firstEntry().getKey()));
        ok("a".equals(r.lastEntry().getKey()));

        // **Es una vista**: lo que se pone en una se ve en la otra.
        r.put("d", 4);
        ok(m.containsKey("d"));
        ok(m.get("d").intValue() == 4);
        m.remove("d");
        ok(!r.containsKey("d"));

        // Invertir lo invertido devuelve el original, no un tercer envoltorio.
        ok(r.reversed().size() == m.size());
        ok("a,b,c".equals(recorrer(r.reversed().sequencedKeySet())));

        // Las tres vistas de coleccion, y sus inversas.
        ok(m.sequencedKeySet().size() == 3);
        ok(m.sequencedValues().size() == 3);
        ok(m.sequencedEntrySet().size() == 3);
        ok("c,b,a".equals(recorrer(m.sequencedKeySet().reversed())));

        // Sacar por la vista de claves saca del mapa.
        m.sequencedKeySet().remove("b");
        ok(!m.containsKey("b"));
        ok(m.size() == 2);
    }

    // ---- LinkedHashSet ---------------------------------------------------------------------------
    static void conjunto() {
        LinkedHashSet<String> s = new LinkedHashSet<String>();
        s.add("a");
        s.add("b");
        s.add("c");
        ok("a,b,c".equals(recorrer(s)));

        s.addFirst("c");                       // ya estaba: se mueve al frente
        ok("c,a,b".equals(recorrer(s)));
        s.addLast("c");
        ok("a,b,c".equals(recorrer(s)));
        s.addFirst("z");                       // nuevo
        ok("z,a,b,c".equals(recorrer(s)));

        SequencedSet<String> r = s.reversed();
        ok("c,b,a,z".equals(recorrer(r)));
        // Vista: sacar de la inversa saca del original.
        r.remove("z");
        ok(!s.contains("z"));
        ok(s.size() == 3);

        ok(LinkedHashSet.newLinkedHashSet(10) != null);
    }

    // ---- SortedSet invertido ---------------------------------------------------------------------
    static void ordenado() {
        TreeSet<String> t = new TreeSet<String>();
        t.add("a");
        t.add("b");
        t.add("c");
        t.add("d");

        SortedSet<String> r = t.reversed();
        ok("d,c,b,a".equals(recorrer(r)));
        ok("d".equals(r.first()));
        ok("a".equals(r.last()));

        // **El borde.** En la vista el orden es d>c>b>a, asi que `headSet("b")` son los que van
        // ANTES de "b" en ESE orden: d y c. Sin "b" -- es exclusivo, como siempre.
        ok("d,c".equals(recorrer(r.headSet("b"))));
        // Y `tailSet("b")` son "b" y los siguientes: b, a. Inclusivo, como siempre.
        ok("b,a".equals(recorrer(r.tailSet("b"))));
        // subSet(from, to): desde "c" inclusive hasta "a" exclusive -> c, b.
        ok("c,b".equals(recorrer(r.subSet("c", "a"))));

        // Vista: agregar por la inversa se ve en el original.
        r.add("e");
        ok(t.contains("e"));
        ok("e,d,c,b,a".equals(recorrer(r)));
        t.remove("e");
        ok(r.size() == 4);

        ok(r.reversed().size() == 4);
        ok("a,b,c,d".equals(recorrer(r.reversed())));
    }

    // ---- SortedMap invertido ---------------------------------------------------------------------
    static void mapaOrdenado() {
        TreeMap<String, Integer> t = new TreeMap<String, Integer>();
        t.put("a", 1);
        t.put("b", 2);
        t.put("c", 3);

        java.util.SortedMap<String, Integer> r = t.reversed();
        ok("c".equals(r.firstKey()));
        ok("a".equals(r.lastKey()));
        ok(r.size() == 3);
        r.put("d", 4);
        ok(t.containsKey("d"));
        ok("d".equals(r.firstKey()));
    }

    // ---- Deque invertido -------------------------------------------------------------------------
    static void cola() {
        ArrayDeque<String> d = new ArrayDeque<String>();
        d.addLast("a");
        d.addLast("b");
        d.addLast("c");

        Deque<String> r = d.reversed();
        ok("c,b,a".equals(recorrer(r)));
        ok("c".equals(r.getFirst()));
        ok("a".equals(r.getLast()));

        // Las puntas cruzadas: agregar al frente de la vista es agregar al final del original.
        r.addFirst("z");
        ok("z".equals(d.getLast()));
        ok("z,c,b,a".equals(recorrer(r)));
        r.removeFirst();
        ok(d.size() == 3);

        ok(r.reversed().size() == 3);
        ok("a,b,c".equals(recorrer(r.reversed())));
    }

    public static int run() {
        mapa();
        conjunto();
        ordenado();
        mapaOrdenado();
        cola();
        return primerFallo;
    }

    // Para el arnes de regresion, que corre la clase con el `java` real y compara la salida.
    public static void main(String[] args) {
        System.out.println(run());
    }
}
