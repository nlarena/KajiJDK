import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

// Comportamiento de java.util.Collections: las vacias, los envoltorios, los algoritmos y los
// puentes. El resultado se codifica en el int que devuelve run(), para poder compararlo contra
// el `java` real sin depender de que la salida por consola coincida.
public class CollTest {

    public static int run() {
        int r = 0;

        // ---- vacias y de un solo elemento -------------------------------------------------------
        List<String> vacia = Collections.emptyList();
        r = r + (vacia.size() == 0 ? 1 : 0);
        r = r + (Collections.emptySet().isEmpty() ? 2 : 0);
        r = r + (Collections.emptyMap().isEmpty() ? 4 : 0);
        r = r + (Collections.emptyIterator().hasNext() ? 0 : 8);
        r = r + (Collections.emptyEnumeration().hasMoreElements() ? 0 : 16);
        r = r + (Collections.emptyListIterator().hasPrevious() ? 0 : 32);

        Set<String> uno = Collections.singleton("a");
        r = r + (uno.size() == 1 && uno.contains("a") ? 64 : 0);
        List<String> unoL = Collections.singletonList("a");
        r = r + (unoL.get(0).equals("a") ? 128 : 0);
        Map<String, String> unoM = Collections.singletonMap("k", "v");
        r = r + (unoM.get("k").equals("v") && unoM.size() == 1 ? 256 : 0);

        List<String> tres = Collections.nCopies(3, "x");
        r = r + (tres.size() == 3 && tres.get(2).equals("x") ? 512 : 0);
        r = r + (Collections.nCopies(0, "x").isEmpty() ? 1024 : 0);

        // El conjunto ordenado vacio: los cortes de lo vacio siguen siendo lo vacio, y la
        // navegacion devuelve null en vez de romper.
        SortedSet<String> vs = Collections.emptySortedSet();
        r = r + (vs.isEmpty() && vs.comparator() == null ? 2048 : 0);
        r = r + (Collections.emptyNavigableSet().ceiling("z") == null ? 4096 : 0);
        r = r + (Collections.emptyNavigableMap().firstEntry() == null ? 8192 : 0);

        // ---- unmodifiable -----------------------------------------------------------------------
        List<String> base = new ArrayList<String>();
        base.add("a");
        base.add("b");
        base.add("c");
        List<String> ro = Collections.unmodifiableList(base);
        r = r + (ro.size() == 3 && ro.get(1).equals("b") ? 1 : 0);
        r = r + (ro.equals(base) ? 2 : 0);          // la vista sigue siendo igual a la de atras
        r = r + (niega(ro, 0) ? 4 : 0);             // add
        r = r + (niega(ro, 1) ? 8 : 0);             // set
        r = r + (niega(ro, 2) ? 16 : 0);            // clear
        r = r + (niega(ro, 3) ? 32 : 0);            // iterator().remove()
        r = r + (niega(ro, 4) ? 64 : 0);            // subList(..).add(..), el agujero de al lado

        // Es una VISTA, no una copia: lo que cambie por abajo se ve por arriba.
        base.add("d");
        r = r + (ro.size() == 4 ? 128 : 0);

        Map<String, String> mbase = new HashMap<String, String>();
        mbase.put("k", "v");
        Map<String, String> mro = Collections.unmodifiableMap(mbase);
        r = r + (mro.get("k").equals("v") ? 256 : 0);
        r = r + (niegaMapa(mro, 0) ? 512 : 0);      // put
        r = r + (niegaMapa(mro, 1) ? 1024 : 0);     // keySet().remove(..)
        r = r + (niegaMapa(mro, 2) ? 2048 : 0);     // entry.setValue(..), el agujero mas fino
        r = r + (niegaMapa(mro, 3) ? 4096 : 0);     // values().clear()

        // ---- synchronized -----------------------------------------------------------------------
        // Sin hilos no hay nada que observar salvo que se comporte como la coleccion de atras.
        List<String> sl = Collections.synchronizedList(new ArrayList<String>());
        sl.add("p");
        sl.add("q");
        r = r + (sl.size() == 2 && sl.get(0).equals("p") ? 1 : 0);
        Map<String, String> sm = Collections.synchronizedMap(new HashMap<String, String>());
        sm.put("a", "1");
        r = r + (sm.get("a").equals("1") ? 2 : 0);
        int suma = 0;
        Iterator<String> it = sl.iterator();
        while (it.hasNext()) {
            suma = suma + it.next().length();
        }
        r = r + (suma == 2 ? 4 : 0);

        // ---- checked ----------------------------------------------------------------------------
        // El punto entero: con genericos borrados, la lista cruda aceptaria el Integer y la
        // ClassCastException saltaria mucho despues, en el get.
        List<String> ck = Collections.checkedList(new ArrayList<String>(), String.class);
        ck.add("ok");
        r = r + (ck.size() == 1 ? 8 : 0);
        List cruda = ck;
        boolean cazado = false;
        try {
            cruda.add(Integer.valueOf(7));
        } catch (ClassCastException e) {
            cazado = true;
        }
        r = r + (cazado ? 16 : 0);
        r = r + (ck.size() == 1 ? 32 : 0);          // y no quedo agregado

        Map cm = Collections.checkedMap(new HashMap<String, String>(), String.class, String.class);
        boolean cazado2 = false;
        try {
            cm.put("k", Integer.valueOf(7));
        } catch (ClassCastException e) {
            cazado2 = true;
        }
        r = r + (cazado2 ? 64 : 0);

        // ---- algoritmos -------------------------------------------------------------------------
        List<Integer> nums = new ArrayList<Integer>();
        nums.add(Integer.valueOf(5));
        nums.add(Integer.valueOf(1));
        nums.add(Integer.valueOf(9));
        nums.add(Integer.valueOf(3));

        r = r + Collections.max(nums).intValue();            // 9
        r = r + Collections.min(nums).intValue() * 10;       // 10
        Comparator<Integer> inverso = Collections.reverseOrder();
        r = r + Collections.max(nums, inverso).intValue() * 100;     // el max al reves es el min: 100
        r = r + Collections.min(nums, inverso).intValue() * 1000;    // 9000

        Collections.sort(nums);
        r = r + (nums.get(0).intValue() == 1 && nums.get(3).intValue() == 9 ? 100000 : 0);
        r = r + Collections.binarySearch(nums, Integer.valueOf(5)) * 1000000;   // indice 2
        // el negativo codifica donde iria: -(2)-1 = -3 para el 4
        r = r + (Collections.binarySearch(nums, Integer.valueOf(4)) == -3 ? 10000000 : 0);
        r = r + (Collections.binarySearch(nums, Integer.valueOf(9), inverso) == -1 ? 100000000 : 0);

        Collections.sort(nums, inverso);
        r = r + (nums.get(0).intValue() == 9 ? 1 : 0);

        Collections.reverse(nums);
        r = r + (nums.get(0).intValue() == 1 ? 2 : 0);

        // rotate corre hacia el final y da la vuelta; con distancia negativa va para el otro lado
        List<String> rot = new ArrayList<String>();
        rot.add("a");
        rot.add("b");
        rot.add("c");
        rot.add("d");
        Collections.rotate(rot, 1);
        r = r + (rot.toString().equals("[d, a, b, c]") ? 4 : 0);
        Collections.rotate(rot, -1);
        r = r + (rot.toString().equals("[a, b, c, d]") ? 8 : 0);
        Collections.rotate(rot, 4);
        r = r + (rot.toString().equals("[a, b, c, d]") ? 16 : 0);

        Collections.swap(rot, 0, 3);
        r = r + (rot.get(0).equals("d") ? 32 : 0);

        List<String> conRep = new ArrayList<String>();
        conRep.add("a");
        conRep.add("b");
        conRep.add("a");
        r = r + (Collections.replaceAll(conRep, "a", "z") ? 64 : 0);
        r = r + (conRep.toString().equals("[z, b, z]") ? 128 : 0);
        r = r + (Collections.replaceAll(conRep, "nada", "x") ? 0 : 256);
        r = r + Collections.frequency(conRep, "z") * 512;             // 2 -> 1024

        List<String> destino = new ArrayList<String>();
        destino.add("1");
        destino.add("2");
        destino.add("3");
        List<String> origen = new ArrayList<String>();
        origen.add("x");
        origen.add("y");
        Collections.copy(destino, origen);
        r = r + (destino.toString().equals("[x, y, 3]") ? 2048 : 0);

        Collections.fill(destino, "=");
        r = r + (destino.toString().equals("[=, =, =]") ? 4096 : 0);

        // ---- subListas y disjuncion --------------------------------------------------------------
        List<String> fuente = new ArrayList<String>();
        fuente.add("a");
        fuente.add("b");
        fuente.add("c");
        fuente.add("a");
        fuente.add("b");
        List<String> objetivo = new ArrayList<String>();
        objetivo.add("a");
        objetivo.add("b");
        r = r + Collections.indexOfSubList(fuente, objetivo);              // 0
        r = r + Collections.lastIndexOfSubList(fuente, objetivo) * 10;     // 30
        List<String> ausente = new ArrayList<String>();
        ausente.add("z");
        r = r + (Collections.indexOfSubList(fuente, ausente) == -1 ? 100 : 0);

        r = r + (Collections.disjoint(fuente, ausente) ? 1000 : 0);
        r = r + (Collections.disjoint(fuente, objetivo) ? 0 : 10000);

        List<String> creciendo = new ArrayList<String>();
        r = r + (Collections.addAll(creciendo, "p", "q", "r") ? 100000 : 0);
        r = r + creciendo.size() * 1000000;                                // 3000000

        // ---- barajado reproducible ---------------------------------------------------------------
        // Con la misma semilla el resultado tiene que ser IDENTICO al del JDK: mismo generador y
        // mismo recorrido de Fisher-Yates. Es la prueba mas exigente del archivo -- cualquier
        // diferencia en el orden de las llamadas a nextInt cambia la permutacion.
        List<Integer> baraja = new ArrayList<Integer>();
        int k = 0;
        while (k < 10) {
            baraja.add(Integer.valueOf(k));
            k = k + 1;
        }
        Collections.shuffle(baraja, new Random(42L));
        r = r + (baraja.toString().equals(barajaEsperada(baraja)) ? 0 : 0);
        int firma = 0;
        k = 0;
        while (k < 10) {
            firma = firma * 3 + baraja.get(k).intValue();
            k = k + 1;
        }
        r = r + firma;

        // ---- puentes ------------------------------------------------------------------------------
        Enumeration<String> en = Collections.enumeration(creciendo);
        int largos = 0;
        while (en.hasMoreElements()) {
            largos = largos + en.nextElement().length();
        }
        r = r + largos * 7;                                                // 21

        ArrayList<String> devuelta = Collections.list(Collections.enumeration(creciendo));
        r = r + (devuelta.equals(creciendo) ? 70 : 0);

        Set<String> desdeMapa = Collections.newSetFromMap(new LinkedHashMap<String, Boolean>());
        r = r + (desdeMapa.add("a") ? 700 : 0);
        r = r + (desdeMapa.add("a") ? 0 : 7000);                           // no entra dos veces
        r = r + (desdeMapa.contains("a") ? 70000 : 0);
        r = r + (desdeMapa.remove("a") ? 700000 : 0);
        r = r + (desdeMapa.isEmpty() ? 7000000 : 0);

        Deque<String> pila = new ArrayDeque<String>();
        Queue<String> lifo = Collections.asLifoQueue(pila);
        lifo.offer("1");
        lifo.offer("2");
        lifo.offer("3");
        // saca por donde metio: lo ultimo primero
        r = r + (lifo.poll().equals("3") ? 1 : 0);
        r = r + (lifo.peek().equals("2") ? 2 : 0);
        r = r + lifo.size() * 4;                                           // 8

        return r;
    }

    // La lista barajada se compara consigo misma; lo que importa es la firma numerica de mas
    // arriba, que es la que se contrasta contra el JDK.
    private static String barajaEsperada(List<Integer> barajada) {
        return barajada.toString();
    }

    // Cada mutador prohibido de una lista de solo lectura, por indice. Devuelve true si tiro
    // UnsupportedOperationException, que es lo que se espera de todos.
    private static boolean niega(List<String> ro, int cual) {
        try {
            if (cual == 0) {
                ro.add("x");
            } else if (cual == 1) {
                ro.set(0, "x");
            } else if (cual == 2) {
                ro.clear();
            } else if (cual == 3) {
                Iterator<String> it = ro.iterator();
                it.next();
                it.remove();
            } else {
                List<String> sub = ro.subList(0, 1);
                sub.add("x");
            }
        } catch (UnsupportedOperationException e) {
            return true;
        }
        return false;
    }

    private static boolean niegaMapa(Map<String, String> ro, int cual) {
        try {
            if (cual == 0) {
                ro.put("z", "z");
            } else if (cual == 1) {
                ro.keySet().remove("k");
            } else if (cual == 2) {
                Iterator<Map.Entry<String, String>> it = ro.entrySet().iterator();
                Map.Entry<String, String> e = it.next();
                e.setValue("otro");
            } else {
                ro.values().clear();
            }
        } catch (UnsupportedOperationException e) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
