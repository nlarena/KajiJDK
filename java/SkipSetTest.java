import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Exercises ConcurrentSkipListSet on OUR VM. Every method returns the number of things that came
 * out wrong, so 0 is a pass.
 *
 * Every type it chains on is imported and named, because a type the file never writes makes the
 * compiler drop the whole expression in silence (finding #251).
 */
public class SkipSetTest {

    // Elements added in a scrambled order, so sorted iteration cannot pass by accident.
    static ConcurrentSkipListSet<Integer> filled() {
        ConcurrentSkipListSet<Integer> set = new ConcurrentSkipListSet<Integer>();
        int i = 0;
        int at = 0;
        while (i < 64) {
            set.add(Integer.valueOf(at));
            at = (at + 23) % 64;
            i = i + 1;
        }
        return set;
    }

    /** add/contains/size, and that adding twice reports the second one as not new. */
    public static int basico() {
        ConcurrentSkipListSet<Integer> set = SkipSetTest.filled();
        int bad = 0;
        if (set.size() != 64) {
            bad = bad + 1;
        }
        int i = 0;
        while (i < 64) {
            if (!set.contains(Integer.valueOf(i))) {
                bad = bad + 1;
            }
            i = i + 1;
        }
        if (set.contains(Integer.valueOf(999))) {
            bad = bad + 1;
        }
        if (set.add(Integer.valueOf(5))) {
            bad = bad + 1;
        }
        if (set.size() != 64) {
            bad = bad + 1;
        }
        if (!set.remove(Integer.valueOf(5))) {
            bad = bad + 1;
        }
        if (set.remove(Integer.valueOf(5))) {
            bad = bad + 1;
        }
        if (set.size() != 63) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Iteration comes out sorted, whatever the insertion order. */
    public static int orden() {
        ConcurrentSkipListSet<Integer> set = SkipSetTest.filled();
        int bad = 0;
        Iterator<Integer> it = set.iterator();
        int expected = 0;
        while (it.hasNext()) {
            Integer e = it.next();
            if (e.intValue() != expected) {
                bad = bad + 1;
            }
            expected = expected + 1;
        }
        if (expected != 64) {
            bad = bad + 1;
        }
        if (set.first().intValue() != 0) {
            bad = bad + 1;
        }
        if (set.last().intValue() != 63) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The four navigation queries, on a set with gaps so floor and ceiling differ. */
    public static int navegacion() {
        ConcurrentSkipListSet<Integer> set = new ConcurrentSkipListSet<Integer>();
        set.add(Integer.valueOf(10));
        set.add(Integer.valueOf(20));
        set.add(Integer.valueOf(30));
        int bad = 0;
        Integer f25 = set.floor(Integer.valueOf(25));
        if (f25 == null || f25.intValue() != 20) {
            bad = bad + 1;
        }
        Integer c25 = set.ceiling(Integer.valueOf(25));
        if (c25 == null || c25.intValue() != 30) {
            bad = bad + 1;
        }
        Integer f20 = set.floor(Integer.valueOf(20));
        if (f20 == null || f20.intValue() != 20) {
            bad = bad + 1;
        }
        Integer l20 = set.lower(Integer.valueOf(20));
        if (l20 == null || l20.intValue() != 10) {
            bad = bad + 1;
        }
        Integer h20 = set.higher(Integer.valueOf(20));
        if (h20 == null || h20.intValue() != 30) {
            bad = bad + 1;
        }
        if (set.lower(Integer.valueOf(10)) != null) {
            bad = bad + 1;
        }
        if (set.higher(Integer.valueOf(30)) != null) {
            bad = bad + 1;
        }
        Integer first = set.pollFirst();
        if (first == null || first.intValue() != 10) {
            bad = bad + 1;
        }
        Integer last = set.pollLast();
        if (last == null || last.intValue() != 30) {
            bad = bad + 1;
        }
        if (set.size() != 1) {
            bad = bad + 1;
        }
        return bad;
    }

    /**
     * The bounded views, and that writing through one lands in the base set.
     *
     * The locals are NavigableSet and not ConcurrentSkipListSet on purpose: the call resolves to
     * the interface declaration and not to the class's own override (finding #123).
     */
    public static int vistas() {
        ConcurrentSkipListSet<Integer> set = SkipSetTest.filled();
        int bad = 0;
        NavigableSet<Integer> mid =
                set.subSet(Integer.valueOf(10), true, Integer.valueOf(20), false);
        if (mid.size() != 10) {
            bad = bad + 1;
        }
        if (mid.contains(Integer.valueOf(9))) {
            bad = bad + 1;
        }
        if (!mid.contains(Integer.valueOf(10))) {
            bad = bad + 1;
        }
        if (mid.contains(Integer.valueOf(20))) {
            bad = bad + 1;
        }
        if (mid.first().intValue() != 10 || mid.last().intValue() != 19) {
            bad = bad + 1;
        }
        NavigableSet<Integer> head = set.headSet(Integer.valueOf(5), false);
        if (head.size() != 5) {
            bad = bad + 1;
        }
        NavigableSet<Integer> tail = set.tailSet(Integer.valueOf(60), true);
        if (tail.size() != 4) {
            bad = bad + 1;
        }
        SortedSet<Integer> half = set.subSet(Integer.valueOf(0), Integer.valueOf(32));
        if (half.size() != 32) {
            bad = bad + 1;
        }
        // A view is live: removing through it must remove from the base set.
        mid.remove(Integer.valueOf(15));
        if (set.contains(Integer.valueOf(15))) {
            bad = bad + 1;
        }
        if (mid.size() != 9) {
            bad = bad + 1;
        }
        // And reversed, the ends swap.
        NavigableSet<Integer> back = set.descendingSet();
        if (back.first().intValue() != 63 || back.last().intValue() != 0) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Everything at once, so one call answers "does it work". */
    public static int todo() {
        return SkipSetTest.basico() + SkipSetTest.orden() + SkipSetTest.navegacion()
                + SkipSetTest.vistas();
    }
}
