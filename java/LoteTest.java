import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Collections;
import java.util.Comparator;

// Los constructores de capacidad y copia, los sueltos, y `reversed()` en los navegables.
public class LoteTest {

    enum Color { ROJO, VERDE, AZUL }

    public static int run() {
        int r = 0;

        // ---- constructores de copia ---------------------------------------------------------------
        List<String> base = new ArrayList<String>();
        base.add("a");
        base.add("b");
        base.add("c");

        ArrayList<String> al = new ArrayList<String>(base);
        r = r + (al.size() == 3 && al.get(0).equals("a") ? 1 : 0);
        r = r + (new ArrayList<String>(100).size() == 0 ? 2 : 0);
        al.trimToSize();
        r = r + (al.size() == 3 && al.get(2).equals("c") ? 4 : 0);

        LinkedList<String> ll = new LinkedList<String>(base);
        r = r + (ll.size() == 3 && ll.getFirst().equals("a") && ll.getLast().equals("c") ? 8 : 0);

        ArrayDeque<String> ad = new ArrayDeque<String>(base);
        r = r + (ad.size() == 3 && ad.peekFirst().equals("a") && ad.peekLast().equals("c") ? 16 : 0);
        ArrayDeque<String> adc = ad.clone();
        r = r + (adc.size() == 3 ? 32 : 0);
        adc.addLast("d");
        r = r + (ad.size() == 3 ? 64 : 0);           // la copia es independiente

        HashSet<String> hs = new HashSet<String>(64);
        hs.add("x");
        r = r + (hs.size() == 1 ? 128 : 0);
        r = r + (new HashSet<String>(8, 0.75f).size() == 0 ? 256 : 0);
        r = r + (HashSet.newHashSet(100).size() == 0 ? 512 : 0);

        Map<String, String> m = new HashMap<String, String>();
        m.put("k", "v");
        r = r + (new Hashtable<String, String>(m).get("k").equals("v") ? 1024 : 0);
        r = r + (new IdentityHashMap<String, String>(m).size() == 1 ? 2048 : 0);
        r = r + (new WeakHashMap<String, String>(m).get("k").equals("v") ? 4096 : 0);
        r = r + (WeakHashMap.newWeakHashMap(50).size() == 0 ? 8192 : 0);

        // ---- PriorityQueue: hereda el comparador ----------------------------------------------------
        Comparator<String> alReves = Collections.reverseOrder();
        PriorityQueue<String> pq = new PriorityQueue<String>(11, alReves);
        pq.offer("a");
        pq.offer("c");
        pq.offer("b");
        r = r + (pq.peek().equals("c") ? 1 : 0);      // con el comparador invertido, la cabeza es la mayor
        PriorityQueue<String> copia = new PriorityQueue<String>(pq);
        r = r + (copia.peek().equals("c") ? 2 : 0);   // la copia conserva el orden
        r = r + (copia.size() == 3 ? 4 : 0);

        SortedSet<String> ts = new TreeSet<String>(alReves);
        ts.add("a");
        ts.add("c");
        ts.add("b");
        PriorityQueue<String> desdeSet = new PriorityQueue<String>(ts);
        r = r + (desdeSet.peek().equals("c") ? 8 : 0);

        // ---- EnumMap --------------------------------------------------------------------------------
        EnumMap<Color, String> em = new EnumMap<Color, String>(Color.class);
        em.put(Color.VERDE, "v");
        em.put(Color.ROJO, "r");
        EnumMap<Color, String> emc = em.clone();
        r = r + (emc.size() == 2 && emc.get(Color.ROJO).equals("r") ? 16 : 0);
        emc.put(Color.AZUL, "a");
        r = r + (em.size() == 2 ? 32 : 0);            // independiente

        Map<Color, String> corriente = new HashMap<Color, String>();
        corriente.put(Color.AZUL, "a");
        EnumMap<Color, String> deMapa = new EnumMap<Color, String>(corriente);
        r = r + (deMapa.get(Color.AZUL).equals("a") ? 64 : 0);
        // un mapa vacio que no sea EnumMap no alcanza para deducir el tipo del enum
        boolean vacioNoVa = false;
        try {
            Map<Color, String> nada = new HashMap<Color, String>();
            EnumMap<Color, String> x = new EnumMap<Color, String>(nada);
        } catch (IllegalArgumentException e) {
            vacioNoVa = true;
        }
        r = r + (vacioNoVa ? 128 : 0);

        // ---- Queue: element/remove lanzan donde peek/poll dan null ------------------------------------
        Queue<String> q = new ArrayDeque<String>();
        r = r + (q.peek() == null ? 256 : 0);
        r = r + (q.poll() == null ? 512 : 0);
        boolean elementLanza = false;
        try {
            q.element();
        } catch (NoSuchElementException e) {
            elementLanza = true;
        }
        r = r + (elementLanza ? 1024 : 0);
        boolean removeLanza = false;
        try {
            q.remove();
        } catch (NoSuchElementException e) {
            removeLanza = true;
        }
        r = r + (removeLanza ? 2048 : 0);
        q.offer("z");
        r = r + (q.element().equals("z") ? 4096 : 0);
        r = r + (q.remove().equals("z") ? 8192 : 0);
        r = r + (q.isEmpty() ? 16384 : 0);

        // ---- Enumeration.asIterator --------------------------------------------------------------------
        Enumeration<String> en = Collections.enumeration(base);
        Iterator<String> it = en.asIterator();
        int cuantos = 0;
        StringBuilder juntos = new StringBuilder();
        while (it.hasNext()) {
            juntos.append(it.next());
            cuantos = cuantos + 1;
        }
        r = r + (cuantos == 3 ? 1 : 0);
        r = r + (juntos.toString().equals("abc") ? 2 : 0);

        // ---- NoSuchElementException con causa ------------------------------------------------------------
        Throwable causa = new IllegalStateException("porque si");
        NoSuchElementException n1 = new NoSuchElementException("mensaje", causa);
        r = r + (n1.getMessage().equals("mensaje") ? 4 : 0);
        r = r + (n1.getCause() == causa ? 8 : 0);
        NoSuchElementException n2 = new NoSuchElementException(causa);
        r = r + (n2.getCause() == causa ? 16 : 0);

        // ---- reversed() en los navegables ------------------------------------------------------------------
        TreeSet<String> nav = new TreeSet<String>();
        nav.add("a");
        nav.add("b");
        nav.add("c");
        StringBuilder alreves = new StringBuilder();
        Iterator<String> ri = nav.reversed().iterator();
        while (ri.hasNext()) {
            alreves.append(ri.next());
        }
        r = r + (alreves.toString().equals("cba") ? 32 : 0);
        r = r + (nav.reversed().first().equals("c") ? 64 : 0);

        java.util.TreeMap<String, String> navm = new java.util.TreeMap<String, String>();
        navm.put("a", "1");
        navm.put("b", "2");
        r = r + (navm.reversed().firstKey().equals("b") ? 128 : 0);
        // es una VISTA del mapa: escribir en el original se ve
        navm.put("c", "3");
        r = r + (navm.reversed().firstKey().equals("c") ? 256 : 0);

        // ---- UUID.nameUUIDFromBytes ------------------------------------------------------------------------
        // El MD5 esta escrito a mano, con su tabla de 64 constantes literal. Un solo digito
        // cambiado da otros 128 bits, asi que esta comparacion es la que la valida.
        byte[] nombre = { 104, 111, 108, 97 };            // "hola"
        UUID u = UUID.nameUUIDFromBytes(nombre);
        r = r + (u.version() == 3 ? 512 : 0);
        r = r + (u.variant() == 2 ? 1024 : 0);
        r = r + (u.toString().length() == 36 ? 2048 : 0);
        // determinista
        r = r + (u.equals(UUID.nameUUIDFromBytes(nombre)) ? 4096 : 0);
        // y el valor exacto, que es lo que prueba la tabla
        r = r + (u.getMostSignificantBits() == 0x4d186321c1a730f3L ? 8192 : 0);
        r = r + (u.getLeastSignificantBits() == 0x94b297e8914ab240L ? 16384 : 0);
        byte[] vacio = new byte[0];
        UUID uv = UUID.nameUUIDFromBytes(vacio);
        // El MD5 del mensaje vacio es d41d8cd98f00b204e9800998ecf8427e, la constante mas conocida
        // del algoritmo. Se le ven los bits de version y variante estampados encima: el `b2` del
        // medio pasa a `32`, y el `e9` a `a9`.
        r = r + (uv.getMostSignificantBits() == 0xd41d8cd98f003204L ? 32768 : 0);
        r = r + (uv.getLeastSignificantBits() == 0xa9800998ecf8427eL ? 65536 : 0);
        r = r + (uv.toString().equals("d41d8cd9-8f00-3204-a980-0998ecf8427e") ? 131072 : 0);

        return r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
