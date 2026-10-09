package java.util.concurrent.atomic;

import java.io.Serializable;

// A running {@code double} sum. Single monitored field on the single carrier (see
// {@link LongAdder}). Floating-point summation is order-sensitive; with no striping the
// order here is simply the call order.
public class DoubleAdder extends Number implements Serializable {

    private double value;

    public DoubleAdder() {
    }

    public synchronized void add(double x) {
        value += x;
    }

    public synchronized double sum() {
        return value;
    }

    public synchronized void reset() {
        value = 0.0;
    }

    public synchronized double sumThenReset() {
        double prev = value;
        value = 0.0;
        return prev;
    }

    public String toString() {
        return Double.toString(sum());
    }

    public double doubleValue() {
        return sum();
    }

    public long longValue() {
        return (long) sum();
    }

    public int intValue() {
        return (int) sum();
    }

    public float floatValue() {
        return (float) sum();
    }
}
