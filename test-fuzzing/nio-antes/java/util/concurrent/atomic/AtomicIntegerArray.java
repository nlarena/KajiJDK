package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.IntUnaryOperator;
import java.util.function.IntBinaryOperator;

// An {@code int[]} whose elements may be updated atomically. Each element is a word,
// so a plain indexed load/store is one interpreter step (atomic on the single carrier);
// the read-modify-write element operations take the monitor.
public class AtomicIntegerArray implements Serializable {

    private final int[] array;

    public AtomicIntegerArray(int length) {
        array = new int[length];
    }

    public AtomicIntegerArray(int[] array) {
        int n = array.length;
        int[] copy = new int[n];
        for (int i = 0; i < n; i++) {
            copy[i] = array[i];
        }
        this.array = copy;
    }

    public final int length() {
        return array.length;
    }

    public final int get(int i) {
        return array[i];
    }

    public final void set(int i, int newValue) {
        array[i] = newValue;
    }

    public final void lazySet(int i, int newValue) {
        array[i] = newValue;
    }

    public final synchronized int getAndSet(int i, int newValue) {
        int prev = array[i];
        array[i] = newValue;
        return prev;
    }

    public final synchronized boolean compareAndSet(int i, int expectedValue, int newValue) {
        if (array[i] == expectedValue) {
            array[i] = newValue;
            return true;
        }
        return false;
    }

    public final boolean weakCompareAndSet(int i, int expectedValue, int newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetPlain(int i, int expectedValue, int newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }

    public final synchronized int getAndIncrement(int i) {
        return array[i]++;
    }

    public final synchronized int getAndDecrement(int i) {
        return array[i]--;
    }

    public final synchronized int getAndAdd(int i, int delta) {
        int prev = array[i];
        array[i] = prev + delta;
        return prev;
    }

    public final synchronized int incrementAndGet(int i) {
        return ++array[i];
    }

    public final synchronized int decrementAndGet(int i) {
        return --array[i];
    }

    public final synchronized int addAndGet(int i, int delta) {
        array[i] = array[i] + delta;
        return array[i];
    }

    public final synchronized int getAndUpdate(int i, IntUnaryOperator updateFunction) {
        int prev = array[i];
        array[i] = updateFunction.applyAsInt(prev);
        return prev;
    }

    public final synchronized int updateAndGet(int i, IntUnaryOperator updateFunction) {
        array[i] = updateFunction.applyAsInt(array[i]);
        return array[i];
    }

    public final synchronized int getAndAccumulate(int i, int x, IntBinaryOperator accumulatorFunction) {
        int prev = array[i];
        array[i] = accumulatorFunction.applyAsInt(prev, x);
        return prev;
    }

    public final synchronized int accumulateAndGet(int i, int x, IntBinaryOperator accumulatorFunction) {
        array[i] = accumulatorFunction.applyAsInt(array[i], x);
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
            b.append(array[i]);
        }
        b.append(']');
        return b.toString();
    }

    public final int getPlain(int i) {
        return array[i];
    }

    public final void setPlain(int i, int newValue) {
        array[i] = newValue;
    }

    public final int getOpaque(int i) {
        return array[i];
    }

    public final void setOpaque(int i, int newValue) {
        array[i] = newValue;
    }

    public final int getAcquire(int i) {
        return array[i];
    }

    public final void setRelease(int i, int newValue) {
        array[i] = newValue;
    }

    public final synchronized int compareAndExchange(int i, int expectedValue, int newValue) {
        int witness = array[i];
        if (witness == expectedValue) {
            array[i] = newValue;
        }
        return witness;
    }

    public final int compareAndExchangeAcquire(int i, int expectedValue, int newValue) {
        return compareAndExchange(i, expectedValue, newValue);
    }

    public final int compareAndExchangeRelease(int i, int expectedValue, int newValue) {
        return compareAndExchange(i, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetVolatile(int i, int expectedValue, int newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetAcquire(int i, int expectedValue, int newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetRelease(int i, int expectedValue, int newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }
}
