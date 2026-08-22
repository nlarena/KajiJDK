package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.IntUnaryOperator;
import java.util.function.IntBinaryOperator;

// An {@code int} value that may be updated atomically. On KajiJDK's single-carrier,
// cooperatively-scheduled runtime there is no true parallelism, so a plain read or
// write of the field is already atomic (one {@code getfield}/{@code putfield} = one
// interpreter step, and safepoints fall only *between* steps). The read-modify-write
// operations still take the monitor, so a compound update is never interleaved with
// another thread that woke at a tick boundary — the exact observable contract of the
// JDK's lock-free version, reached here with the tools the VM already has.
public class AtomicInteger extends Number implements Serializable {

    private volatile int value;

    public AtomicInteger(int initialValue) {
        value = initialValue;
    }

    public AtomicInteger() {
    }

    public final int get() {
        return value;
    }

    public final void set(int newValue) {
        value = newValue;
    }

    public final void lazySet(int newValue) {
        value = newValue;
    }

    public final synchronized int getAndSet(int newValue) {
        int prev = value;
        value = newValue;
        return prev;
    }

    public final synchronized boolean compareAndSet(int expectedValue, int newValue) {
        if (value == expectedValue) {
            value = newValue;
            return true;
        }
        return false;
    }

    public final boolean weakCompareAndSet(int expectedValue, int newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetPlain(int expectedValue, int newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final synchronized int getAndIncrement() {
        return value++;
    }

    public final synchronized int getAndDecrement() {
        return value--;
    }

    public final synchronized int getAndAdd(int delta) {
        int prev = value;
        value = prev + delta;
        return prev;
    }

    public final synchronized int incrementAndGet() {
        return ++value;
    }

    public final synchronized int decrementAndGet() {
        return --value;
    }

    public final synchronized int addAndGet(int delta) {
        value = value + delta;
        return value;
    }

    public final synchronized int getAndUpdate(IntUnaryOperator updateFunction) {
        int prev = value;
        value = updateFunction.applyAsInt(prev);
        return prev;
    }

    public final synchronized int updateAndGet(IntUnaryOperator updateFunction) {
        value = updateFunction.applyAsInt(value);
        return value;
    }

    public final synchronized int getAndAccumulate(int x, IntBinaryOperator accumulatorFunction) {
        int prev = value;
        value = accumulatorFunction.applyAsInt(prev, x);
        return prev;
    }

    public final synchronized int accumulateAndGet(int x, IntBinaryOperator accumulatorFunction) {
        value = accumulatorFunction.applyAsInt(value, x);
        return value;
    }

    public String toString() {
        return Integer.toString(get());
    }

    public int intValue() {
        return get();
    }

    public long longValue() {
        return (long) get();
    }

    public float floatValue() {
        return (float) get();
    }

    public double doubleValue() {
        return (double) get();
    }

    public final int getPlain() {
        return value;
    }

    public final void setPlain(int newValue) {
        value = newValue;
    }

    public final int getOpaque() {
        return value;
    }

    public final void setOpaque(int newValue) {
        value = newValue;
    }

    public final int getAcquire() {
        return value;
    }

    public final void setRelease(int newValue) {
        value = newValue;
    }

    public final synchronized int compareAndExchange(int expectedValue, int newValue) {
        int witness = value;
        if (witness == expectedValue) {
            value = newValue;
        }
        return witness;
    }

    public final int compareAndExchangeAcquire(int expectedValue, int newValue) {
        return compareAndExchange(expectedValue, newValue);
    }

    public final int compareAndExchangeRelease(int expectedValue, int newValue) {
        return compareAndExchange(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetVolatile(int expectedValue, int newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetAcquire(int expectedValue, int newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetRelease(int expectedValue, int newValue) {
        return compareAndSet(expectedValue, newValue);
    }
}
