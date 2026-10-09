package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.LongUnaryOperator;
import java.util.function.LongBinaryOperator;

// A {@code long[]} whose elements may be updated atomically. See
// {@link AtomicIntegerArray}; the long element is a single {@code lastore}/{@code laload}.
public class AtomicLongArray implements Serializable {

    private final long[] array;

    public AtomicLongArray(int length) {
        array = new long[length];
    }

    public AtomicLongArray(long[] array) {
        int n = array.length;
        long[] copy = new long[n];
        for (int i = 0; i < n; i++) {
            copy[i] = array[i];
        }
        this.array = copy;
    }

    public final int length() {
        return array.length;
    }

    public final long get(int i) {
        return array[i];
    }

    public final void set(int i, long newValue) {
        array[i] = newValue;
    }

    public final void lazySet(int i, long newValue) {
        array[i] = newValue;
    }

    public final synchronized long getAndSet(int i, long newValue) {
        long prev = array[i];
        array[i] = newValue;
        return prev;
    }

    public final synchronized boolean compareAndSet(int i, long expectedValue, long newValue) {
        if (array[i] == expectedValue) {
            array[i] = newValue;
            return true;
        }
        return false;
    }

    public final boolean weakCompareAndSet(int i, long expectedValue, long newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetPlain(int i, long expectedValue, long newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }

    public final synchronized long getAndIncrement(int i) {
        return array[i]++;
    }

    public final synchronized long getAndDecrement(int i) {
        return array[i]--;
    }

    public final synchronized long getAndAdd(int i, long delta) {
        long prev = array[i];
        array[i] = prev + delta;
        return prev;
    }

    public final synchronized long incrementAndGet(int i) {
        return ++array[i];
    }

    public final synchronized long decrementAndGet(int i) {
        return --array[i];
    }

    public final synchronized long addAndGet(int i, long delta) {
        array[i] = array[i] + delta;
        return array[i];
    }

    public final synchronized long getAndUpdate(int i, LongUnaryOperator updateFunction) {
        long prev = array[i];
        array[i] = updateFunction.applyAsLong(prev);
        return prev;
    }

    public final synchronized long updateAndGet(int i, LongUnaryOperator updateFunction) {
        array[i] = updateFunction.applyAsLong(array[i]);
        return array[i];
    }

    public final synchronized long getAndAccumulate(int i, long x, LongBinaryOperator accumulatorFunction) {
        long prev = array[i];
        array[i] = accumulatorFunction.applyAsLong(prev, x);
        return prev;
    }

    public final synchronized long accumulateAndGet(int i, long x, LongBinaryOperator accumulatorFunction) {
        array[i] = accumulatorFunction.applyAsLong(array[i], x);
        return array[i];
    }

    public String toString() {
        int n = array.length;
        if (n == 0) {
            return "[]";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                b.append(',');
                b.append(' ');
            }
            b.append(Long.toString(array[i]));
        }
        b.append(']');
        return b.toString();
    }

    public final long getPlain(int i) {
        return array[i];
    }

    public final void setPlain(int i, long newValue) {
        array[i] = newValue;
    }

    public final long getOpaque(int i) {
        return array[i];
    }

    public final void setOpaque(int i, long newValue) {
        array[i] = newValue;
    }

    public final long getAcquire(int i) {
        return array[i];
    }

    public final void setRelease(int i, long newValue) {
        array[i] = newValue;
    }

    public final synchronized long compareAndExchange(int i, long expectedValue, long newValue) {
        long witness = array[i];
        if (witness == expectedValue) {
            array[i] = newValue;
        }
        return witness;
    }

    public final long compareAndExchangeAcquire(int i, long expectedValue, long newValue) {
        return compareAndExchange(i, expectedValue, newValue);
    }

    public final long compareAndExchangeRelease(int i, long expectedValue, long newValue) {
        return compareAndExchange(i, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetVolatile(int i, long expectedValue, long newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetAcquire(int i, long expectedValue, long newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetRelease(int i, long expectedValue, long newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }
}
