package java.util.stream;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.DoubleConsumer;

// KajiLibrary's java.util.stream.Stream<T> — a sequence of reference elements supporting a
// functional pipeline: intermediate ops (filter/map/distinct/sorted/limit/skip/peek) return a
// new Stream, terminal ops (forEach/count/toArray/reduce/collect/toList/min/max/…) consume it.
//
// EAGER (each intermediate op materialises a fresh Object[] backing; see StreamImpl). Correct for
// finite streams; the lazy Spliterator model is a later tier. `StreamImpl` is a same-file
// top-level class so it can be referenced by the factories without cross-file resolution.
//
// Now rooted in BaseStream<T, Stream<T>>, which contributes iterator/isParallel/sequential/
// parallel/unordered/onClose/close. The four `S`-returning ops are redeclared below with
// Stream<T> as the return type — a covariant override, matching the JDK exactly.
//
// Still a KajiLibrary subset. Deliberately absent, each for a stated reason:
//
//   * `generate(Supplier)` and the two-arg `iterate(T, UnaryOperator)` build INFINITE streams.
//     Our model is eager — every op materialises its whole backing array — so those two cannot
//     be implemented without hanging. Declaring them and throwing would be worse than not
//     declaring them. The three-arg `iterate(seed, hasNext, next)` (Java 9+) IS finite and is
//     implemented below.
//   * `of(T)` (the single-element overload) is omitted on purpose: this javac does not prefer
//     `of(T[])` over `of(T)` for an array argument (it picks `of(T)` with T = the array type),
//     so adding it would silently break every existing `Stream.of(someArray)` call. See the
//     compiler-defect notes for this session.
//   * `gather(Gatherer)` needs java.util.stream.Gatherer (Java 22+), not ported.
//   * `spliterator()` — java.util.Spliterator does not exist; see BaseStream.
//
// Two things ARE declared here but cannot be *used* yet, and the reason is the compiler, not the
// implementation. Both are documented at their declaration and have a repro in the defect notes:
//
//   * `builder()` and the nested `Stream.Builder<T>`. The nested interface compiles and resolves
//     as a type from other files, but every method CALL on a nested interface loaded from the
//     classpath is silently dropped by this javac: `b.add("x")` emits `ldc "x"; pop` and
//     `b.build()` emits nothing at all, so the caller ends up holding the builder itself and
//     blows up with "operand stack underflow". Nothing in this package calls it, so the damage is
//     confined to user code that tries;
//   * `mapMulti(BiConsumer<? super T, ? super Consumer<R>>)`, for a different reason: a call
//     whose inference has to reach a type variable nested inside a type argument (`Consumer<R>`)
//     is also silently dropped. The three primitive variants — `mapMultiToInt`, `mapMultiToLong`,
//     `mapMultiToDouble` — nest no type variable and work; they are tested.
public interface Stream<T> extends BaseStream<T, Stream<T>> {

    Stream<T> filter(Predicate<? super T> predicate);

    <R> Stream<R> map(Function<? super T, ? extends R> mapper);

    <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

    IntStream mapToInt(ToIntFunction<? super T> mapper);

    LongStream mapToLong(ToLongFunction<? super T> mapper);

    DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);

    IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper);

    LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper);

    DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper);

    Stream<T> distinct();

    Stream<T> sorted();

    Stream<T> sorted(Comparator<? super T> comparator);

    Stream<T> limit(long maxSize);

    Stream<T> skip(long n);

    Stream<T> peek(Consumer<? super T> action);

    // Prefix of the stream up to (excluding) the first element that fails `predicate`.
    // `default`, as in the JDK: the body only needs toArray(). StreamImpl still overrides it to
    // walk its own backing array instead of a copy.
    default Stream<T> takeWhile(Predicate<? super T> predicate) {
        Object[] a = this.toArray();
        int n = 0;
        while (n < a.length && predicate.test((T) a[n])) {
            n = n + 1;
        }
        Object[] out = new Object[n];
        for (int i = 0; i < n; i++) {
            out[i] = a[i];
        }
        return new StreamImpl<T>(out, n);
    }

    // The remainder after that same prefix.
    default Stream<T> dropWhile(Predicate<? super T> predicate) {
        Object[] a = this.toArray();
        int start = 0;
        while (start < a.length && predicate.test((T) a[start])) {
            start = start + 1;
        }
        int len = a.length - start;
        Object[] out = new Object[len];
        for (int i = 0; i < len; i++) {
            out[i] = a[start + i];
        }
        return new StreamImpl<T>(out, len);
    }

    // Replace each element with zero or more elements, pushed into the Consumer the mapper is
    // handed. `default`, as in the JDK (Java 16+). The `? super` on the sink parameter is the
    // JDK's own and is load-bearing here: with an invariant `BiConsumer<T, Consumer<R>>` this
    // javac silently drops the *call* at every call site (same defect that hit Collectors'
    // flatMapping; repro in the notes).
    default <R> Stream<R> mapMulti(BiConsumer<? super T, ? super Consumer<R>> mapper) {
        Object[] a = this.toArray();
        ObjSink<R> sink = new ObjSink<R>();
        for (int i = 0; i < a.length; i++) {
            mapper.accept((T) a[i], sink);
        }
        return sink.toStream();
    }

    default IntStream mapMultiToInt(BiConsumer<? super T, ? super IntConsumer> mapper) {
        Object[] a = this.toArray();
        IntSink sink = new IntSink();
        for (int i = 0; i < a.length; i++) {
            mapper.accept((T) a[i], sink);
        }
        return sink.toStream();
    }

    default LongStream mapMultiToLong(BiConsumer<? super T, ? super LongConsumer> mapper) {
        Object[] a = this.toArray();
        LongSink sink = new LongSink();
        for (int i = 0; i < a.length; i++) {
            mapper.accept((T) a[i], sink);
        }
        return sink.toStream();
    }

    default DoubleStream mapMultiToDouble(BiConsumer<? super T, ? super DoubleConsumer> mapper) {
        Object[] a = this.toArray();
        DoubleSink sink = new DoubleSink();
        for (int i = 0; i < a.length; i++) {
            mapper.accept((T) a[i], sink);
        }
        return sink.toStream();
    }

    // A mutable builder for a Stream. Nested in Stream exactly as in the JDK; `add` is declared
    // abstract here rather than `default` (a one-line implementation either way).
    interface Builder<T> extends Consumer<T> {

        void accept(T t);

        Builder<T> add(T t);

        Stream<T> build();
    }

    // The implementation is nested INSIDE Stream, unlike every other helper in this file, because
    // a nested type of an interface cannot be named from a sibling top-level class once the file
    // is in a named package: neither `Stream.Builder` nor a bare `Builder` resolves, while the
    // identical code in the default package compiles. Repro in the defect notes. The JDK keeps
    // its equivalent in java.util.stream.Streams.
    final class BuilderImpl<T> implements Builder<T> {

        private final ObjSink<T> sink;

        BuilderImpl() {
            this.sink = new ObjSink<T>();
        }

        public void accept(T t) {
            this.sink.accept(t);
        }

        public Builder<T> add(T t) {
            this.sink.accept(t);
            return this;
        }

        public Stream<T> build() {
            return this.sink.toStream();
        }
    }

    // Covariant redeclarations of BaseStream's S-returning ops (JDK does the same).
    Stream<T> sequential();

    Stream<T> parallel();

    Stream<T> unordered();

    Stream<T> onClose(Runnable closeHandler);

    void forEach(Consumer<? super T> action);

    // Same as forEach for us: our streams always have an encounter order and are sequential.
    void forEachOrdered(Consumer<? super T> action);

    long count();

    Object[] toArray();

    // NOTE: `<A> A[] toArray(IntFunction<A[]> generator)` is NOT declared. It compiles, but it
    // does not work on our VM: a call whose static return type is a type-variable array (`A[]`)
    // does not reach the generator's `apply`, and the caller gets a zero-length array instead of
    // the elements — silently, with no exception. Repro in the defect notes; re-add the method
    // once that is fixed, the implementation is a four-liner.

    T reduce(T identity, BinaryOperator<T> accumulator);

    Optional<T> reduce(BinaryOperator<T> accumulator);

    <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner);

    // Mutable reduction into a caller-supplied container.
    <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);

    // Reduction driven by a Collector (see Collectors for the ready-made ones).
    <R, A> R collect(Collector<? super T, A, R> collector);

    boolean anyMatch(Predicate<? super T> predicate);

    boolean allMatch(Predicate<? super T> predicate);

    boolean noneMatch(Predicate<? super T> predicate);

    Optional<T> findFirst();

    // Sequential, so "any" is "first".
    Optional<T> findAny();

    Optional<T> min(Comparator<? super T> comparator);

    Optional<T> max(Comparator<? super T> comparator);

    // (collect(Collector) used to be deferred here on finding #17 — "bare method-type-variable
    // return breaks the override check". That is FIXED in the frozen javac: `<R,A> R collect(...)`
    // now overrides cleanly. It is declared above and implemented below.)

    // An unmodifiable List in the JDK; ours is a plain ArrayList, because KajiLibrary's
    // java.util.Collections has no unmodifiable* wrapper. `default`, as in the JDK.
    default List<T> toList() {
        Object[] a = this.toArray();
        ArrayList<T> out = new ArrayList<T>();
        for (int i = 0; i < a.length; i++) {
            out.add((T) a[i]);
        }
        return out;
    }

    // NOTE: `T...` compiles but this javac does not set ACC_VARARGS (finding #200), so callers
    // must pass an actual array: Stream.of(new String[] { "a", "b" }).
    static <T> Stream<T> of(T... values) {
        return new StreamImpl<T>(values, values.length);
    }

    static <T> Builder<T> builder() {
        return new BuilderImpl<T>();
    }

    static <T> Stream<T> empty() {
        return new StreamImpl<T>(new Object[0], 0);
    }

    // A one-element stream, or an empty one if the value is null.
    static <T> Stream<T> ofNullable(T value) {
        if (value == null) {
            return new StreamImpl<T>(new Object[0], 0);
        }
        Object[] one = new Object[1];
        one[0] = value;
        return new StreamImpl<T>(one, 1);
    }

    // The finite three-arg iterate (Java 9+): seed, seed', seed''… while `hasNext` holds.
    // The two-arg infinite overload is not implementable eagerly; see the class comment.
    static <T> Stream<T> iterate(T seed, Predicate<? super T> hasNext, UnaryOperator<T> next) {
        // `next` widened to Function so `apply` resolves: UnaryOperator inherits it from a
        // generic superinterface, which this javac does not look through (finding #15).
        Function<T, T> step = next;
        Object[] out = new Object[16];
        int n = 0;
        T cur = seed;
        while (hasNext.test(cur)) {
            if (n == out.length) {
                Object[] bigger = new Object[out.length * 2];
                for (int k = 0; k < n; k++) {
                    bigger[k] = out[k];
                }
                out = bigger;
            }
            out[n] = cur;
            n = n + 1;
            cur = step.apply(cur);
        }
        return new StreamImpl<T>(out, n);
    }

    // Concatenation: every element of `a`, then every element of `b`.
    static <T> Stream<T> concat(Stream<? extends T> a, Stream<? extends T> b) {
        Object[] left = a.toArray();
        Object[] right = b.toArray();
        Object[] out = new Object[left.length + right.length];
        for (int i = 0; i < left.length; i++) {
            out[i] = left[i];
        }
        for (int i = 0; i < right.length; i++) {
            out[left.length + i] = right[i];
        }
        return new StreamImpl<T>(out, out.length);
    }
}

// ---- the sinks that mapMulti* and builder() push into ---------------------------------------

// A growable Object[] seen as a Consumer<R>; becomes the result stream.
final class ObjSink<R> implements Consumer<R> {

    private Object[] data;
    private int size;

    ObjSink() {
        this.data = new Object[16];
        this.size = 0;
    }

    public void accept(R v) {
        if (this.size == this.data.length) {
            Object[] bigger = new Object[this.data.length * 2];
            for (int i = 0; i < this.size; i++) {
                bigger[i] = this.data[i];
            }
            this.data = bigger;
        }
        this.data[this.size] = v;
        this.size = this.size + 1;
    }

    Stream<R> toStream() {
        return new StreamImpl<R>(this.data, this.size);
    }
}

final class IntSink implements IntConsumer {

    private int[] data;
    private int size;

    IntSink() {
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
        int[] exact = new int[this.size];
        for (int i = 0; i < this.size; i++) {
            exact[i] = this.data[i];
        }
        return IntStream.of(exact);
    }
}

final class LongSink implements LongConsumer {

    private long[] data;
    private int size;

    LongSink() {
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
        long[] exact = new long[this.size];
        for (int i = 0; i < this.size; i++) {
            exact[i] = this.data[i];
        }
        return LongStream.of(exact);
    }
}

final class DoubleSink implements DoubleConsumer {

    private double[] data;
    private int size;

    DoubleSink() {
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
        double[] exact = new double[this.size];
        for (int i = 0; i < this.size; i++) {
            exact[i] = this.data[i];
        }
        return DoubleStream.of(exact);
    }
}

// The eager implementation: a backing Object[] (elements erased) and its live length.
final class StreamImpl<T> implements Stream<T> {

    private final Object[] data;
    private final int size;

    // Close handlers. The list is SHARED with every stream derived from this one by an
    // intermediate op, which reproduces the JDK's behaviour: onClose() on any stage of a
    // pipeline registers on the shared source stage, and close() on any stage runs them all.
    private final ArrayList<Runnable> closeHandlers;

    // Source constructor: starts a fresh pipeline with its own (empty) handler list.
    StreamImpl(Object[] data, int size) {
        this.data = data;
        this.size = size;
        this.closeHandlers = new ArrayList<Runnable>();
    }

    // Derived-stage constructor: inherits the upstream handler list by reference.
    StreamImpl(Object[] data, int size, ArrayList<Runnable> closeHandlers) {
        this.data = data;
        this.size = size;
        this.closeHandlers = closeHandlers;
    }

    public Stream<T> filter(Predicate<? super T> predicate) {
        Object[] out = new Object[this.size];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            T e = (T) this.data[i];
            if (predicate.test(e)) {
                out[n] = e;
                n = n + 1;
            }
        }
        return new StreamImpl<T>(out, n, this.closeHandlers);
    }

    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        Object[] out = new Object[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.apply((T) this.data[i]);
        }
        return new StreamImpl<R>(out, this.size, this.closeHandlers);
    }

    // Map each element to a sub-stream and concatenate them (eager: drain each sub-stream via
    // toArray). The sub-stream is bound to a local instead of chaining `mapper.apply(...).toArray()`:
    // chaining a call off the result of a call into a classpath type mis-compiles and blows up at
    // run time with "operand stack underflow". See the defect notes.
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        Object[] out = new Object[16];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            Stream<? extends R> sub = mapper.apply((T) this.data[i]);
            Object[] arr = sub.toArray();
            for (int j = 0; j < arr.length; j++) {
                if (n == out.length) {
                    Object[] bigger = new Object[out.length * 2];
                    for (int k = 0; k < n; k++) {
                        bigger[k] = out[k];
                    }
                    out = bigger;
                }
                out[n] = arr[j];
                n = n + 1;
            }
        }
        return new StreamImpl<R>(out, n, this.closeHandlers);
    }

    // Bridges to the primitive streams: map each element to an int/long/double.
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        int[] out = new int[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsInt((T) this.data[i]);
        }
        return IntStream.of(out);
    }

    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        long[] out = new long[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsLong((T) this.data[i]);
        }
        return LongStream.of(out);
    }

    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        double[] out = new double[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsDouble((T) this.data[i]);
        }
        return DoubleStream.of(out);
    }

    public Stream<T> distinct() {
        Object[] out = new Object[this.size];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            Object v = this.data[i];
            boolean seen = false;
            for (int j = 0; j < n; j++) {
                Object w = out[j];
                if (v == null) {
                    if (w == null) {
                        seen = true;
                    }
                } else {
                    if (v.equals(w)) {
                        seen = true;
                    }
                }
            }
            if (!seen) {
                out[n] = v;
                n = n + 1;
            }
        }
        return new StreamImpl<T>(out, n, this.closeHandlers);
    }

    // Natural order (elements must be mutually Comparable — cast at run time, as the JDK does).
    public Stream<T> sorted() {
        Object[] out = this.slice(0, this.size);
        for (int i = 1; i < this.size; i++) {
            Object key = out[i];
            int j = i - 1;
            while (j >= 0 && ((Comparable<T>) out[j]).compareTo((T) key) > 0) {
                out[j + 1] = out[j];
                j = j - 1;
            }
            out[j + 1] = key;
        }
        return new StreamImpl<T>(out, this.size, this.closeHandlers);
    }

    public Stream<T> sorted(Comparator<? super T> comparator) {
        Object[] out = this.slice(0, this.size);
        for (int i = 1; i < this.size; i++) {
            Object key = out[i];
            int j = i - 1;
            while (j >= 0 && comparator.compare((T) out[j], (T) key) > 0) {
                out[j + 1] = out[j];
                j = j - 1;
            }
            out[j + 1] = key;
        }
        return new StreamImpl<T>(out, this.size, this.closeHandlers);
    }

    public Stream<T> limit(long maxSize) {
        int n = this.size;
        if (maxSize < n) {
            n = (int) maxSize;
        }
        return new StreamImpl<T>(this.slice(0, n), n, this.closeHandlers);
    }

    public Stream<T> skip(long n) {
        int start;
        if (n > this.size) {
            start = this.size;
        } else {
            start = (int) n;
        }
        int len = this.size - start;
        return new StreamImpl<T>(this.slice(start, len), len, this.closeHandlers);
    }

    public Stream<T> peek(Consumer<? super T> action) {
        for (int i = 0; i < this.size; i++) {
            action.accept((T) this.data[i]);
        }
        return this;
    }

    public void forEach(Consumer<? super T> action) {
        for (int i = 0; i < this.size; i++) {
            action.accept((T) this.data[i]);
        }
    }

    public long count() {
        // Explicit widening cast: this javac does not insert the implicit int -> long
        // conversion on `return` (nor on assignment), so a bare `return this.size;` pushes an
        // int where the descriptor promises a long and the interpreter panics. See defect notes.
        return (long) this.size;
    }

    public Object[] toArray() {
        return this.slice(0, this.size);
    }

    // `op` widened to BiFunction so `apply` resolves: BinaryOperator inherits it from BiFunction,
    // and the frozen javac doesn't resolve a method inherited through a generic superinterface (#15).
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        BiFunction<T, T, T> op = accumulator;
        T acc = identity;
        for (int i = 0; i < this.size; i++) {
            acc = op.apply(acc, (T) this.data[i]);
        }
        return acc;
    }

    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        if (this.size == 0) {
            return Optional.empty();
        }
        BiFunction<T, T, T> op = accumulator;
        T acc = (T) this.data[0];
        for (int i = 1; i < this.size; i++) {
            acc = op.apply(acc, (T) this.data[i]);
        }
        return Optional.of(acc);
    }

    public boolean anyMatch(Predicate<? super T> predicate) {
        for (int i = 0; i < this.size; i++) {
            if (predicate.test((T) this.data[i])) {
                return true;
            }
        }
        return false;
    }

    public boolean allMatch(Predicate<? super T> predicate) {
        for (int i = 0; i < this.size; i++) {
            if (!predicate.test((T) this.data[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean noneMatch(Predicate<? super T> predicate) {
        for (int i = 0; i < this.size; i++) {
            if (predicate.test((T) this.data[i])) {
                return false;
            }
        }
        return true;
    }

    public Optional<T> findFirst() {
        if (this.size == 0) {
            return Optional.empty();
        }
        return Optional.of((T) this.data[0]);
    }

    public Optional<T> min(Comparator<? super T> comparator) {
        if (this.size == 0) {
            return Optional.empty();
        }
        T m = (T) this.data[0];
        for (int i = 1; i < this.size; i++) {
            T e = (T) this.data[i];
            if (comparator.compare(e, m) < 0) {
                m = e;
            }
        }
        return Optional.of(m);
    }

    public Optional<T> max(Comparator<? super T> comparator) {
        if (this.size == 0) {
            return Optional.empty();
        }
        T m = (T) this.data[0];
        for (int i = 1; i < this.size; i++) {
            T e = (T) this.data[i];
            if (comparator.compare(e, m) > 0) {
                m = e;
            }
        }
        return Optional.of(m);
    }

    public Optional<T> findAny() {
        return this.findFirst();
    }

    public List<T> toList() {
        ArrayList<T> list = new ArrayList<T>();
        for (int i = 0; i < this.size; i++) {
            list.add((T) this.data[i]);
        }
        return list;
    }

    // ---- flatMap to the primitive streams ------------------------------------------------
    // Each element maps to a whole primitive stream; drain it with toArray() and concatenate.

    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        int[] out = new int[16];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            IntStream sub = mapper.apply((T) this.data[i]);
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
        int[] exact = new int[n];
        for (int i = 0; i < n; i++) {
            exact[i] = out[i];
        }
        return IntStream.of(exact);
    }

    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        long[] out = new long[16];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            LongStream sub = mapper.apply((T) this.data[i]);
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
        long[] exact = new long[n];
        for (int i = 0; i < n; i++) {
            exact[i] = out[i];
        }
        return LongStream.of(exact);
    }

    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        double[] out = new double[16];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            DoubleStream sub = mapper.apply((T) this.data[i]);
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
        double[] exact = new double[n];
        for (int i = 0; i < n; i++) {
            exact[i] = out[i];
        }
        return DoubleStream.of(exact);
    }

    // ---- short-circuiting slices ---------------------------------------------------------

    public Stream<T> takeWhile(Predicate<? super T> predicate) {
        int n = 0;
        while (n < this.size && predicate.test((T) this.data[n])) {
            n = n + 1;
        }
        return new StreamImpl<T>(this.slice(0, n), n, this.closeHandlers);
    }

    public Stream<T> dropWhile(Predicate<? super T> predicate) {
        int start = 0;
        while (start < this.size && predicate.test((T) this.data[start])) {
            start = start + 1;
        }
        int len = this.size - start;
        return new StreamImpl<T>(this.slice(start, len), len, this.closeHandlers);
    }

    // ---- extra terminal ops --------------------------------------------------------------

    // Body inlined instead of delegating to forEach(action): this javac captures a `? super T`
    // parameter as a capture of Object and then refuses to pass it to a `? super T` parameter,
    // so even `this.forEach(action)` does not typecheck. See the defect notes (repro P12).
    public void forEachOrdered(Consumer<? super T> action) {
        for (int i = 0; i < this.size; i++) {
            action.accept((T) this.data[i]);
        }
    }

    // Three-arg reduce. `combiner` is unused: it only matters for parallel splitting, and we
    // never split. The JDK makes the same guarantee for a sequential pipeline.
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        U acc = identity;
        for (int i = 0; i < this.size; i++) {
            acc = accumulator.apply(acc, (T) this.data[i]);
        }
        return acc;
    }

    // Mutable reduction. `combiner` unused, same reason as above.
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        R container = supplier.get();
        for (int i = 0; i < this.size; i++) {
            accumulator.accept(container, (T) this.data[i]);
        }
        return container;
    }

    // Collector-driven reduction. The cast on accumulator() is a workaround: this javac cannot
    // assign the capture of `Collector<? super T, A, R>.accumulator()` to a `? super T`-typed
    // local (an `? extends` capture in the same position is fine). See the defect notes.
    // Every component is bound to an explicitly-typed local before being invoked. Chaining
    // (`collector.supplier().get()`) compiles but blows up at run time with
    // "field_offset: field not found" — see the defect notes.
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        Supplier<A> sup = collector.supplier();
        A container = sup.get();
        BiConsumer<A, T> acc = (BiConsumer<A, T>) collector.accumulator();
        for (int i = 0; i < this.size; i++) {
            acc.accept(container, (T) this.data[i]);
        }
        Function<A, R> fin = collector.finisher();
        return fin.apply(container);
    }

    // ---- BaseStream ----------------------------------------------------------------------

    public Iterator<T> iterator() {
        return new StreamItr<T>(this.slice(0, this.size));
    }

    // Always sequential: we have no fork/join substrate.
    public boolean isParallel() {
        return false;
    }

    public Stream<T> sequential() {
        return this;
    }

    // Returns a sequential stream — see BaseStream.parallel()'s note.
    public Stream<T> parallel() {
        return this;
    }

    public Stream<T> unordered() {
        return this;
    }

    public Stream<T> onClose(Runnable closeHandler) {
        this.closeHandlers.add(closeHandler);
        return this;
    }

    public void close() {
        for (int i = 0; i < this.closeHandlers.size(); i++) {
            this.closeHandlers.get(i).run();
        }
        this.closeHandlers.clear();
    }

    private Object[] slice(int start, int len) {
        Object[] out = new Object[len];
        for (int i = 0; i < len; i++) {
            out[i] = this.data[start + i];
        }
        return out;
    }
}

// The Iterator handed out by StreamImpl.iterator(). A same-file top-level class (the codebase's
// convention for helper types, e.g. ArrayListItr), over a private snapshot of the elements.
final class StreamItr<T> implements Iterator<T> {

    private final Object[] data;
    private int cursor;

    StreamItr(Object[] data) {
        this.data = data;
        this.cursor = 0;
    }

    public boolean hasNext() {
        return this.cursor < this.data.length;
    }

    public T next() {
        T e = (T) this.data[this.cursor];
        this.cursor = this.cursor + 1;
        return e;
    }
}
