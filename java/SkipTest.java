import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Exercises ConcurrentSkipListMap on OUR VM. Every method returns the number of things that came
 * out wrong, so 0 is a pass.
 *
 * Every type it chains on is imported and named, because a type the file never writes makes the
 * compiler drop the whole expression in silence (finding #251).
 */
public class SkipTest {

    // Keys inserted in a scrambled order, so nothing passes by accident of insertion order.
    static int[] scrambled() {
        int[] out = new int[64];
        int i = 0;
        int at = 0;
        while (i < 64) {
            out[i] = at;
            at = (at + 23) % 64;
            i = i + 1;
        }
        return out;
    }

    static ConcurrentSkipListMap<Integer, String> filled() {
        ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<Integer, String>();
        int[] keys = SkipTest.scrambled();
        int i = 0;
        while (i < keys.length) {
            map.put(Integer.valueOf(keys[i]), "v" + keys[i]);
            i = i + 1;
        }
        return map;
    }

    /** put/get/size, and that a re-put replaces rather than duplicates. */
    public static int basico() {
        ConcurrentSkipListMap<Integer, String> map = SkipTest.filled();
        int bad = 0;
        if (map.size() != 64) {
            bad = bad + 1;
        }
        int i = 0;
        while (i < 64) {
            String got = map.get(Integer.valueOf(i));
            if (got == null || !got.equals("v" + i)) {
                bad = bad + 1;
            }
            i = i + 1;
        }
        String previous = map.put(Integer.valueOf(5), "otro");
        if (previous == null || !previous.equals("v5")) {
            bad = bad + 1;
        }
        if (map.size() != 64) {
            bad = bad + 1;
        }
        if (!map.get(Integer.valueOf(5)).equals("otro")) {
            bad = bad + 1;
        }
        if (map.get(Integer.valueOf(999)) != null) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The whole point of a skip list: iteration comes out SORTED, whatever the insertion order. */
    public static int orden() {
        ConcurrentSkipListMap<Integer, String> map = SkipTest.filled();
        int bad = 0;
        NavigableSet<Integer> keys = map.keySet();
        Iterator<Integer> it = keys.iterator();
        int expected = 0;
        while (it.hasNext()) {
            Integer key = it.next();
            if (key.intValue() != expected) {
                bad = bad + 1;
            }
            expected = expected + 1;
        }
        if (expected != 64) {
            bad = bad + 1;
        }
        if (map.firstKey().intValue() != 0) {
            bad = bad + 1;
        }
        if (map.lastKey().intValue() != 63) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The navigable queries, on a map with gaps so floor and ceiling differ. */
    public static int navegacion() {
        ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<Integer, String>();
        map.put(Integer.valueOf(10), "a");
        map.put(Integer.valueOf(20), "b");
        map.put(Integer.valueOf(30), "c");
        int bad = 0;
        Integer f25 = map.floorKey(Integer.valueOf(25));
        if (f25 == null || f25.intValue() != 20) {
            bad = bad + 1;
        }
        Integer c25 = map.ceilingKey(Integer.valueOf(25));
        if (c25 == null || c25.intValue() != 30) {
            bad = bad + 1;
        }
        // On an exact hit floor and ceiling return it; lower and higher must step past.
        Integer f20 = map.floorKey(Integer.valueOf(20));
        if (f20 == null || f20.intValue() != 20) {
            bad = bad + 1;
        }
        Integer c20 = map.ceilingKey(Integer.valueOf(20));
        if (c20 == null || c20.intValue() != 20) {
            bad = bad + 1;
        }
        Integer l20 = map.lowerKey(Integer.valueOf(20));
        if (l20 == null || l20.intValue() != 10) {
            bad = bad + 1;
        }
        Integer h20 = map.higherKey(Integer.valueOf(20));
        if (h20 == null || h20.intValue() != 30) {
            bad = bad + 1;
        }
        if (map.lowerKey(Integer.valueOf(10)) != null) {
            bad = bad + 1;
        }
        if (map.higherKey(Integer.valueOf(30)) != null) {
            bad = bad + 1;
        }
        Map.Entry<Integer, String> first = map.firstEntry();
        if (first == null || first.getKey().intValue() != 10 || !first.getValue().equals("a")) {
            bad = bad + 1;
        }
        Map.Entry<Integer, String> last = map.lastEntry();
        if (last == null || last.getKey().intValue() != 30) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Removal, including the two-argument form that must only fire on a value match. */
    public static int borrado() {
        ConcurrentSkipListMap<Integer, String> map = SkipTest.filled();
        int bad = 0;
        int i = 0;
        while (i < 64) {
            if (i % 2 == 0) {
                String gone = map.remove(Integer.valueOf(i));
                if (gone == null || !gone.equals("v" + i)) {
                    bad = bad + 1;
                }
            }
            i = i + 1;
        }
        if (map.size() != 32) {
            bad = bad + 1;
        }
        i = 0;
        while (i < 64) {
            boolean present = map.containsKey(Integer.valueOf(i));
            if (present != (i % 2 == 1)) {
                bad = bad + 1;
            }
            i = i + 1;
        }
        // Wrong value: must not remove.
        if (map.remove(Integer.valueOf(7), "noesa")) {
            bad = bad + 1;
        }
        if (!map.containsKey(Integer.valueOf(7))) {
            bad = bad + 1;
        }
        if (!map.remove(Integer.valueOf(7), "v7")) {
            bad = bad + 1;
        }
        if (map.containsKey(Integer.valueOf(7))) {
            bad = bad + 1;
        }
        return bad;
    }

    /** putIfAbsent and the two replace forms, which are what make it a ConcurrentMap. */
    public static int atomicos() {
        ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<Integer, String>();
        int bad = 0;
        if (map.putIfAbsent(Integer.valueOf(1), "uno") != null) {
            bad = bad + 1;
        }
        String kept = map.putIfAbsent(Integer.valueOf(1), "otro");
        if (kept == null || !kept.equals("uno")) {
            bad = bad + 1;
        }
        if (!map.get(Integer.valueOf(1)).equals("uno")) {
            bad = bad + 1;
        }
        boolean r1 = map.replace(Integer.valueOf(1), "noesa", "nuevo");
        if (r1) {
            bad = bad + 1;
        }
        boolean r2 = map.replace(Integer.valueOf(1), "uno", "nuevo");
        if (!r2) {
            bad = bad + 1;
        }
        if (!map.get(Integer.valueOf(1)).equals("nuevo")) {
            bad = bad + 1;
        }
        if (map.replace(Integer.valueOf(99), "x") != null) {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * The bounded views, and that writing through one lands in the base map.
     *
     * The locals are NavigableMap and not ConcurrentNavigableMap on purpose: assigning to the
     * narrower type does not compile, because the call resolves to NavigableMap's declaration
     * instead of the class's own override (finding #123, from the caller's side).
     */
    public static int vistas() {
        ConcurrentSkipListMap<Integer, String> map = SkipTest.filled();
        int bad = 0;
        NavigableMap<Integer, String> mid =
                map.subMap(Integer.valueOf(10), true, Integer.valueOf(20), false);
        if (mid.size() != 10) {
            bad = bad + 1;
        }
        if (mid.containsKey(Integer.valueOf(9))) {
            bad = bad + 1;
        }
        if (!mid.containsKey(Integer.valueOf(10))) {
            bad = bad + 1;
        }
        if (mid.containsKey(Integer.valueOf(20))) {
            bad = bad + 1;
        }
        if (mid.firstKey().intValue() != 10 || mid.lastKey().intValue() != 19) {
            bad = bad + 1;
        }
        NavigableMap<Integer, String> head = map.headMap(Integer.valueOf(5), false);
        if (head.size() != 5) {
            bad = bad + 1;
        }
        NavigableMap<Integer, String> tail = map.tailMap(Integer.valueOf(60), true);
        if (tail.size() != 4) {
            bad = bad + 1;
        }
        // A view is live: removing through it must remove from the base map.
        mid.remove(Integer.valueOf(15));
        if (map.containsKey(Integer.valueOf(15))) {
            bad = bad + 1;
        }
        if (mid.size() != 9) {
            bad = bad + 1;
        }
        // And reversed, the ends swap.
        NavigableMap<Integer, String> back = map.descendingMap();
        if (back.firstKey().intValue() != 63 || back.lastKey().intValue() != 0) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The entry set and the values collection. */
    public static int colecciones() {
        ConcurrentSkipListMap<Integer, String> map = SkipTest.filled();
        int bad = 0;
        Set<Map.Entry<Integer, String>> entries = map.entrySet();
        if (entries.size() != 64) {
            bad = bad + 1;
        }
        Iterator<Map.Entry<Integer, String>> it = entries.iterator();
        int seen = 0;
        int expected = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, String> e = it.next();
            if (e.getKey().intValue() != expected) {
                bad = bad + 1;
            }
            if (!e.getValue().equals("v" + expected)) {
                bad = bad + 1;
            }
            expected = expected + 1;
            seen = seen + 1;
        }
        if (seen != 64) {
            bad = bad + 1;
        }
        Collection<String> values = map.values();
        if (values.size() != 64) {
            bad = bad + 1;
        }
        if (!values.contains("v42")) {
            bad = bad + 1;
        }
        if (values.contains("nada")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Everything at once, so one call answers "does it work". */
    public static int todo() {
        return SkipTest.basico() + SkipTest.orden() + SkipTest.navegacion() + SkipTest.borrado()
                + SkipTest.atomicos() + SkipTest.vistas() + SkipTest.colecciones();
    }
}
