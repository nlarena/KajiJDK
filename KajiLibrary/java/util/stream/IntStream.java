package java.util.stream;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.function.IntPredicate;
import java.util.function.IntFunction;

// Cross-package imports (needed regardless; same-package finder issue is #4).
import java.util.OptionalInt;
import java.util.OptionalDouble;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntToLongFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.Spliterator;
import java.util.Spliterators;

// KajiLibrary's java.util.stream.IntStream — a sequence of primitive ints supporting a
// functional pipeline: intermediate ops (filter/map) return a new IntStream, terminal ops
// (sum/reduce/forEach/count/toArray/…/min/max/average) consume it. Not generic, so a concrete
// `implements IntStream` sidesteps the generic-override check (#9).
//
// EAGER: each intermediate op materialises a fresh backing int[] (see IntStreamImpl). Correct
// for finite streams; the lazy Spliterator model is a later tier. A KajiLibrary subset.
//
// Absent on purpose: `summaryStatistics()` (java.util.IntSummaryStatistics does not exist in
// KajiLibrary and lives outside this package), the PrimitiveIterator.OfInt `iterator()` and
// `spliterator()` (idem), and `generate`/the two-arg `iterate`, which build INFINITE streams that
// an eager model cannot represent. The finite three-arg `iterate` is implemented.
//
// `builder()` and the nested `IntStream.Builder` are declared but not usable yet: every method
// CALL on a nested interface loaded from the classpath is silently dropped by this javac (see
// Stream's header and the defect notes). `mapMulti` works — its sink type is not generic.
// Rooted in BaseStream<Integer, IntStream>: iterator/isParallel/sequential/parallel/unordered/
// onClose/close come from there. The four S-returning ops are redeclared below with
// IntStream as the return type, exactly as the JDK does. `iterator()` hands out an
// Iterator<Integer> (boxed) rather than the JDK's PrimitiveIterator.OfInt, because
// java.util.PrimitiveIterator does not exist in KajiLibrary.
public interface IntStream extends BaseStream<Integer, IntStream> {

    IntStream filter(IntPredicate predicate);

    IntStream map(IntUnaryOperator mapper);

    // Map each element to an IntStream and concatenate the results.
    IntStream flatMap(IntFunction<? extends IntStream> mapper);

    // Widening bridges to the other primitive streams.
    LongStream mapToLong(IntToLongFunction mapper);

    DoubleStream mapToDouble(IntToDoubleFunction mapper);

    // The same elements, widened. `asLongStream`/`asDoubleStream` are `mapTo*` with the identity
    // conversion; the JDK declares them separately and so do we.
    LongStream asLongStream();

    DoubleStream asDoubleStream();

    IntStream distinct();

    IntStream sorted();

    IntStream limit(long maxSize);

    IntStream skip(long n);

    IntStream peek(IntConsumer action);


    // Prefix up to (excluding) the first element that fails `predicate`. `default`, as in the
    // JDK: the body only needs toArray(), so every implementation gets it for free. IntStreamImpl
    // still overrides it to walk its own backing array instead of a copy.
    default IntStream takeWhile(IntPredicate predicate) {
        int[] a = this.toArray();
        int n = 0;
        while (n < a.length && predicate.test(a[n])) {
            n = n + 1;
        }
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = a[i];
        }
        return new IntStreamImpl(out, n);
    }

    // The remainder after that same prefix.
    default IntStream dropWhile(IntPredicate predicate) {
        int[] a = this.toArray();
        int start = 0;
        while (start < a.length && predicate.test(a[start])) {
            start = start + 1;
        }
        int len = a.length - start;
        int[] out = new int[len];
        for (int i = 0; i < len; i++) {
            out[i] = a[start + i];
        }
        return new IntStreamImpl(out, len);
    }

    // Replace each element with zero or more elements, pushed into the IntConsumer. `default`,
    // as in the JDK (Java 16+).
    default IntStream mapMulti(IntMapMultiConsumer mapper) {
        int[] a = this.toArray();
        IntBuf buf = new IntBuf();
        for (int i = 0; i < a.length; i++) {
            mapper.accept(a[i], buf);
        }
        return buf.toStream();
    }

    // The sink handed to mapMulti's mapper. Nested in IntStream exactly as in the JDK.
    interface IntMapMultiConsumer {
        void accept(int value, IntConsumer ic);
    }

    // A mutable builder for an IntStream. Nested in IntStream exactly as in the JDK; `add` is
    // declared abstract here rather than `default` (the implementation is one line either way).
    interface Builder extends IntConsumer {

        void accept(int t);

        Builder add(int t);

        IntStream build();
    }

    // The builder implementation is nested INSIDE IntStream, unlike every other helper in this
    // file, because a nested type of an interface cannot be named from a sibling top-level class
    // once the file is in a named package: neither `IntStream.Builder` nor a bare `Builder`
    // resolves ("no se encuentra el símbolo"), while the identical code in the default package
    // compiles. Repro in the defect notes. The JDK keeps its equivalent in java.util.stream.Streams.
    final class BuilderImpl implements Builder {

        private final IntBuf buf;

        BuilderImpl() {
            this.buf = new IntBuf();
        }

        public void accept(int t) {
            this.buf.accept(t);
        }

        public Builder add(int t) {
            this.buf.accept(t);
            return this;
        }

        public IntStream build() {
            return this.buf.toStream();
        }
    }

    // Map each element to an object, producing a Stream<U>.
    <U> Stream<U> mapToObj(IntFunction<? extends U> mapper);

    // Sequential, so "any" is "first".
    OptionalInt findAny();

    // Covariant redeclarations of BaseStream's S-returning ops.
    IntStream sequential();

    IntStream parallel();

    IntStream unordered();

    IntStream onClose(Runnable closeHandler);

    void forEach(IntConsumer action);

    // Same as forEach for us: our streams always have an encounter order and are sequential.
    void forEachOrdered(IntConsumer action);

    int sum();

    int reduce(int identity, IntBinaryOperator op);

    // Fold with no identity: an empty stream gives an empty OptionalInt.
    OptionalInt reduce(IntBinaryOperator op);

    // Mutable reduction into a caller-supplied container.
    <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner);

    long count();

    int[] toArray();

    boolean anyMatch(IntPredicate predicate);

    boolean allMatch(IntPredicate predicate);

    boolean noneMatch(IntPredicate predicate);

    OptionalInt min();

    OptionalInt max();

    OptionalInt findFirst();

    OptionalDouble average();

    // Bridge to the object stream: box each int into an Integer.
    Stream<Integer> boxed();

    // --- factories ---

    static IntStream of(int... values) {
        return new IntStreamImpl(values, values.length);
    }

    // The single-element overload. Unlike Stream.of(T) this one is safe to declare: `of(int)`
    // and `of(int[])` are not confusable for a primitive element type, so an existing
    // `IntStream.of(someIntArray)` still binds to the array overload.
    static IntStream of(int t) {
        int[] one = new int[1];
        one[0] = t;
        return new IntStreamImpl(one, 1);
    }

    static Builder builder() {
        return new BuilderImpl();
    }

    static IntStream empty() {
        return new IntStreamImpl(new int[0], 0);
    }

    // The finite three-arg iterate (Java 9+): seed, next(seed), … while `hasNext` holds. The
    // two-arg infinite overload is not implementable eagerly (see Stream's class comment).
    static IntStream iterate(int seed, IntPredicate hasNext, IntUnaryOperator next) {
        int[] out = new int[16];
        int n = 0;
        int cur = seed;
        while (hasNext.test(cur)) {
            if (n == out.length) {
                int[] bigger = new int[out.length * 2];
                for (int k = 0; k < n; k++) {
                    bigger[k] = out[k];
                }
                out = bigger;
            }
            out[n] = cur;
            n = n + 1;
            cur = next.applyAsInt(cur);
        }
        return new IntStreamImpl(out, n);
    }

    // [startInclusive, endExclusive)
    static IntStream range(int startInclusive, int endExclusive) {
        int n = endExclusive - startInclusive;
        if (n < 0) {
            n = 0;
        }
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = startInclusive + i;
        }
        return new IntStreamImpl(a, n);
    }

    static IntStream rangeClosed(int startInclusive, int endInclusive) {
        return IntStream.range(startInclusive, endInclusive + 1);
    }

    // Concatenation: every element of `a`, then every element of `b`.
    static IntStream concat(IntStream a, IntStream b) {
        int[] left = a.toArray();
        int[] right = b.toArray();
        int[] out = new int[left.length + right.length];
        for (int i = 0; i < left.length; i++) {
            out[i] = left[i];
        }
        for (int i = 0; i < right.length; i++) {
            out[left.length + i] = right[i];
        }
        return new IntStreamImpl(out, out.length);
    }
}

// The eager implementation: a backing int[] and how many of its slots are live. Package-private
// (referenced only by IntStream's factories, in the same compilation unit — no #7).
final class IntStreamImpl implements IntStream {

    private final int[] data;
    private final int size;

    // Close handlers, shared by reference with every stream derived from this one,
    // so onClose()/close() on any stage of a pipeline sees the same list (as in the JDK).
    private final ArrayList<Runnable> closeHandlers;

    IntStreamImpl(int[] data, int size) {
        this.data = data;
        this.size = size;
        this.closeHandlers = new ArrayList<Runnable>();
    }

    IntStreamImpl(int[] data, int size, ArrayList<Runnable> closeHandlers) {
        this.data = data;
        this.size = size;
        this.closeHandlers = closeHandlers;
    }

    public IntStream filter(IntPredicate predicate) {
        int[] out = new int[this.size];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            if (predicate.test(this.data[i])) {
                out[n] = this.data[i];
                n = n + 1;
            }
        }
        return new IntStreamImpl(out, n, this.closeHandlers);
    }

    public IntStream map(IntUnaryOperator mapper) {
        int[] out = new int[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsInt(this.data[i]);
        }
        return new IntStreamImpl(out, this.size, this.closeHandlers);
    }

    // Map each element to a sub-stream and concatenate (eager: each sub-stream is drained via
    // toArray). The sub-stream goes into a local instead of chaining `mapper.apply(v).toArray()`:
    // chaining a call onto the result of a call into a classpath type mis-compiles (defect notes).
    public IntStream flatMap(IntFunction<? extends IntStream> mapper) {
        int[] out = new int[16];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            IntStream sub = mapper.apply(this.data[i]);
            if (sub == null) {
                continue;
            }
            int[] arr = sub.toArray();
            for (int j = 0; j < arr.length; j++) {
                if (n == out.length) {
                    int[] bigger = new int[out.length * 2];
                    for (int k = 0; k < n; k++) {
                        bigger[k] = out[k];
                    }
                    out = bigger;
                }
                out[n] = arr[j];
                n = n + 1;
            }
        }
        return new IntStreamImpl(out, n, this.closeHandlers);
    }

    public LongStream mapToLong(IntToLongFunction mapper) {
        long[] out = new long[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsLong(this.data[i]);
        }
        return LongStream.of(out);
    }

    public DoubleStream mapToDouble(IntToDoubleFunction mapper) {
        double[] out = new double[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsDouble(this.data[i]);
        }
        return DoubleStream.of(out);
    }

    // The `(long)`/`(double)` casts are explicit: this javac does not insert the widening
    // conversion on assignment (finding #217).
    public LongStream asLongStream() {
        long[] out = new long[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = (long) this.data[i];
        }
        return LongStream.of(out);
    }

    public DoubleStream asDoubleStream() {
        double[] out = new double[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = (double) this.data[i];
        }
        return DoubleStream.of(out);
    }

    // Keep the first occurrence of each value (O(n^2) membership scan — fine for a subset).
    public IntStream distinct() {
        int[] out = new int[this.size];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            int v = this.data[i];
            boolean seen = false;
            for (int j = 0; j < n; j++) {
                if (out[j] == v) {
                    seen = true;
                }
            }
            if (!seen) {
                out[n] = v;
                n = n + 1;
            }
        }
        return new IntStreamImpl(out, n, this.closeHandlers);
    }

    // Ascending order. Inlined insertion sort — can't call Arrays.sort (java.util static, #11).
    public IntStream sorted() {
        int[] out = new int[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = this.data[i];
        }
        for (int i = 1; i < this.size; i++) {
            int key = out[i];
            int j = i - 1;
            while (j >= 0 && out[j] > key) {
                out[j + 1] = out[j];
                j = j - 1;
            }
            out[j + 1] = key;
        }
        return new IntStreamImpl(out, this.size, this.closeHandlers);
    }

    public IntStream limit(long maxSize) {
        int n = this.size;
        if (maxSize < n) {
            n = (int) maxSize;
        }
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = this.data[i];
        }
        return new IntStreamImpl(out, n, this.closeHandlers);
    }

    public IntStream skip(long n) {
        int start;
        if (n > this.size) {
            start = this.size;
        } else {
            start = (int) n;
        }
        int len = this.size - start;
        int[] out = new int[len];
        for (int i = 0; i < len; i++) {
            out[i] = this.data[start + i];
        }
        return new IntStreamImpl(out, len, this.closeHandlers);
    }

    // Eager peek: applies `action` now and passes the same elements through.
    public IntStream peek(IntConsumer action) {
        for (int i = 0; i < this.size; i++) {
            action.accept(this.data[i]);
        }
        return this;
    }

    public void forEach(IntConsumer action) {
        for (int i = 0; i < this.size; i++) {
            action.accept(this.data[i]);
        }
    }

    public int sum() {
        int s = 0;
        for (int i = 0; i < this.size; i++) {
            s = s + this.data[i];
        }
        return s;
    }

    public int reduce(int identity, IntBinaryOperator op) {
        int acc = identity;
        for (int i = 0; i < this.size; i++) {
            acc = op.applyAsInt(acc, this.data[i]);
        }
        return acc;
    }

    public void forEachOrdered(IntConsumer action) {
        for (int i = 0; i < this.size; i++) {
            action.accept(this.data[i]);
        }
    }

    public OptionalInt reduce(IntBinaryOperator op) {
        if (this.size == 0) {
            return OptionalInt.empty();
        }
        int acc = this.data[0];
        for (int i = 1; i < this.size; i++) {
            acc = op.applyAsInt(acc, this.data[i]);
        }
        return OptionalInt.of(acc);
    }

    // The combiner is never invoked: our collect is sequential and never splits.
    public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        R container = supplier.get();
        for (int i = 0; i < this.size; i++) {
            accumulator.accept(container, this.data[i]);
        }
        return container;
    }

    public long count() {
        // Explicit widening cast: this javac does not insert the implicit int -> long
        // conversion on `return` (nor on assignment), so a bare `return this.size;` pushes an
        // int where the descriptor promises a long and the interpreter panics. See defect notes.
        return (long) this.size;
    }

    public int[] toArray() {
        int[] out = new int[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = this.data[i];
        }
        return out;
    }

    public boolean anyMatch(IntPredicate predicate) {
        for (int i = 0; i < this.size; i++) {
            if (predicate.test(this.data[i])) {
                return true;
            }
        }
        return false;
    }

    public boolean allMatch(IntPredicate predicate) {
        for (int i = 0; i < this.size; i++) {
            if (!predicate.test(this.data[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean noneMatch(IntPredicate predicate) {
        for (int i = 0; i < this.size; i++) {
            if (predicate.test(this.data[i])) {
                return false;
            }
        }
        return true;
    }

    public OptionalInt min() {
        if (this.size == 0) {
            return OptionalInt.empty();
        }
        int m = this.data[0];
        for (int i = 1; i < this.size; i++) {
            if (this.data[i] < m) {
                m = this.data[i];
            }
        }
        return OptionalInt.of(m);
    }

    public OptionalInt max() {
        if (this.size == 0) {
            return OptionalInt.empty();
        }
        int m = this.data[0];
        for (int i = 1; i < this.size; i++) {
            if (this.data[i] > m) {
                m = this.data[i];
            }
        }
        return OptionalInt.of(m);
    }

    public OptionalInt findFirst() {
        if (this.size == 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(this.data[0]);
    }

    public OptionalDouble average() {
        if (this.size == 0) {
            return OptionalDouble.empty();
        }
        int s = 0;
        for (int i = 0; i < this.size; i++) {
            s = s + this.data[i];
        }
        return OptionalDouble.of((double) s / (double) this.size);
    }

    public Stream<Integer> boxed() {
        Integer[] out = new Integer[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = Integer.valueOf(this.data[i]);
        }
        return Stream.<Integer>of(out);
    }

    public IntStream takeWhile(IntPredicate predicate) {
        int n = 0;
        while (n < this.size && predicate.test(this.data[n])) {
            n = n + 1;
        }
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = this.data[i];
        }
        return new IntStreamImpl(out, n, this.closeHandlers);
    }

    public IntStream dropWhile(IntPredicate predicate) {
        int start = 0;
        while (start < this.size && predicate.test(this.data[start])) {
            start = start + 1;
        }
        int len = this.size - start;
        int[] out = new int[len];
        for (int i = 0; i < len; i++) {
            out[i] = this.data[start + i];
        }
        return new IntStreamImpl(out, len, this.closeHandlers);
    }

    public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
        Object[] out = new Object[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.apply(this.data[i]);
        }
        return new StreamImpl<U>(out, this.size);
    }

    public OptionalInt findAny() {
        return this.findFirst();
    }

    // ---- BaseStream ------------------------------------------------------------------

    public Iterator<Integer> iterator() {
        int[] copy = new int[this.size];
        for (int i = 0; i < this.size; i++) {
            copy[i] = this.data[i];
        }
        return new IntStreamItr(copy);
    }

    // Always sequential: we have no fork/join substrate.
    public boolean isParallel() {
        return false;
    }

    public IntStream sequential() {
        return this;
    }

    public IntStream parallel() {
        return this;
    }

    public IntStream unordered() {
        return this;
    }

    public IntStream onClose(Runnable closeHandler) {
        this.closeHandlers.add(closeHandler);
        return this;
    }

    public void close() {
        for (int i = 0; i < this.closeHandlers.size(); i++) {
            Runnable handler = this.closeHandlers.get(i);
            handler.run();
        }
        this.closeHandlers.clear();
    }

    /**
     * A spliterator over this stream's elements.
     *
     * <p>Directly over the backing int[], which is a private snapshot: the split is a pure
     * index range, with no copying and no iterator in between. ORDERED because a stream has an
     * encounter order; SIZED and SUBSIZED come from the array-backed spliterator itself.
     */
    public Spliterator<Integer> spliterator() {
        return Spliterators.spliterator(this.data, 0, this.size, Spliterator.ORDERED);
    }

}

// The Iterator handed out by IntStreamImpl.iterator(): boxes each int on demand.
final class IntStreamItr implements Iterator<Integer> {

    private final int[] data;
    private int cursor;

    IntStreamItr(int[] data) {
        this.data = data;
        this.cursor = 0;
    }

    public boolean hasNext() {
        return this.cursor < this.data.length;
    }

    public Integer next() {
        int v = this.data[this.cursor];
        this.cursor = this.cursor + 1;
        return Integer.valueOf(v);
    }
}

// The IntConsumer that mapMulti pushes into: a growable int[] that becomes the result stream.
final class IntBuf implements IntConsumer {

    private int[] data;
    private int size;

    IntBuf() {
        this.data = new int[16];
        this.size = 0;
    }

    public void accept(int v) {
        if (this.size == this.data.length) {
            int[] bigger = new int[this.data.length * 2];
            for (int i = 0; i < this.size; i++) {
                bigger[i] = this.data[i];
            }
            this.data = bigger;
        }
        this.data[this.size] = v;
        this.size = this.size + 1;
    }

    IntStream toStream() {
        return new IntStreamImpl(this.data, this.size);
    }
}
