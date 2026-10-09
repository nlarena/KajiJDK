package java.util.concurrent.atomic;

import java.io.Serializable;

// A running {@code long} sum. The JDK spreads updates across striped cells to cut
// contention, then totals them in {@link #sum}. On KajiJDK's single carrier there is
// no contention to stripe, so one monitored field is the whole story — the observable
// contract (eventually-consistent sum, non-atomic across concurrent add/sum) is met.
// It stands in for {@code Number} directly (the JDK routes through package-private
// {@code Striped64}, which merely extends {@code Number} too).
public class LongAdder extends Number implements Serializable {

    private long value;

    public LongAdder() {
    }

    public synchronized void add(long x) {
        value += x;
    }

    public synchronized void increment() {
        value += 1L;
    }

    public synchronized void decrement() {
        value -= 1L;
    }

    public synchronized long sum() {
        return value;
    }

    public synchronized void reset() {
        value = 0L;
    }

    public synchronized long sumThenReset() {
        long prev = value;
        value = 0L;
        return prev;
    }

    public String toString() {
        return Long.toString(sum());
    }

    public long longValue() {
        return sum();
    }

    public int intValue() {
        return (int) sum();
    }

    public float floatValue() {
        return (float) sum();
    }

    public double doubleValue() {
        return (double) sum();
    }
}
