package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;

// An {@code Object[]} whose elements may be updated atomically, typed {@code E}.
// Compare-and-set uses reference identity, like the JDK.
public class AtomicReferenceArray<E> implements Serializable {

    private final Object[] array;

    public AtomicReferenceArray(int length) {
        array = new Object[length];
    }

    public AtomicReferenceArray(E[] array) {
        int n = array.length;
        Object[] copy = new Object[n];
        for (int i = 0; i < n; i++) {
            copy[i] = array[i];
        }
        this.array = copy;
    }

    public final int length() {
        return array.length;
    }

    public final E get(int i) {
        return (E) array[i];
    }

    public final void set(int i, E newValue) {
        array[i] = newValue;
    }

    public final void lazySet(int i, E newValue) {
        array[i] = newValue;
    }

    public final synchronized E getAndSet(int i, E newValue) {
        E prev = (E) array[i];
        array[i] = newValue;
        return prev;
    }

    public final synchronized boolean compareAndSet(int i, E expectedValue, E newValue) {
        if (array[i] == expectedValue) {
            array[i] = newValue;
            return true;
        }
        return false;
    }

    public final boolean weakCompareAndSet(int i, E expectedValue, E newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetPlain(int i, E expectedValue, E newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }

    public final synchronized E getAndUpdate(int i, UnaryOperator<E> updateFunction) {
        E prev = (E) array[i];
        array[i] = updateFunction.apply(prev);
        return prev;
    }

    public final synchronized E updateAndGet(int i, UnaryOperator<E> updateFunction) {
        E next = updateFunction.apply((E) array[i]);
        array[i] = next;
        return next;
    }

    public final synchronized E getAndAccumulate(int i, E x, BinaryOperator<E> accumulatorFunction) {
        E prev = (E) array[i];
        array[i] = accumulatorFunction.apply(prev, x);
        return prev;
    }

    public final synchronized E accumulateAndGet(int i, E x, BinaryOperator<E> accumulatorFunction) {
        E next = accumulatorFunction.apply((E) array[i], x);
        array[i] = next;
        return next;
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
            Object v = array[i];
            b.append(v == null ? "null" : v.toString());
        }
        b.append(']');
        return b.toString();
    }

    public final E getPlain(int i) {
        return (E) array[i];
    }

    public final void setPlain(int i, E newValue) {
        array[i] = newValue;
    }

    public final E getOpaque(int i) {
        return (E) array[i];
    }

    public final void setOpaque(int i, E newValue) {
        array[i] = newValue;
    }

    public final E getAcquire(int i) {
        return (E) array[i];
    }

    public final void setRelease(int i, E newValue) {
        array[i] = newValue;
    }

    public final synchronized E compareAndExchange(int i, E expectedValue, E newValue) {
        E witness = (E) array[i];
        if (witness == expectedValue) {
            array[i] = newValue;
        }
        return witness;
    }

    public final E compareAndExchangeAcquire(int i, E expectedValue, E newValue) {
        return compareAndExchange(i, expectedValue, newValue);
    }

    public final E compareAndExchangeRelease(int i, E expectedValue, E newValue) {
        return compareAndExchange(i, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetVolatile(int i, E expectedValue, E newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetAcquire(int i, E expectedValue, E newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }

    public final boolean weakCompareAndSetRelease(int i, E expectedValue, E newValue) {
        return compareAndSet(i, expectedValue, newValue);
    }
}
