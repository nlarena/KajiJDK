package java.util.stream;

// Cross-package imports (needed regardless; same-package finder issue is #4).
import java.util.OptionalInt;
import java.util.OptionalDouble;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;

// KajiLibrary's java.util.stream.IntStream — a sequence of primitive ints supporting a
// functional pipeline: intermediate ops (filter/map) return a new IntStream, terminal ops
// (sum/reduce/forEach/count/toArray/…/min/max/average) consume it. Not generic, so a concrete
// `implements IntStream` sidesteps the generic-override check (#9).
//
// EAGER: each intermediate op materialises a fresh backing int[] (see IntStreamImpl). Correct
// for finite streams; the lazy Spliterator model is a later tier. `boxed`/`mapToObj` (which
// need Stream<T>) and distinct/sorted/limit/skip/peek are deferred. A KajiLibrary subset.
public interface IntStream {

    IntStream filter(IntPredicate predicate);

    IntStream map(IntUnaryOperator mapper);

    IntStream distinct();

    IntStream sorted();

    IntStream limit(long maxSize);

    IntStream skip(long n);

    IntStream peek(IntConsumer action);

    void forEach(IntConsumer action);

    int sum();

    int reduce(int identity, IntBinaryOperator op);

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

    static IntStream empty() {
        return new IntStreamImpl(new int[0], 0);
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
}

// The eager implementation: a backing int[] and how many of its slots are live. Package-private
// (referenced only by IntStream's factories, in the same compilation unit — no #7).
final class IntStreamImpl implements IntStream {

    private final int[] data;
    private final int size;

    IntStreamImpl(int[] data, int size) {
        this.data = data;
        this.size = size;
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
        return new IntStreamImpl(out, n);
    }

    public IntStream map(IntUnaryOperator mapper) {
        int[] out = new int[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsInt(this.data[i]);
        }
        return new IntStreamImpl(out, this.size);
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
        return new IntStreamImpl(out, n);
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
        return new IntStreamImpl(out, this.size);
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
        return new IntStreamImpl(out, n);
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
        return new IntStreamImpl(out, len);
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

    public long count() {
        return this.size;
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
}
