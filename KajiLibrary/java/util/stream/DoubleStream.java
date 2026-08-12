package java.util.stream;

import java.util.OptionalDouble;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;

// KajiLibrary's java.util.stream.DoubleStream — the double-specialized primitive stream, the
// mirror of IntStream/LongStream over double values. EAGER; a KajiLibrary subset. Unlike Int/Long
// there is no range() (a double range isn't well-defined). Not generic, so no #9.
public interface DoubleStream {

    DoubleStream filter(DoublePredicate predicate);

    DoubleStream map(DoubleUnaryOperator mapper);

    DoubleStream distinct();

    DoubleStream sorted();

    DoubleStream limit(long maxSize);

    DoubleStream skip(long n);

    DoubleStream peek(DoubleConsumer action);

    void forEach(DoubleConsumer action);

    double sum();

    double reduce(double identity, DoubleBinaryOperator op);

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

    static DoubleStream empty() {
        return new DoubleStreamImpl(new double[0], 0);
    }
}

final class DoubleStreamImpl implements DoubleStream {

    private final double[] data;
    private final int size;

    DoubleStreamImpl(double[] data, int size) {
        this.data = data;
        this.size = size;
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
        return new DoubleStreamImpl(out, n);
    }

    public DoubleStream map(DoubleUnaryOperator mapper) {
        double[] out = new double[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = mapper.applyAsDouble(this.data[i]);
        }
        return new DoubleStreamImpl(out, this.size);
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
        return new DoubleStreamImpl(out, n);
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
        return new DoubleStreamImpl(out, this.size);
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
        return new DoubleStreamImpl(out, n);
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
        return new DoubleStreamImpl(out, len);
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

    public long count() {
        return this.size;
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
}
