package java.util.stream;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.function.LongPredicate;
import java.util.function.LongFunction;

import java.util.OptionalLong;
import java.util.OptionalDouble;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongToIntFunction;
import java.util.function.LongToDoubleFunction;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.Spliterator;
import java.util.Spliterators;

// KajiLibrary's java.util.stream.LongStream — the long-specialized primitive stream, the mirror
// of IntStream over long values. EAGER (each intermediate op materialises a fresh long[]); a
// KajiLibrary subset. Not generic, so `implements LongStream` sidesteps #9.
// Absent on purpose: `summaryStatistics()` (no java.util.LongSummaryStatistics), the
// PrimitiveIterator.OfLong `iterator()`/`spliterator()`, and `generate`/two-arg `iterate` (both
// infinite). `builder()` is declared but not callable — see IntStream's header.
// Rooted in BaseStream<Long, LongStream>: iterator/isParallel/sequential/parallel/unordered/
// onClose/close come from there. The four S-returning ops are redeclared below with
// LongStream as the return type, exactly as the JDK does. `iterator()` hands out an
// Iterator<Long> (boxed) rather than the JDK's PrimitiveIterator.OfLong, because
// java.util.PrimitiveIterator does not exist in KajiLibrary.
public interface LongStream extends BaseStream<Long, LongStream> {

    LongStream filter(LongPredicate predicate);

    LongStream map(LongUnaryOperator mapper);

    // Map each element to a LongStream and concatenate the results.
    LongStream flatMap(LongFunction<? extends LongStream> mapper);

    // Bridges to the other primitive streams. `mapToInt` narrows, so the mapper must do the
    // narrowing itself; `asDoubleStream` is the identity widening.
    IntStream mapToInt(LongToIntFunction mapper);

    DoubleStream mapToDouble(LongToDoubleFunction mapper);

    DoubleStream asDoubleStream();

    LongStream distinct();

    LongStream sorted();

    LongStream limit(long maxSize);

    LongStream skip(long n);

    LongStream peek(LongConsumer action);


    // Prefix up to (excluding) the first element that fails `predicate`. `default`, as in the
    // JDK: the body only needs toArray(), so every implementation gets it for free. LongStreamImpl
    // still overrides it to walk its own backing array instead of a copy.
    default LongStream takeWhile(LongPredicate predicate) {
        long[] a = this.toArray();
        int n = 0;
        while (n < a.length && predicate.test(a[n])) {
            n = n + 1;
        }
        long[] out = new long[n];
        for (int i = 0; i < n; i++) {
            out[i] = a[i];
        }
        return new LongStreamImpl(out, n);
    }

    // The remainder after that same prefix.
    default LongStream dropWhile(LongPredicate predicate) {
        long[] a = this.toArray();
        int start = 0;
        while (start < a.length && predicate.test(a[start])) {
            start = start + 1;
        }
        int len = a.length - start;
        long[] out = new long[len];
        for (int i = 0; i < len; i++) {
            out[i] = a[start + i];
        }
        return new LongStreamImpl(out, len);
    }

    // Replace each element with zero or more elements, pushed into the LongConsumer. `default`,
    // as in the JDK (Java 16+).
    default LongStream mapMulti(LongMapMultiConsumer mapper) {
        long[] a = this.toArray();
        LongBuf buf = new LongBuf();
        for (int i = 0; i < a.length; i++) {
            mapper.accept(a[i], buf);
        }
        return buf.toStream();
    }

    // The sink handed to mapMulti's mapper. Nested in LongStream exactly as in the JDK.
    interface LongMapMultiConsumer {
        void accept(long value, LongConsumer ic);
    }

    // A mutable builder for a LongStream. Nested exactly as in the JDK; `add` is declared
    // abstract here rather than `default` (the implementation is one line either way).
    interface Builder extends LongConsumer {

        void accept(long t);

        Builder add(long t);

        LongStream build();
    }

    // The builder implementation is nested INSIDE LongStream, unlike every other helper in this
    // file, because a nested type of an interface cannot be named from a sibling top-level class
    // once the file is in a named package: neither `LongStream.Builder` nor a bare `Builder`
    // resolves, while the identical code in the default package compiles. Repro in the notes.
    final class BuilderImpl implements Builder {

        private final LongBuf buf;

        BuilderImpl() {
            this.buf = new LongBuf();
        }

        public void accept(long t) {
            this.buf.accept(t);
        }

        public Builder add(long t) {
            this.buf.accept(t);
            return this;
        }

        public LongStream build() {
            return this.buf.toStream();
        }
    }

    // Map each element to an object, producing a Stream<U>.
    <U> Stream<U> mapToObj(LongFunction<? extends U> mapper);

    // Sequential, so "any" is "first".
    OptionalLong findAny();

    // Covariant redeclarations of BaseStream's S-returning ops.
    LongStream sequential();

    LongStream parallel();

    LongStream unordered();

    LongStream onClose(Runnable closeHandler);

    void forEach(LongConsumer action);

    // Same as forEach for us: our streams always have an encounter order and are sequential.
    void forEachOrdered(LongConsumer action);

    long sum();

    long reduce(long identity, LongBinaryOperator op);

    // Fold with no identity: an empty stream gives an empty OptionalLong.
    OptionalLong reduce(LongBinaryOperator op);

    // Mutable reduction into a caller-supplied container.
    <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner);

    long count();

    long[] toArray();

    boolean anyMatch(LongPredicate predicate);

    boolean allMatch(LongPredicate predicate);

    boolean noneMatch(LongPredicate predicate);

    OptionalLong min();

    OptionalLong max();

    OptionalLong findFirst();

    OptionalDouble average();

    // Bridge to the object stream: box each long into a Long.
    Stream<Long> boxed();

    static LongStream of(long... values) {
        return new LongStreamImpl(values, values.length);
    }

    // The single-element overload; safe next to `of(long[])` because a primitive element type
    // and its array are not confusable (unlike Stream.of(T) vs Stream.of(T[])).
    static LongStream of(long t) {
        long[] one = new long[1];
        one[0] = t;
        return new LongStreamImpl(one, 1);
    }

    static Builder builder() {
        return new BuilderImpl();
    }

    static LongStream empty() {
        return new LongStreamImpl(new long[0], 0);
    }

    // The finite three-arg iterate (Java 9+). The two-arg infinite overload is not implementable
    // eagerly; see Stream's class comment.
    static LongStream iterate(long seed, LongPredicate hasNext, LongUnaryOperator next) {
        long[] out = new long[16];
        int n = 0;
        long cur = seed;
        while (hasNext.test(cur)) {
            if (n == out.length) {
                long[] bigger = new long[out.length * 2];
                for (int k = 0; k < n; k++) {
                    bigger[k] = out[k];
                }
                out = bigger;
            }
            out[n] = cur;
            n = n + 1;
            cur = next.applyAsLong(cur);
        }
        return new LongStreamImpl(out, n);
    }

    static LongStream range(long startInclusive, long endExclusive) {
        int n = (int) (endExclusive - startInclusive);
        if (n < 0) {
            n = 0;
        }
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = startInclusive + i;
        }
        return new LongStreamImpl(a, n);
    }

    static LongStream rangeClosed(long startInclusive, long endInclusive) {
        return LongStream.range(startInclusive, endInclusive + 1);
    }

    // Concatenation: every element of `a`, then every element of `b`.
    static LongStream concat(LongStream a, LongStream b) {
        long[] left = a.toArray();
        long[] right = b.toArray();
        long[] out = new long[left.length + right.length];
        for (int i = 0; i < left.length; i++) {
            out[i] = left[i];
        }
        for (int i = 0; i < right.length; i++) {
            out[left.length + i] = right[i];
        }
        return new LongStreamImpl(out, out.length);
    }
}

final class LongStreamImpl implements LongStream {

    private final long[] data;
    private final int size;

    // Close handlers, shared by reference with every stream derived from this one,
    // so onClose()/close() on any stage of a pipeline sees the same list (as in the JDK).
    private final ArrayList<Runnable> closeHandlers;

    LongStreamImpl(long[] data, int size) {
        this.data = data;
        this.size = size;
        this.closeHandlers = new ArrayList<Runnable>();
    }

    LongStreamImpl(long[] data, int size, ArrayList<Runnable> closeHandlers) {
        this.data = data;
        this.size = size;
        this.closeHandlers = closeHandlers;
    }

    public LongStream filter(LongPredicate predicate) {
        long[] out = new long[this.size];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            if (predicate.test(this.data[i])) {
                out[n] = this.data[i];
                n = n + 1;
            }
        }
        return new LongStreamImpl(out, n, this.closeHandlers);
    }

    public LongStream map(LongUnaryOperator mapper) {
        long[] out = new long[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsLong(this.data[i]);
        }
        return new LongStreamImpl(out, this.size, this.closeHandlers);
    }

    // The sub-stream goes into a local rather than chaining `mapper.apply(v).toArray()`:
    // chaining onto the result of a call into a classpath type mis-compiles (defect notes).
    public LongStream flatMap(LongFunction<? extends LongStream> mapper) {
        long[] out = new long[16];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            LongStream sub = mapper.apply(this.data[i]);
            if (sub == null) {
                continue;
            }
            long[] arr = sub.toArray();
            for (int j = 0; j < arr.length; j++) {
                if (n == out.length) {
                    long[] bigger = new long[out.length * 2];
                    for (int k = 0; k < n; k++) {
                        bigger[k] = out[k];
                    }
                    out = bigger;
                }
                out[n] = arr[j];
                n = n + 1;
            }
        }
        return new LongStreamImpl(out, n, this.closeHandlers);
    }

    public IntStream mapToInt(LongToIntFunction mapper) {
        int[] out = new int[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsInt(this.data[i]);
        }
        return IntStream.of(out);
    }

    public DoubleStream mapToDouble(LongToDoubleFunction mapper) {
        double[] out = new double[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsDouble(this.data[i]);
        }
        return DoubleStream.of(out);
    }

    // The `(double)` cast is explicit: this javac does not insert the widening conversion (#217).
    public DoubleStream asDoubleStream() {
        double[] out = new double[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = (double) this.data[i];
        }
        return DoubleStream.of(out);
    }

    public LongStream distinct() {
        long[] out = new long[this.size];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            long v = this.data[i];
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
        return new LongStreamImpl(out, n, this.closeHandlers);
    }

    public LongStream sorted() {
        long[] out = new long[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = this.data[i];
        }
        for (int i = 1; i < this.size; i++) {
            long key = out[i];
            int j = i - 1;
            while (j >= 0 && out[j] > key) {
                out[j + 1] = out[j];
                j = j - 1;
            }
            out[j + 1] = key;
        }
        return new LongStreamImpl(out, this.size, this.closeHandlers);
    }

    public LongStream limit(long maxSize) {
        int n = this.size;
        if (maxSize < n) {
            n = (int) maxSize;
        }
        long[] out = new long[n];
        for (int i = 0; i < n; i++) {
            out[i] = this.data[i];
        }
        return new LongStreamImpl(out, n, this.closeHandlers);
    }

    public LongStream skip(long n) {
        int start;
        if (n > this.size) {
            start = this.size;
        } else {
            start = (int) n;
        }
        int len = this.size - start;
        long[] out = new long[len];
        for (int i = 0; i < len; i++) {
            out[i] = this.data[start + i];
        }
        return new LongStreamImpl(out, len, this.closeHandlers);
    }

    public LongStream peek(LongConsumer action) {
        for (int i = 0; i < this.size; i++) {
            action.accept(this.data[i]);
        }
        return this;
    }

    public void forEach(LongConsumer action) {
        for (int i = 0; i < this.size; i++) {
            action.accept(this.data[i]);
        }
    }

    public long sum() {
        long s = 0L;
        for (int i = 0; i < this.size; i++) {
            s = s + this.data[i];
        }
        return s;
    }

    public long reduce(long identity, LongBinaryOperator op) {
        long acc = identity;
        for (int i = 0; i < this.size; i++) {
            acc = op.applyAsLong(acc, this.data[i]);
        }
        return acc;
    }

    public void forEachOrdered(LongConsumer action) {
        for (int i = 0; i < this.size; i++) {
            action.accept(this.data[i]);
        }
    }

    public OptionalLong reduce(LongBinaryOperator op) {
        if (this.size == 0) {
            return OptionalLong.empty();
        }
        long acc = this.data[0];
        for (int i = 1; i < this.size; i++) {
            acc = op.applyAsLong(acc, this.data[i]);
        }
        return OptionalLong.of(acc);
    }

    // The combiner is never invoked: our collect is sequential and never splits.
    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
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

    public long[] toArray() {
        long[] out = new long[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = this.data[i];
        }
        return out;
    }

    public boolean anyMatch(LongPredicate predicate) {
        for (int i = 0; i < this.size; i++) {
            if (predicate.test(this.data[i])) {
                return true;
            }
        }
        return false;
    }

    public boolean allMatch(LongPredicate predicate) {
        for (int i = 0; i < this.size; i++) {
            if (!predicate.test(this.data[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean noneMatch(LongPredicate predicate) {
        for (int i = 0; i < this.size; i++) {
            if (predicate.test(this.data[i])) {
                return false;
            }
        }
        return true;
    }

    public OptionalLong min() {
        if (this.size == 0) {
            return OptionalLong.empty();
        }
        long m = this.data[0];
        for (int i = 1; i < this.size; i++) {
            if (this.data[i] < m) {
                m = this.data[i];
            }
        }
        return OptionalLong.of(m);
    }

    public OptionalLong max() {
        if (this.size == 0) {
            return OptionalLong.empty();
        }
        long m = this.data[0];
        for (int i = 1; i < this.size; i++) {
            if (this.data[i] > m) {
                m = this.data[i];
            }
        }
        return OptionalLong.of(m);
    }

    public OptionalLong findFirst() {
        if (this.size == 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(this.data[0]);
    }

    public OptionalDouble average() {
        if (this.size == 0) {
            return OptionalDouble.empty();
        }
        long s = 0L;
        for (int i = 0; i < this.size; i++) {
            s = s + this.data[i];
        }
        return OptionalDouble.of((double) s / (double) this.size);
    }

    public Stream<Long> boxed() {
        Long[] out = new Long[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = Long.valueOf(this.data[i]);
        }
        return Stream.<Long>of(out);
    }

    public LongStream takeWhile(LongPredicate predicate) {
        int n = 0;
        while (n < this.size && predicate.test(this.data[n])) {
            n = n + 1;
        }
        long[] out = new long[n];
        for (int i = 0; i < n; i++) {
            out[i] = this.data[i];
        }
        return new LongStreamImpl(out, n, this.closeHandlers);
    }

    public LongStream dropWhile(LongPredicate predicate) {
        int start = 0;
        while (start < this.size && predicate.test(this.data[start])) {
            start = start + 1;
        }
        int len = this.size - start;
        long[] out = new long[len];
        for (int i = 0; i < len; i++) {
            out[i] = this.data[start + i];
        }
        return new LongStreamImpl(out, len, this.closeHandlers);
    }

    public <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
        Object[] out = new Object[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.apply(this.data[i]);
        }
        return new StreamImpl<U>(out, this.size);
    }

    public OptionalLong findAny() {
        return this.findFirst();
    }

    // ---- BaseStream ------------------------------------------------------------------

    public Iterator<Long> iterator() {
        long[] copy = new long[this.size];
        for (int i = 0; i < this.size; i++) {
            copy[i] = this.data[i];
        }
        return new LongStreamItr(copy);
    }

    // Always sequential: we have no fork/join substrate.
    public boolean isParallel() {
        return false;
    }

    public LongStream sequential() {
        return this;
    }

    public LongStream parallel() {
        return this;
    }

    public LongStream unordered() {
        return this;
    }

    public LongStream onClose(Runnable closeHandler) {
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
     * <p>Directly over the backing long[], which is a private snapshot: the split is a pure
     * index range, with no copying and no iterator in between. ORDERED because a stream has an
     * encounter order; SIZED and SUBSIZED come from the array-backed spliterator itself.
     */
    public Spliterator<Long> spliterator() {
        return Spliterators.spliterator(this.data, 0, this.size, Spliterator.ORDERED);
    }

}

// The Iterator handed out by LongStreamImpl.iterator(): boxes each long on demand.
final class LongStreamItr implements Iterator<Long> {

    private final long[] data;
    private int cursor;

    LongStreamItr(long[] data) {
        this.data = data;
        this.cursor = 0;
    }

    public boolean hasNext() {
        return this.cursor < this.data.length;
    }

    public Long next() {
        long v = this.data[this.cursor];
        this.cursor = this.cursor + 1;
        return Long.valueOf(v);
    }
}

// The LongConsumer that mapMulti pushes into: a growable long[] that becomes the result stream.
final class LongBuf implements LongConsumer {

    private long[] data;
    private int size;

    LongBuf() {
        this.data = new long[16];
        this.size = 0;
    }

    public void accept(long v) {
        if (this.size == this.data.length) {
            long[] bigger = new long[this.data.length * 2];
            for (int i = 0; i < this.size; i++) {
                bigger[i] = this.data[i];
            }
            this.data = bigger;
        }
        this.data[this.size] = v;
        this.size = this.size + 1;
    }

    LongStream toStream() {
        return new LongStreamImpl(this.data, this.size);
    }
}
