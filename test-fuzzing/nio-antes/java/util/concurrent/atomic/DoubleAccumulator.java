package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.DoubleBinaryOperator;

// A running {@code double} folded through a caller-supplied associative function from a
// given identity. Single monitored cell on the single carrier (see {@link LongAccumulator}).
public class DoubleAccumulator extends Number implements Serializable {

    private final DoubleBinaryOperator function;
    private final double identity;
    private double value;

    public DoubleAccumulator(DoubleBinaryOperator accumulatorFunction, double identity) {
        this.function = accumulatorFunction;
        this.identity = identity;
        this.value = identity;
    }

    public synchronized void accumulate(double x) {
        value = function.applyAsDouble(value, x);
    }

    public synchronized double get() {
        return value;
    }

    public synchronized void reset() {
        value = identity;
    }

    public synchronized double getThenReset() {
        double prev = value;
        value = identity;
        return prev;
    }

    public String toString() {
        return Double.toString(get());
    }

    public double doubleValue() {
        return get();
    }

    public long longValue() {
        return (long) get();
    }

    public int intValue() {
        return (int) get();
    }

    public float floatValue() {
        return (float) get();
    }
}
