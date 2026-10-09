package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.LongUnaryOperator;
import java.util.function.LongBinaryOperator;

// A {@code long} value updated atomically. See {@link AtomicInteger} for why the
// read-modify-write path takes the monitor while plain get/set need not on KajiJDK's
// single-carrier runtime — a {@code long} field is written by one {@code putfield},
// which is one interpreter step, so even a 64-bit store is never torn here.
public class AtomicLong extends Number implements Serializable {

    private volatile long value;

    public AtomicLong(long initialValue) {
        value = initialValue;
    }

    public AtomicLong() {
    }

    public final long get() {
        return value;
    }

    public final void set(long newValue) {
        value = newValue;
    }

    public final void lazySet(long newValue) {
        value = newValue;
    }

    public final synchronized long getAndSet(long newValue) {
        long prev = value;
        value = newValue;
        return prev;
    }

    public final synchronized boolean compareAndSet(long expectedValue, long newValue) {
        if (value == expectedValue) {
            value = newValue;
            return true;
        }
        return false;
    }

    public final boolean weakCompareAndSet(long expectedValue, long newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetPlain(long expectedValue, long newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final synchronized long getAndIncrement() {
        return value++;
    }

    public final synchronized long getAndDecrement() {
        return value--;
    }

    public final synchronized long getAndAdd(long delta) {
        long prev = value;
        value = prev + delta;
        return prev;
    }

    public final synchronized long incrementAndGet() {
        return ++value;
    }

    public final synchronized long decrementAndGet() {
        return --value;
    }

    public final synchronized long addAndGet(long delta) {
        value = value + delta;
        return value;
    }

    public final synchronized long getAndUpdate(LongUnaryOperator updateFunction) {
        long prev = value;
        value = updateFunction.applyAsLong(prev);
        return prev;
    }

    public final synchronized long updateAndGet(LongUnaryOperator updateFunction) {
        value = updateFunction.applyAsLong(value);
        return value;
    }

    public final synchronized long getAndAccumulate(long x, LongBinaryOperator accumulatorFunction) {
        long prev = value;
        value = accumulatorFunction.applyAsLong(prev, x);
        return prev;
    }

    public final synchronized long accumulateAndGet(long x, LongBinaryOperator accumulatorFunction) {
        value = accumulatorFunction.applyAsLong(value, x);
        return value;
    }

    public String toString() {
        return Long.toString(get());
    }

    public int intValue() {
        return (int) get();
    }

    public long longValue() {
        return get();
    }

    public float floatValue() {
        return (float) get();
    }

    public double doubleValue() {
        return (double) get();
    }

    public final long getPlain() {
        return value;
    }

    public final void setPlain(long newValue) {
        value = newValue;
    }

    public final long getOpaque() {
        return value;
    }

    public final void setOpaque(long newValue) {
        value = newValue;
    }

    public final long getAcquire() {
        return value;
    }

    public final void setRelease(long newValue) {
        value = newValue;
    }

    public final synchronized long compareAndExchange(long expectedValue, long newValue) {
        long witness = value;
        if (witness == expectedValue) {
            value = newValue;
        }
        return witness;
    }

    public final long compareAndExchangeAcquire(long expectedValue, long newValue) {
        return compareAndExchange(expectedValue, newValue);
    }

    public final long compareAndExchangeRelease(long expectedValue, long newValue) {
        return compareAndExchange(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetVolatile(long expectedValue, long newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetAcquire(long expectedValue, long newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetRelease(long expectedValue, long newValue) {
        return compareAndSet(expectedValue, newValue);
    }
}
