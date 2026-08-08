package java.util.stream;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.BinaryOperator;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.ToDoubleFunction;

// KajiLibrary's java.util.stream.Stream<T> — a sequence of reference elements supporting a
// functional pipeline: intermediate ops (filter/map/distinct/sorted/limit/skip/peek) return a
// new Stream, terminal ops (forEach/count/toArray/reduce/collect/toList/min/max/…) consume it.
//
// EAGER (each intermediate op materialises a fresh Object[] backing; see StreamImpl). Correct for
// finite streams; the lazy Spliterator model is a later tier. A KajiLibrary subset (the JDK adds
// flatMap/mapToInt/iterate/generate/…). `StreamImpl` is a same-file top-level class so it can be
// referenced by the factories without cross-file resolution.
public interface Stream<T> {

    Stream<T> filter(Predicate<? super T> predicate);

    <R> Stream<R> map(Function<? super T, ? extends R> mapper);

    <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

    IntStream mapToInt(ToIntFunction<? super T> mapper);

    LongStream mapToLong(ToLongFunction<? super T> mapper);

    DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);

    Stream<T> distinct();

    Stream<T> sorted();

    Stream<T> sorted(Comparator<? super T> comparator);

    Stream<T> limit(long maxSize);

    Stream<T> skip(long n);

    Stream<T> peek(Consumer<? super T> action);

    // (sequential/parallel/isParallel are declared in BaseStream in the JDK, not in Stream, so
    // they're omitted here rather than modelling BaseStream's self-referential `S extends
    // BaseStream<T,S>` generic. Our streams are always sequential anyway.)

    void forEach(Consumer<? super T> action);

    long count();

    Object[] toArray();

    T reduce(T identity, BinaryOperator<T> accumulator);

    Optional<T> reduce(BinaryOperator<T> accumulator);

    boolean anyMatch(Predicate<? super T> predicate);

    boolean allMatch(Predicate<? super T> predicate);

    boolean noneMatch(Predicate<? super T> predicate);

    Optional<T> findFirst();

    Optional<T> min(Comparator<? super T> comparator);

    Optional<T> max(Comparator<? super T> comparator);

    // collect(Collector) is deferred: `<R,A> R collect(...)` has a bare method-type-variable
    // return, and the frozen javac's override check doesn't unify it ("R no es un subtipo de R" —
    // finding #17; #9 fixed class type variables but not method ones). `Collectors` is ready for
    // when #17 lands.

    List<T> toList();

    static <T> Stream<T> of(T... values) {
        return new StreamImpl<T>(values, values.length);
    }

    static <T> Stream<T> empty() {
        return new StreamImpl<T>(new Object[0], 0);
    }
}

// The eager implementation: a backing Object[] (elements erased) and its live length.
final class StreamImpl<T> implements Stream<T> {

    private final Object[] data;
    private final int size;

    StreamImpl(Object[] data, int size) {
        this.data = data;
        this.size = size;
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
        return new StreamImpl<T>(out, n);
    }

    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        Object[] out = new Object[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.apply((T) this.data[i]);
        }
        return new StreamImpl<R>(out, this.size);
    }

    // Map each element to a sub-stream and concatenate them (eager: drain each sub-stream via
    // toArray). `mapper.apply(...).toArray()` is chained to avoid a wildcard-capture local.
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        Object[] out = new Object[16];
        int n = 0;
        for (int i = 0; i < this.size; i++) {
            Object[] arr = mapper.apply((T) this.data[i]).toArray();
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
        return new StreamImpl<R>(out, n);
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
        return new StreamImpl<T>(out, n);
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
        return new StreamImpl<T>(out, this.size);
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
        return new StreamImpl<T>(out, this.size);
    }

    public Stream<T> limit(long maxSize) {
        int n = this.size;
        if (maxSize < n) {
            n = (int) maxSize;
        }
        return new StreamImpl<T>(this.slice(0, n), n);
    }

    public Stream<T> skip(long n) {
        int start;
        if (n > this.size) {
            start = this.size;
        } else {
            start = (int) n;
        }
        int len = this.size - start;
        return new StreamImpl<T>(this.slice(start, len), len);
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
        return this.size;
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

    public List<T> toList() {
        ArrayList<T> list = new ArrayList<T>();
        for (int i = 0; i < this.size; i++) {
            list.add((T) this.data[i]);
        }
        return list;
    }

    private Object[] slice(int start, int len) {
        Object[] out = new Object[len];
        for (int i = 0; i < len; i++) {
            out[i] = this.data[start + i];
        }
        return out;
    }
}
