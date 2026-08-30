import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

// Comportamiento de TreeMap y TreeSet como NavigableMap/NavigableSet: orden, navegacion, y sobre
// todo los cortes, que son VISTAS y no copias. El resultado va codificado en el int que devuelve
// run() para poder compararlo contra el `java` real.
public class TreeTest {

    private static TreeMap<String, Integer> base() {
        TreeMap<String, Integer> m = new TreeMap<String, Integer>();
        m.put("c", Integer.valueOf(3));
        m.put("a", Integer.valueOf(1));
        m.put("e", Integer.valueOf(5));
        m.put("b", Integer.valueOf(2));
        m.put("d", Integer.valueOf(4));
        return m;
    }

    private static String texto(Iterable<String> it) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> i = it.iterator();
        while (i.hasNext()) {
            sb.append(i.next());
        }
        return sb.toString();
    }

    public static int run() {
        int r = 0;
        TreeMap<String, Integer> m = base();

        // ---- orden de las tres vistas -----------------------------------------------------------
        // keySet era un HashSet: recorrer las claves de un mapa ORDENADO salia en orden de hash, y
        // values/entrySet heredaban ese desorden por construirse sobre el.
        r = r + (texto(m.keySet()).equals("abcde") ? 1 : 0);
        r = r + (m.values().toString().equals("[1, 2, 3, 4, 5]") ? 2 : 0);
        r = r + (m.entrySet().toString().equals("[a=1, b=2, c=3, d=4, e=5]") ? 4 : 0);
        r = r + (texto(m.navigableKeySet()).equals("abcde") ? 8 : 0);
        r = r + (texto(m.descendingKeySet()).equals("edcba") ? 16 : 0);
        r = r + (m.firstKey().equals("a") && m.lastKey().equals("e") ? 32 : 0);

        // ---- entradas de los extremos -----------------------------------------------------------
        r = r + (m.firstEntry().getKey().equals("a") ? 64 : 0);
        r = r + (m.lastEntry().getValue().intValue() == 5 ? 128 : 0);
        // la entrada que devuelve la navegacion es una foto, no una posicion
        boolean fija = false;
        try {
            m.firstEntry().setValue(Integer.valueOf(99));
        } catch (UnsupportedOperationException e) {
            fija = true;
        }
        r = r + (fija ? 256 : 0);

        // ---- los cuatro vecinos -----------------------------------------------------------------
        r = r + (m.lowerKey("c").equals("b") ? 512 : 0);
        r = r + (m.floorKey("c").equals("c") ? 1024 : 0);       // floor incluye la propia
        r = r + (m.ceilingKey("c").equals("c") ? 2048 : 0);
        r = r + (m.higherKey("c").equals("d") ? 4096 : 0);
        r = r + (m.lowerKey("a") == null ? 8192 : 0);           // no hay nada antes de la primera
        r = r + (m.higherKey("e") == null ? 16384 : 0);
        // con una clave que NO esta en el mapa
        r = r + (m.floorKey("cc").equals("c") ? 32768 : 0);
        r = r + (m.ceilingKey("cc").equals("d") ? 65536 : 0);
        r = r + (m.lowerEntry("c").getValue().intValue() == 2 ? 131072 : 0);
        r = r + (m.higherEntry("c").getValue().intValue() == 4 ? 262144 : 0);

        // ---- putFirst/putLast: un mapa ordenado no puede cumplirlos ------------------------------
        boolean niega = false;
        try {
            m.putFirst("z", Integer.valueOf(0));
        } catch (UnsupportedOperationException e) {
            niega = true;
        }
        r = r + (niega ? 524288 : 0);

        // ---- los cortes, que son VISTAS ---------------------------------------------------------
        SortedMap<String, Integer> sub = m.subMap("b", "e");     // [b, e)
        r = r + (texto(sub.keySet()).equals("bcd") ? 1 : 0);
        r = r + (sub.size() == 3 ? 2 : 0);
        r = r + (sub.firstKey().equals("b") && sub.lastKey().equals("d") ? 4 : 0);
        r = r + (sub.containsKey("a") ? 0 : 8);                  // fuera del rango: no se ve
        r = r + (sub.get("a") == null ? 16 : 0);

        // escribir en el mapa se ve por la vista
        m.put("bb", Integer.valueOf(22));
        r = r + (texto(sub.keySet()).equals("bbbcd") ? 32 : 0);
        m.remove("bb");

        // y escribir por la vista se ve en el mapa
        sub.put("cc", Integer.valueOf(33));
        r = r + (m.containsKey("cc") ? 64 : 0);
        m.remove("cc");

        // fuera del rango no se puede escribir: la vista prometio ser exactamente ese rango
        boolean fuera = false;
        try {
            sub.put("z", Integer.valueOf(0));
        } catch (IllegalArgumentException e) {
            fuera = true;
        }
        r = r + (fuera ? 128 : 0);

        r = r + (texto(m.headMap("c").keySet()).equals("ab") ? 256 : 0);
        r = r + (texto(m.tailMap("c").keySet()).equals("cde") ? 512 : 0);
        r = r + (texto(m.headMap("c", true).keySet()).equals("abc") ? 1024 : 0);
        r = r + (texto(m.tailMap("c", false).keySet()).equals("de") ? 2048 : 0);
        r = r + (texto(m.subMap("b", false, "e", true).keySet()).equals("cde") ? 4096 : 0);

        // un corte de un corte
        NavigableMap<String, Integer> nsub = m.subMap("a", true, "e", true);
        r = r + (texto(nsub.subMap("b", true, "d", false).keySet()).equals("bc") ? 8192 : 0);

        // clear() sobre la vista borra ese rango del mapa, y nada mas
        TreeMap<String, Integer> victima = base();
        victima.subMap("b", "d").clear();
        r = r + (texto(victima.keySet()).equals("ade") ? 16384 : 0);

        // ---- el mapa descendente ----------------------------------------------------------------
        NavigableMap<String, Integer> desc = m.descendingMap();
        r = r + (texto(desc.keySet()).equals("edcba") ? 1 : 0);
        r = r + (desc.firstKey().equals("e") ? 2 : 0);
        r = r + (desc.lastKey().equals("a") ? 4 : 0);
        // en el descendente los vecinos se dan vuelta: "el siguiente" es el menor
        r = r + (desc.higherKey("c").equals("b") ? 8 : 0);
        r = r + (desc.lowerKey("c").equals("d") ? 16 : 0);
        r = r + (desc.ceilingKey("cc").equals("c") ? 32 : 0);
        // y un corte del descendente se pide en el orden del descendente
        r = r + (texto(desc.subMap("d", true, "b", false).keySet()).equals("dc") ? 64 : 0);
        r = r + (texto(desc.headMap("c").keySet()).equals("ed") ? 128 : 0);
        r = r + (texto(desc.tailMap("c").keySet()).equals("cba") ? 256 : 0);
        // darlo vuelta otra vez devuelve el orden original
        r = r + (texto(desc.descendingMap().keySet()).equals("abcde") ? 512 : 0);
        r = r + (desc.comparator() != null ? 1024 : 0);          // el descendente SI tiene uno
        r = r + (m.comparator() == null ? 2048 : 0);             // el mapa natural, no

        // ---- poll -------------------------------------------------------------------------------
        TreeMap<String, Integer> pila = base();
        r = r + (pila.pollFirstEntry().getKey().equals("a") ? 4096 : 0);
        r = r + (pila.pollLastEntry().getKey().equals("e") ? 8192 : 0);
        r = r + (texto(pila.keySet()).equals("bcd") ? 16384 : 0);
        pila.clear();
        r = r + (pila.pollFirstEntry() == null ? 32768 : 0);

        // ---- constructores de copia -------------------------------------------------------------
        TreeMap<String, Integer> copia = new TreeMap<String, Integer>(m);
        r = r + (texto(copia.keySet()).equals(texto(m.keySet())) ? 1 : 0);

        // el que copia un SortedMap se queda con SU comparador: sin eso, copiar un mapa ordenado
        // al reves daria uno ordenado al derecho
        Comparator<String> alReves = Collections.reverseOrder();
        TreeMap<String, Integer> invertido = new TreeMap<String, Integer>(alReves);
        invertido.putAll(m);
        SortedMap<String, Integer> comoOrdenado = invertido;
        TreeMap<String, Integer> copiaInv = new TreeMap<String, Integer>(comoOrdenado);
        r = r + (texto(copiaInv.keySet()).equals("edcba") ? 2 : 0);
        r = r + (copiaInv.comparator() != null ? 4 : 0);

        // ---- TreeSet ----------------------------------------------------------------------------
        TreeSet<String> s = new TreeSet<String>();
        s.add("c");
        s.add("a");
        s.add("e");
        s.add("b");
        s.add("d");
        r = r + (texto(s).equals("abcde") ? 8 : 0);
        r = r + (s.first().equals("a") && s.last().equals("e") ? 16 : 0);
        r = r + (s.lower("c").equals("b") ? 32 : 0);
        r = r + (s.floor("cc").equals("c") ? 64 : 0);
        r = r + (s.ceiling("cc").equals("d") ? 128 : 0);
        r = r + (s.higher("e") == null ? 256 : 0);
        r = r + (texto(s.headSet("c")).equals("ab") ? 512 : 0);
        r = r + (texto(s.tailSet("c")).equals("cde") ? 1024 : 0);
        r = r + (texto(s.subSet("b", "e")).equals("bcd") ? 2048 : 0);
        r = r + (texto(s.subSet("b", false, "e", true)).equals("cde") ? 4096 : 0);
        r = r + (texto(s.descendingSet()).equals("edcba") ? 8192 : 0);
        r = r + (texto(s.descendingSet().headSet("c")).equals("ed") ? 16384 : 0);
        r = r + (texto(s.descendingSet().descendingSet()).equals("abcde") ? 32768 : 0);

        StringBuilder atras = new StringBuilder();
        Iterator<String> di = s.descendingIterator();
        while (di.hasNext()) {
            atras.append(di.next());
        }
        r = r + (atras.toString().equals("edcba") ? 65536 : 0);

        // el corte es vista tambien del lado del conjunto
        SortedSet<String> corte = s.subSet("b", "e");
        s.add("cc");
        r = r + (texto(corte).equals("bcccd") ? 131072 : 0);
        corte.remove("cc");
        r = r + (s.contains("cc") ? 0 : 262144);

        // poll saca de los extremos
        TreeSet<String> pilaS = new TreeSet<String>(s);
        r = r + (pilaS.pollFirst().equals("a") ? 524288 : 0);
        r = r + (pilaS.pollLast().equals("e") ? 1048576 : 0);
        r = r + (texto(pilaS).equals("bcd") ? 2097152 : 0);

        // ---- constructores del conjunto ---------------------------------------------------------
        List<String> desordenada = new ArrayList<String>();
        desordenada.add("z");
        desordenada.add("m");
        desordenada.add("a");
        desordenada.add("m");
        TreeSet<String> deLista = new TreeSet<String>(desordenada);
        r = r + (texto(deLista).equals("amz") ? 1 : 0);          // ordena y descarta el repetido

        TreeSet<String> invS = new TreeSet<String>(alReves);
        invS.addAll(desordenada);
        SortedSet<String> comoOrdenadoS = invS;
        TreeSet<String> copiaInvS = new TreeSet<String>(comoOrdenadoS);
        r = r + (texto(copiaInvS).equals("zma") ? 2 : 0);

        // ---- la vista de claves no deja agregar --------------------------------------------------
        // No sabria que valor poner. Es la unica diferencia entre un conjunto y la vista de claves
        // de un mapa.
        NavigableSet<String> claves = m.navigableKeySet();
        boolean sinAdd = false;
        try {
            claves.add("z");
        } catch (UnsupportedOperationException e) {
            sinAdd = true;
        }
        r = r + (sinAdd ? 4 : 0);
        // pero si deja quitar, y eso quita del mapa
        TreeMap<String, Integer> conClaves = base();
        conClaves.navigableKeySet().remove("c");
        r = r + (texto(conClaves.keySet()).equals("abde") ? 8 : 0);

        // ---- addFirst/addLast se niegan ----------------------------------------------------------
        boolean sinExtremo = false;
        try {
            s.addFirst("z");
        } catch (UnsupportedOperationException e) {
            sinExtremo = true;
        }
        r = r + (sinExtremo ? 16 : 0);
        r = r + (s.getFirst().equals("a") ? 32 : 0);

        // ---- igualdad entre implementaciones ------------------------------------------------------
        Set<String> comoConjunto = s;
        r = r + (comoConjunto.equals(new java.util.HashSet<String>(desordenada)) ? 0 : 64);
        Map<String, Integer> comoMapa = m;
        r = r + (comoMapa.equals(new java.util.HashMap<String, Integer>(m)) ? 128 : 0);

        return r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
