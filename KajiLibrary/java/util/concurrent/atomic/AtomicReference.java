package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;

// An object reference updated atomically. The compare-and-set uses reference
// identity (`==`), like the JDK. The monitor guards the read-modify-write path so
// a witnessed value cannot change under a cooperative thread switch.
public class AtomicReference<V> implements Serializable {

    private volatile V value;

    public AtomicReference(V initialValue) {
        value = initialValue;
    }

    public AtomicReference() {
    }

    public final V get() {
        return value;
    }

    public final void set(V newValue) {
        value = newValue;
    }

    public final void lazySet(V newValue) {
        value = newValue;
    }

    public final synchronized boolean compareAndSet(V expectedValue, V newValue) {
        if (value == expectedValue) {
            value = newValue;
            return true;
        }
        return false;
    }

    public final boolean weakCompareAndSet(V expectedValue, V newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetPlain(V expectedValue, V newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final synchronized V getAndSet(V newValue) {
        V prev = value;
        value = newValue;
        return prev;
    }

    public final synchronized V getAndUpdate(UnaryOperator<V> updateFunction) {
        V prev = value;
        value = updateFunction.apply(prev);
        return prev;
    }

    public final synchronized V updateAndGet(UnaryOperator<V> updateFunction) {
        value = updateFunction.apply(value);
        return value;
    }

    public final synchronized V getAndAccumulate(V x, BinaryOperator<V> accumulatorFunction) {
        V prev = value;
        value = accumulatorFunction.apply(prev, x);
        return prev;
    }

    public final synchronized V accumulateAndGet(V x, BinaryOperator<V> accumulatorFunction) {
        value = accumulatorFunction.apply(value, x);
        return value;
    }

    public String toString() {
        V v = get();
        return v == null ? "null" : v.toString();
    }

    public final V getPlain() {
        return value;
    }

    public final void setPlain(V newValue) {
        value = newValue;
    }

    public final V getOpaque() {
        return value;
    }

    public final void setOpaque(V newValue) {
        value = newValue;
    }

    public final V getAcquire() {
        return value;
    }

    public final void setRelease(V newValue) {
        value = newValue;
    }

    public final synchronized V compareAndExchange(V expectedValue, V newValue) {
        V witness = value;
        if (witness == expectedValue) {
            value = newValue;
        }
        return witness;
    }

    public final V compareAndExchangeAcquire(V expectedValue, V newValue) {
        return compareAndExchange(expectedValue, newValue);
    }

    public final V compareAndExchangeRelease(V expectedValue, V newValue) {
        return compareAndExchange(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetVolatile(V expectedValue, V newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetAcquire(V expectedValue, V newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetRelease(V expectedValue, V newValue) {
        return compareAndSet(expectedValue, newValue);
    }
}
