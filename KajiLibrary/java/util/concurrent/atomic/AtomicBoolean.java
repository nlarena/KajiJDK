package java.util.concurrent.atomic;

import java.io.Serializable;

// A {@code boolean} value updated atomically. Backed by a single field; the
// compare-and-set path takes the monitor so a test-then-set never interleaves with
// another green thread on KajiJDK's cooperative carrier.
public class AtomicBoolean implements Serializable {

    private volatile boolean value;

    public AtomicBoolean(boolean initialValue) {
        value = initialValue;
    }

    public AtomicBoolean() {
    }

    public final boolean get() {
        return value;
    }

    public final synchronized boolean compareAndSet(boolean expectedValue, boolean newValue) {
        if (value == expectedValue) {
            value = newValue;
            return true;
        }
        return false;
    }

    public boolean weakCompareAndSet(boolean expectedValue, boolean newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public boolean weakCompareAndSetPlain(boolean expectedValue, boolean newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final void set(boolean newValue) {
        value = newValue;
    }

    public final void lazySet(boolean newValue) {
        value = newValue;
    }

    public final synchronized boolean getAndSet(boolean newValue) {
        boolean prev = value;
        value = newValue;
        return prev;
    }

    public String toString() {
        return get() ? "true" : "false";
    }

    public final boolean getPlain() {
        return value;
    }

    public final void setPlain(boolean newValue) {
        value = newValue;
    }

    public final boolean getOpaque() {
        return value;
    }

    public final void setOpaque(boolean newValue) {
        value = newValue;
    }

    public final boolean getAcquire() {
        return value;
    }

    public final void setRelease(boolean newValue) {
        value = newValue;
    }

    public final synchronized boolean compareAndExchange(boolean expectedValue, boolean newValue) {
        boolean witness = value;
        if (witness == expectedValue) {
            value = newValue;
        }
        return witness;
    }

    public final boolean compareAndExchangeAcquire(boolean expectedValue, boolean newValue) {
        return compareAndExchange(expectedValue, newValue);
    }

    public final boolean compareAndExchangeRelease(boolean expectedValue, boolean newValue) {
        return compareAndExchange(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetVolatile(boolean expectedValue, boolean newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetAcquire(boolean expectedValue, boolean newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetRelease(boolean expectedValue, boolean newValue) {
        return compareAndSet(expectedValue, newValue);
    }
}
