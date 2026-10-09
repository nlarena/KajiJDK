package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.LongBinaryOperator;

// A running {@code long} folded through a caller-supplied associative function from a
// given identity — {@link LongAdder} generalized. Single monitored cell on the single
// carrier; the function must be side-effect-free and associative (order across threads
// is unspecified in the JDK — here it is call order).
public class LongAccumulator extends Number implements Serializable {

    private final LongBinaryOperator function;
    private final long identity;
    private long value;

    public LongAccumulator(LongBinaryOperator accumulatorFunction, long identity) {
        this.function = accumulatorFunction;
        this.identity = identity;
        this.value = identity;
    }

    public synchronized void accumulate(long x) {
        value = function.applyAsLong(value, x);
    }

    public synchronized long get() {
        return value;
    }

    public synchronized void reset() {
        value = identity;
    }

    public synchronized long getThenReset() {
        long prev = value;
        value = identity;
        return prev;
    }

    public String toString() {
        return Long.toString(get());
    }

    public long longValue() {
        return get();
    }

    public int intValue() {
        return (int) get();
    }

    public float floatValue() {
        return (float) get();
    }

    public double doubleValue() {
        return (double) get();
    }
}
