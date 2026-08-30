package java.util.stream;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.function.DoublePredicate;
import java.util.function.DoubleFunction;

import java.util.OptionalDouble;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.Spliterator;
import java.util.Spliterators;

// KajiLibrary's java.util.stream.DoubleStream — the double-specialized primitive stream, the
// mirror of IntStream/LongStream over double values. EAGER; a KajiLibrary subset. Unlike Int/Long
// there is no range() (a double range isn't well-defined). Not generic, so no #9.
// Rooted in BaseStream<Double, DoubleStream>: iterator/isParallel/sequential/parallel/unordered/
// onClose/close come from there. The four S-returning ops are redeclared below with
// DoubleStream as the return type, exactly as the JDK does. `iterator()` hands out an
// Iterator<Double> (boxed) rather than the JDK's PrimitiveIterator.OfDouble, because
// java.util.PrimitiveIterator does not exist in KajiLibrary.
// Absent on purpose: `summaryStatistics()` (no java.util.DoubleSummaryStatistics), the
// PrimitiveIterator.OfDouble `iterator()`/`spliterator()`, and `generate`/two-arg `iterate` (both
// infinite). `builder()` is declared but not callable — see IntStream's header.
public interface DoubleStream extends BaseStream<Double, DoubleStream> {

    DoubleStream filter(DoublePredicate predicate);

    DoubleStream map(DoubleUnaryOperator mapper);

    // Map each element to a DoubleStream and concatenate the results.
    DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper);

    // Bridges to the integral primitive streams. Both narrow, so the mapper does the conversion.
    IntStream mapToInt(DoubleToIntFunction mapper);

    LongStream mapToLong(DoubleToLongFunction mapper);

    DoubleStream distinct();

    DoubleStream sorted();

    DoubleStream limit(long maxSize);

    DoubleStream skip(long n);

    DoubleStream peek(DoubleConsumer action);


    // Prefix up to (excluding) the first element that fails `predicate`. `default`, as in the
    // JDK: the body only needs toArray(), so every implementation gets it for free. DoubleStreamImpl
    // still overrides it to walk its own backing array instead of a copy.
    default DoubleStream takeWhile(DoublePredicate predicate) {
        double[] a = this.toArray();
        int n = 0;
        while (n < a.length && predicate.test(a[n])) {
            n = n + 1;
        }
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            out[i] = a[i];
        }
        return new DoubleStreamImpl(out, n);
    }

    // The remainder after that same prefix.
    default DoubleStream dropWhile(DoublePredicate predicate) {
        double[] a = this.toArray();
        int start = 0;
        while (start < a.length && predicate.test(a[start])) {
            start = start + 1;
        }
        int len = a.length - start;
        double[] out = new double[len];
        for (int i = 0; i < len; i++) {
            out[i] = a[start + i];
        }
        return new DoubleStreamImpl(out, len);
    }

    // Replace each element with zero or more elements, pushed into the DoubleConsumer. `default`,
    // as in the JDK (Java 16+).
    default DoubleStream mapMulti(DoubleMapMultiConsumer mapper) {
        double[] a = this.toArray();
        DoubleBuf buf = new DoubleBuf();
        for (int i = 0; i < a.length; i++) {
            mapper.accept(a[i], buf);
        }
        return buf.toStream();
    }

    // The sink handed to mapMulti's mapper. Nested in DoubleStream exactly as in the JDK.
    interface DoubleMapMultiConsumer {
        void accept(double value, DoubleConsumer ic);
    }

    // A mutable builder for a DoubleStream. Nested exactly as in the JDK; `add` is declared
    // abstract here rather than `default` (the implementation is one line either way).
    interface Builder extends DoubleConsumer {

        void accept(double t);

        Builder add(double t);

        DoubleStream build();
    }

    // The builder implementation is nested INSIDE DoubleStream, unlike every other helper in this
    // file, because a nested type of an interface cannot be named from a sibling top-level class
    // once the file is in a named package: neither `DoubleStream.Builder` nor a bare `Builder`
    // resolves, while the identical code in the default package compiles. Repro in the notes.
    final class BuilderImpl implements Builder {

        private final DoubleBuf buf;

        BuilderImpl() {
            this.buf = new DoubleBuf();
        }

        public void accept(double t) {
            this.buf.accept(t);
        }

        public Builder add(double t) {
            this.buf.accept(t);
            return this;
        }

        public DoubleStream build() {
            return this.buf.toStream();
        }
    }

    // Map each element to an object, producing a Stream<U>.
    <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper);

    // Sequential, so "any" is "first".
    OptionalDouble findAny();

    // Covariant redeclarations of BaseStream's S-returning ops.
    DoubleStream sequential();

    DoubleStream parallel();

    DoubleStream unordered();

    DoubleStream onClose(Runnable closeHandler);

    void forEach(DoubleConsumer action);

    // Same as forEach for us: our streams always have an encounter order and are sequential.
    void forEachOrdered(DoubleConsumer action);

    double sum();

    double reduce(double identity, DoubleBinaryOperator op);

    // Fold with no identity: an empty stream gives an empty OptionalDouble.
    OptionalDouble reduce(DoubleBinaryOperator op);

    // Mutable reduction into a caller-supplied container.
    <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner);

    long count();

    double[] toArray();

    boolean anyMatch(DoublePredicate predicate);

    boolean allMatch(DoublePredicate predicate);

    boolean noneMatch(DoublePredicate predicate);

    OptionalDouble min();

    OptionalDouble max();

    OptionalDouble findFirst();

    OptionalDouble average();

    // Bridge to the object stream: box each double into a Double.
    Stream<Double> boxed();

    static DoubleStream of(double... values) {
        return new DoubleStreamImpl(values, values.length);
    }

    // The single-element overload; safe next to `of(double[])` because a primitive element type
    // and its array are not confusable (unlike Stream.of(T) vs Stream.of(T[])).
    static DoubleStream of(double t) {
        double[] one = new double[1];
        one[0] = t;
        return new DoubleStreamImpl(one, 1);
    }

    static Builder builder() {
        return new BuilderImpl();
    }

    static DoubleStream empty() {
        return new DoubleStreamImpl(new double[0], 0);
    }

    // The finite three-arg iterate (Java 9+). The two-arg infinite overload is not implementable
    // eagerly; see Stream's class comment.
    static DoubleStream iterate(double seed, DoublePredicate hasNext, DoubleUnaryOperator next) {
        double[] out = new double[16];
        int n = 0;
        double cur = seed;
        while (hasNext.test(cur)) {
            if (n == out.length) {
                double[] bigger = new double[out.length * 2];
                for (int k = 0; k < n; k++) {
                    bigger[k] = out[k];
                }
                out = bigger;
            }
            out[n] = cur;
            n = n + 1;
            cur = next.applyAsDouble(cur);
        }
        return new DoubleStreamImpl(out, n);
    }

    // Concatenation: every element of `a`, then every element of `b`.
    static DoubleStream concat(DoubleStream a, DoubleStream b) {
        double[] left = a.toArray();
        double[] right = b.toArray();
        double[] out = new double[left.length + right.length];
        for (int i = 0; i < left.length; i++) {
            out[i] = left[i];
        }
        for (int i = 0; i < right.length; i++) {
            out[left.length + i] = right[i];
        }
        return new DoubleStreamImpl(out, out.length);
    }
}

final class DoubleStreamImpl implements DoubleStream {

    private final double[] data;
    private final int size;

    // Close handlers, shared by reference with every stream derived from this one,
    // so onClose()/close() on any stage of a pipeline sees the same list (as in the JDK).
    private final ArrayList<Runnable> closeHandlers;

    DoubleStreamImpl(double[] data, int size) {
        this.data = data;
        this.size = size;
        this.closeHandlers = new ArrayList<Runnable>();
    }

    DoubleStreamImpl(double[] data, int size, ArrayList<Runnable> closeHandlers) {
        this.data = data;
        this.size = size;
        this.closeHandlers = closeHandlers;
    }

    public DoubleStream filter(DoublePredicate predicate) {
        double[] out = new double[this.size];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            if (predicate.test(this.data[i])) {
                out[n] = this.data[i];
                n = n + 1;
            }
        }
        return new DoubleStreamImpl(out, n, this.closeHandlers);
    }

    public DoubleStream map(DoubleUnaryOperator mapper) {
        double[] out = new double[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsDouble(this.data[i]);
        }
        return new DoubleStreamImpl(out, this.size, this.closeHandlers);
    }

    // The sub-stream goes into a local rather than chaining `mapper.apply(v).toArray()`:
    // chaining onto the result of a call into a classpath type mis-compiles (defect notes).
    public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        double[] out = new double[16];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            DoubleStream sub = mapper.apply(this.data[i]);
            if (sub == null) {
                continue;
            }
            double[] arr = sub.toArray();
            for (int j = 0; j < arr.length; j++) {
                if (n == out.length) {
                    double[] bigger = new double[out.length * 2];
                    for (int k = 0; k < n; k++) {
                        bigger[k] = out[k];
                    }
                    out = bigger;
                }
                out[n] = arr[j];
                n = n + 1;
            }
        }
        return new DoubleStreamImpl(out, n, this.closeHandlers);
    }

    public IntStream mapToInt(DoubleToIntFunction mapper) {
        int[] out = new int[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsInt(this.data[i]);
        }
        return IntStream.of(out);
    }

    public LongStream mapToLong(DoubleToLongFunction mapper) {
        long[] out = new long[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsLong(this.data[i]);
        }
        return LongStream.of(out);
    }

    public DoubleStream distinct() {
        double[] out = new double[this.size];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            double v = this.data[i];
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
        return new DoubleStreamImpl(out, n, this.closeHandlers);
    }

    public DoubleStream sorted() {
        double[] out = new double[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = this.data[i];
        }
        for (int i = 1; i < this.size; i++) {
            double key = out[i];
            int j = i - 1;
            while (j >= 0 && out[j] > key) {
                out[j + 1] = out[j];
                j = j - 1;
            }
            out[j + 1] = key;
        }
        return new DoubleStreamImpl(out, this.size, this.closeHandlers);
    }

    public DoubleStream limit(long maxSize) {
        int n = this.size;
        if (maxSize < n) {
            n = (int) maxSize;
        }
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            out[i] = this.data[i];
        }
        return new DoubleStreamImpl(out, n, this.closeHandlers);
    }

    public DoubleStream skip(long n) {
        int start;
        if (n > this.size) {
            start = this.size;
        } else {
            start = (int) n;
        }
        int len = this.size - start;
        double[] out = new double[len];
        for (int i = 0; i < len; i++) {
            out[i] = this.data[start + i];
        }
        return new DoubleStreamImpl(out, len, this.closeHandlers);
    }

    public DoubleStream peek(DoubleConsumer action) {
        for (int i = 0; i < this.size; i++) {
            action.accept(this.data[i]);
        }
        return this;
    }

    public void forEach(DoubleConsumer action) {
        for (int i = 0; i < this.size; i++) {
            action.accept(this.data[i]);
        }
    }

    public double sum() {
        double s = 0.0;
        for (int i = 0; i < this.size; i++) {
            s = s + this.data[i];
        }
        return s;
    }

    public double reduce(double identity, DoubleBinaryOperator op) {
        double acc = identity;
        for (int i = 0; i < this.size; i++) {
            acc = op.applyAsDouble(acc, this.data[i]);
        }
        return acc;
    }

    public void forEachOrdered(DoubleConsumer action) {
        for (int i = 0; i < this.size; i++) {
            action.accept(this.data[i]);
        }
    }

    public OptionalDouble reduce(DoubleBinaryOperator op) {
        if (this.size == 0) {
            return OptionalDouble.empty();
        }
        double acc = this.data[0];
        for (int i = 1; i < this.size; i++) {
            acc = op.applyAsDouble(acc, this.data[i]);
        }
        return OptionalDouble.of(acc);
    }

    // The combiner is never invoked: our collect is sequential and never splits.
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
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

    public double[] toArray() {
        double[] out = new double[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = this.data[i];
        }
        return out;
    }

    public boolean anyMatch(DoublePredicate predicate) {
        for (int i = 0; i < this.size; i++) {
            if (predicate.test(this.data[i])) {
                return true;
            }
        }
        return false;
    }

    public boolean allMatch(DoublePredicate predicate) {
        for (int i = 0; i < this.size; i++) {
            if (!predicate.test(this.data[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean noneMatch(DoublePredicate predicate) {
        for (int i = 0; i < this.size; i++) {
            if (predicate.test(this.data[i])) {
                return false;
            }
        }
        return true;
    }

    public OptionalDouble min() {
        if (this.size == 0) {
            return OptionalDouble.empty();
        }
        double m = this.data[0];
        for (int i = 1; i < this.size; i++) {
            if (this.data[i] < m) {
                m = this.data[i];
            }
        }
        return OptionalDouble.of(m);
    }

    public OptionalDouble max() {
        if (this.size == 0) {
            return OptionalDouble.empty();
        }
        double m = this.data[0];
        for (int i = 1; i < this.size; i++) {
            if (this.data[i] > m) {
                m = this.data[i];
            }
        }
        return OptionalDouble.of(m);
    }

    public OptionalDouble findFirst() {
        if (this.size == 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(this.data[0]);
    }

    public OptionalDouble average() {
        if (this.size == 0) {
            return OptionalDouble.empty();
        }
        double s = 0.0;
        for (int i = 0; i < this.size; i++) {
            s = s + this.data[i];
        }
        return OptionalDouble.of(s / (double) this.size);
    }

    public Stream<Double> boxed() {
        Double[] out = new Double[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = Double.valueOf(this.data[i]);
        }
        return Stream.<Double>of(out);
    }

    public DoubleStream takeWhile(DoublePredicate predicate) {
        int n = 0;
        while (n < this.size && predicate.test(this.data[n])) {
            n = n + 1;
        }
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            out[i] = this.data[i];
        }
        return new DoubleStreamImpl(out, n, this.closeHandlers);
    }

    public DoubleStream dropWhile(DoublePredicate predicate) {
        int start = 0;
        while (start < this.size && predicate.test(this.data[start])) {
            start = start + 1;
        }
        int len = this.size - start;
        double[] out = new double[len];
        for (int i = 0; i < len; i++) {
            out[i] = this.data[start + i];
        }
        return new DoubleStreamImpl(out, len, this.closeHandlers);
    }

    public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        Object[] out = new Object[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.apply(this.data[i]);
        }
        return new StreamImpl<U>(out, this.size);
    }

    public OptionalDouble findAny() {
        return this.findFirst();
    }

    // ---- BaseStream ------------------------------------------------------------------

    public Iterator<Double> iterator() {
        double[] copy = new double[this.size];
        for (int i = 0; i < this.size; i++) {
            copy[i] = this.data[i];
        }
        return new DoubleStreamItr(copy);
    }

    // Always sequential: we have no fork/join substrate.
    public boolean isParallel() {
        return false;
    }

    public DoubleStream sequential() {
        return this;
    }

    public DoubleStream parallel() {
        return this;
    }

    public DoubleStream unordered() {
        return this;
    }

    public DoubleStream onClose(Runnable closeHandler) {
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
     * <p>Directly over the backing double[], which is a private snapshot: the split is a pure
     * index range, with no copying and no iterator in between. ORDERED because a stream has an
     * encounter order; SIZED and SUBSIZED come from the array-backed spliterator itself.
     */
    public Spliterator<Double> spliterator() {
        return Spliterators.spliterator(this.data, 0, this.size, Spliterator.ORDERED);
    }

}

// The Iterator handed out by DoubleStreamImpl.iterator(): boxes each double on demand.
final class DoubleStreamItr implements Iterator<Double> {

    private final double[] data;
    private int cursor;

    DoubleStreamItr(double[] data) {
        this.data = data;
        this.cursor = 0;
    }

    public boolean hasNext() {
        return this.cursor < this.data.length;
    }

    public Double next() {
        double v = this.data[this.cursor];
        this.cursor = this.cursor + 1;
        return Double.valueOf(v);
    }
}

// The DoubleConsumer that mapMulti pushes into: a growable double[] that becomes the result stream.
final class DoubleBuf implements DoubleConsumer {

    private double[] data;
    private int size;

    DoubleBuf() {
        this.data = new double[16];
        this.size = 0;
    }

    public void accept(double v) {
        if (this.size == this.data.length) {
            double[] bigger = new double[this.data.length * 2];
            for (int i = 0; i < this.size; i++) {
                bigger[i] = this.data[i];
            }
            this.data = bigger;
        }
        this.data[this.size] = v;
        this.size = this.size + 1;
    }

    DoubleStream toStream() {
        return new DoubleStreamImpl(this.data, this.size);
    }
}
