// Por import y nombre simple: una llamada estatica calificada no resuelve (finding #274).
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Exercises java.util.Spliterator, Spliterators and the {@code spliterator()} the collections
 * now answer. Every method returns the number of things that came out wrong, so 0 is a pass.
 *
 * <p>The expected numbers are not invented: they were read off the JDK 25 with a throwaway
 * program, because a characteristics word is a fact about the specification, not something one
 * can derive. The same source compiles against the JDK 25, where {@code main} prints the same
 * counts.
 */
public class SpliterTest {

    // ORDERED|SIZED|SUBSIZED|IMMUTABLE — what Arrays.spliterator promises.
    static final int ARRAY_CHARS = 17488;

    // SIZED|SUBSIZED — what Spliterators.spliterator adds on its own.
    static final int SIZED_ONLY = 16448;

    /** Collects what a spliterator hands over, so the traversal can be compared as a string. */
    static final class Sink implements Consumer<String> {

        StringBuilder seen = new StringBuilder();

        int count;

        public void accept(String value) {
            this.seen.append(value);
            this.count = this.count + 1;
        }
    }

    static final class IntSink implements IntConsumer {

        int sum;

        int count;

        public void accept(int value) {
            this.sum = this.sum + value;
            this.count = this.count + 1;
        }
    }

    static String[] letters() {
        String[] a = new String[8];
        a[0] = "a";
        a[1] = "b";
        a[2] = "c";
        a[3] = "d";
        a[4] = "e";
        a[5] = "f";
        a[6] = "g";
        a[7] = "h";
        return a;
    }

    static List<String> list() {
        List<String> l = new ArrayList<String>();
        String[] a = SpliterTest.letters();
        for (int i = 0; i < a.length; i++) {
            l.add(a[i]);
        }
        return l;
    }

    /** The eight constants. A wrong bit here makes every other check meaningless. */
    public static int constantes() {
        int bad = 0;
        if (Spliterator.DISTINCT != 1 || Spliterator.SORTED != 4) {
            bad = bad + 1;
        }
        if (Spliterator.ORDERED != 16 || Spliterator.SIZED != 64) {
            bad = bad + 1;
        }
        if (Spliterator.NONNULL != 256 || Spliterator.IMMUTABLE != 1024) {
            bad = bad + 1;
        }
        if (Spliterator.CONCURRENT != 4096 || Spliterator.SUBSIZED != 16384) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Over an array: the size is exact and the traversal is the array's order. */
    public static int arreglos() {
        int bad = 0;
        Spliterator<String> s = Arrays.spliterator(SpliterTest.letters());
        if (s.characteristics() != SpliterTest.ARRAY_CHARS) {
            bad = bad + 1;
        }
        if (s.estimateSize() != 8L || s.getExactSizeIfKnown() != 8L) {
            bad = bad + 1;
        }
        if (!s.hasCharacteristics(Spliterator.SIZED)) {
            bad = bad + 1;
        }
        if (s.hasCharacteristics(Spliterator.DISTINCT)) {
            bad = bad + 1;
        }
        Sink sink = new Sink();
        s.forEachRemaining(sink);
        if (!sink.seen.toString().equals("abcdefgh") || sink.count != 8) {
            bad = bad + 1;
        }
        if (s.estimateSize() != 0L) {
            bad = bad + 1;
        }
        Spliterator<String> range = Arrays.spliterator(SpliterTest.letters(), 2, 6);
        if (range.estimateSize() != 4L) {
            bad = bad + 1;
        }
        Sink partial = new Sink();
        range.forEachRemaining(partial);
        if (!partial.seen.toString().equals("cdef")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** tryAdvance hands over one at a time, and says so. */
    public static int deAUno() {
        int bad = 0;
        Spliterator<String> s = Arrays.spliterator(SpliterTest.letters());
        Sink sink = new Sink();
        int taken = 0;
        while (s.tryAdvance(sink)) {
            taken = taken + 1;
        }
        if (taken != 8 || !sink.seen.toString().equals("abcdefgh")) {
            bad = bad + 1;
        }
        if (s.tryAdvance(sink)) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The split: the prefix goes to the returned half, the rest stays. Nothing is lost. */
    public static int reparto() {
        int bad = 0;
        Spliterator<String> s = Arrays.spliterator(SpliterTest.letters());
        Spliterator<String> pre = s.trySplit();
        if (pre == null) {
            return 1;
        }
        if (pre.estimateSize() != 4L || s.estimateSize() != 4L) {
            bad = bad + 1;
        }
        Sink head = new Sink();
        pre.forEachRemaining(head);
        Sink tail = new Sink();
        s.forEachRemaining(tail);
        if (!head.seen.toString().equals("abcd") || !tail.seen.toString().equals("efgh")) {
            bad = bad + 1;
        }
        // Un solo elemento no se parte.
        String[] one = new String[1];
        one[0] = "z";
        Spliterator<String> tiny = Arrays.spliterator(one);
        if (tiny.trySplit() != null) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Over a collection: late-binding, and the size is the collection's. */
    public static int colecciones() {
        int bad = 0;
        List<String> l = SpliterTest.list();
        Spliterator<String> s = Spliterators.spliterator(l, 0);
        if (s.characteristics() != SpliterTest.SIZED_ONLY) {
            bad = bad + 1;
        }
        if (s.estimateSize() != 8L) {
            bad = bad + 1;
        }
        Sink sink = new Sink();
        s.forEachRemaining(sink);
        if (!sink.seen.toString().equals("abcdefgh")) {
            bad = bad + 1;
        }
        // El que la lista misma responde.
        Spliterator<String> own = l.spliterator();
        if (!own.hasCharacteristics(Spliterator.ORDERED)) {
            bad = bad + 1;
        }
        if (!own.hasCharacteristics(Spliterator.SIZED)) {
            bad = bad + 1;
        }
        if (own.getExactSizeIfKnown() != 8L) {
            bad = bad + 1;
        }
        Sink fromList = new Sink();
        own.forEachRemaining(fromList);
        if (!fromList.seen.toString().equals("abcdefgh")) {
            bad = bad + 1;
        }
        // CONCURRENT saca SIZED de la mesa: el tamano puede cambiar debajo del recorrido, asi
        // que no se promete aunque la coleccion sepa contestar cuantos tiene ahora mismo.
        Spliterator<String> shared = Spliterators.spliterator(l, Spliterator.CONCURRENT);
        if (shared.characteristics() != Spliterator.CONCURRENT) {
            bad = bad + 1;
        }
        if (shared.hasCharacteristics(Spliterator.SIZED)) {
            bad = bad + 1;
        }
        if (shared.estimateSize() != 8L || shared.getExactSizeIfKnown() != -1L) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Over an iterator, with and without a declared size. */
    public static int iteradores() {
        int bad = 0;
        List<String> l = SpliterTest.list();
        Spliterator<String> known = Spliterators.spliterator(l.iterator(), 8L, 0);
        if (known.characteristics() != SpliterTest.SIZED_ONLY || known.estimateSize() != 8L) {
            bad = bad + 1;
        }
        Spliterator<String> pre = known.trySplit();
        if (pre == null) {
            return bad + 1;
        }
        // El primer lote se lleva los ocho: el lote arranca en 1024.
        if (pre.estimateSize() != 8L || known.estimateSize() != 0L) {
            bad = bad + 1;
        }
        Spliterator<String> unknown = Spliterators.spliteratorUnknownSize(l.iterator(), 0);
        if (unknown.characteristics() != 0 || unknown.estimateSize() != Long.MAX_VALUE) {
            bad = bad + 1;
        }
        if (unknown.getExactSizeIfKnown() != -1L) {
            bad = bad + 1;
        }
        Spliterator<String> half = unknown.trySplit();
        if (half == null) {
            return bad + 1;
        }
        // El pedazo de algo sin medir estima la mitad de lo que falta, no lo que se llevo.
        if (half.estimateSize() != Long.MAX_VALUE / 2L) {
            bad = bad + 1;
        }
        if (half.hasCharacteristics(Spliterator.SIZED)) {
            bad = bad + 1;
        }
        Sink sink = new Sink();
        half.forEachRemaining(sink);
        if (sink.count != 8 || !sink.seen.toString().equals("abcdefgh")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Nothing at all: the empty ones, which still have to answer every question. */
    public static int vacios() {
        int bad = 0;
        Spliterator<String> s = Spliterators.emptySpliterator();
        if (s.estimateSize() != 0L || s.getExactSizeIfKnown() != 0L) {
            bad = bad + 1;
        }
        if (s.characteristics() != SpliterTest.SIZED_ONLY) {
            bad = bad + 1;
        }
        if (s.trySplit() != null) {
            bad = bad + 1;
        }
        Sink sink = new Sink();
        if (s.tryAdvance(sink) || sink.count != 0) {
            bad = bad + 1;
        }
        s.forEachRemaining(sink);
        if (sink.count != 0) {
            bad = bad + 1;
        }
        Spliterator.OfInt ints = Spliterators.emptyIntSpliterator();
        if (ints.estimateSize() != 0L || ints.characteristics() != SpliterTest.SIZED_ONLY) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The primitive ones, where the whole point is that nothing gets boxed. */
    public static int primitivos() {
        int bad = 0;
        int[] numbers = new int[4];
        numbers[0] = 1;
        numbers[1] = 2;
        numbers[2] = 3;
        numbers[3] = 4;
        Spliterator.OfInt s = Arrays.spliterator(numbers);
        if (s.characteristics() != SpliterTest.ARRAY_CHARS || s.estimateSize() != 4L) {
            bad = bad + 1;
        }
        IntSink sink = new IntSink();
        s.forEachRemaining(sink);
        if (sink.sum != 10 || sink.count != 4) {
            bad = bad + 1;
        }
        Spliterator.OfInt other = Spliterators.spliterator(numbers, 0);
        if (other.characteristics() != SpliterTest.SIZED_ONLY) {
            bad = bad + 1;
        }
        Spliterator.OfInt head = other.trySplit();
        if (head == null) {
            return bad + 1;
        }
        IntSink first = new IntSink();
        head.forEachRemaining(first);
        IntSink rest = new IntSink();
        other.forEachRemaining(rest);
        if (first.sum != 3 || rest.sum != 7) {
            bad = bad + 1;
        }
        long[] longs = new long[3];
        longs[0] = 10L;
        longs[1] = 20L;
        longs[2] = 30L;
        Spliterator.OfLong overLongs = Arrays.spliterator(longs);
        if (overLongs.estimateSize() != 3L) {
            bad = bad + 1;
        }
        double[] doubles = new double[2];
        doubles[0] = 1.5d;
        doubles[1] = 2.5d;
        Spliterator.OfDouble overDoubles = Arrays.spliterator(doubles);
        if (overDoubles.estimateSize() != 2L) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Back to an iterator: what went in has to come out, in the same order. */
    public static int vuelta() {
        int bad = 0;
        Spliterator<String> s = Arrays.spliterator(SpliterTest.letters());
        Iterator<String> it = Spliterators.iterator(s);
        StringBuilder seen = new StringBuilder();
        while (it.hasNext()) {
            seen.append(it.next());
        }
        if (!seen.toString().equals("abcdefgh")) {
            bad = bad + 1;
        }
        if (it.hasNext()) {
            bad = bad + 1;
        }
        int[] numbers = new int[3];
        numbers[0] = 5;
        numbers[1] = 6;
        numbers[2] = 7;
        Spliterator.OfInt over = Arrays.spliterator(numbers);
        PrimitiveIterator.OfInt ints = Spliterators.iterator(over);
        int sum = 0;
        while (ints.hasNext()) {
            sum = sum + ints.nextInt();
        }
        if (sum != 18) {
            bad = bad + 1;
        }
        return bad;
    }

    /** SORTED, the only characteristic that owes an answer to a second question. */
    public static int ordenados() {
        int bad = 0;
        TreeSet<String> set = new TreeSet<String>();
        set.add("d");
        set.add("a");
        set.add("c");
        set.add("b");
        Spliterator<String> s = set.spliterator();
        if (!s.hasCharacteristics(Spliterator.SORTED)) {
            bad = bad + 1;
        }
        if (!s.hasCharacteristics(Spliterator.DISTINCT)) {
            bad = bad + 1;
        }
        // Orden natural: el comparador es null, y eso NO es lo mismo que negarse a contestar.
        if (s.getComparator() != null) {
            bad = bad + 1;
        }
        Sink sink = new Sink();
        s.forEachRemaining(sink);
        if (!sink.seen.toString().equals("abcd")) {
            bad = bad + 1;
        }
        // Uno que no es SORTED se niega, en vez de devolver null.
        Spliterator<String> plain = Arrays.spliterator(SpliterTest.letters());
        int caught = 0;
        try {
            Comparator<? super String> ignored = plain.getComparator();
        } catch (IllegalStateException expected) {
            caught = 1;
        }
        if (caught != 1) {
            bad = bad + 1;
        }
        return bad;
    }

    public static int todo() {
        return SpliterTest.constantes() + SpliterTest.arreglos() + SpliterTest.deAUno()
                + SpliterTest.reparto() + SpliterTest.colecciones() + SpliterTest.iteradores()
                + SpliterTest.vacios() + SpliterTest.primitivos() + SpliterTest.vuelta()
                + SpliterTest.ordenados();
    }

    public static void main(String[] args) {
        System.out.println("constantes   " + SpliterTest.constantes());
        System.out.println("arreglos     " + SpliterTest.arreglos());
        System.out.println("deAUno       " + SpliterTest.deAUno());
        System.out.println("reparto      " + SpliterTest.reparto());
        System.out.println("colecciones  " + SpliterTest.colecciones());
        System.out.println("iteradores   " + SpliterTest.iteradores());
        System.out.println("vacios       " + SpliterTest.vacios());
        System.out.println("primitivos   " + SpliterTest.primitivos());
        System.out.println("vuelta       " + SpliterTest.vuelta());
        System.out.println("ordenados    " + SpliterTest.ordenados());
        System.out.println("TOTAL        " + SpliterTest.todo());
    }
}
