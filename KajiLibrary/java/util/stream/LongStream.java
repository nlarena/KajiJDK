package java.util.stream;

import java.util.OptionalLong;
import java.util.OptionalDouble;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;

// KajiLibrary's java.util.stream.LongStream — the long-specialized primitive stream, the mirror
// of IntStream over long values. EAGER (each intermediate op materialises a fresh long[]); a
// KajiLibrary subset. Not generic, so `implements LongStream` sidesteps #9.
public interface LongStream {

    LongStream filter(LongPredicate predicate);

    LongStream map(LongUnaryOperator mapper);

    LongStream distinct();

    LongStream sorted();

    LongStream limit(long maxSize);

    LongStream skip(long n);

    LongStream peek(LongConsumer action);

    void forEach(LongConsumer action);

    long sum();

    long reduce(long identity, LongBinaryOperator op);

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

    static LongStream empty() {
        return new LongStreamImpl(new long[0], 0);
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
}

final class LongStreamImpl implements LongStream {

    private final long[] data;
    private final int size;

    LongStreamImpl(long[] data, int size) {
        this.data = data;
        this.size = size;
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
        return new LongStreamImpl(out, n);
    }

    public LongStream map(LongUnaryOperator mapper) {
        long[] out = new long[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsLong(this.data[i]);
        }
        return new LongStreamImpl(out, this.size);
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
        return new LongStreamImpl(out, n);
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
        return new LongStreamImpl(out, this.size);
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
        return new LongStreamImpl(out, n);
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
        return new LongStreamImpl(out, len);
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

    public long count() {
        return this.size;
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
}
