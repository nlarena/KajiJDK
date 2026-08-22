package java.util.concurrent.atomic;

// Minimal java.util.concurrent.atomic.AtomicInteger — the base of lock-free counters. The one
// true primitive is `compareAndSet` (a **native**: an atomic compare-and-set on the heap field is
// a VM operation, exactly as the JDK treats it). Everything else — `get`/`set` (a `volatile`
// field, so H4 gives them acquire/release) and the arithmetic — is a **retry loop over CAS**, the
// same shape as the real JDK's fallback and how a lock-free counter is actually written.
public class AtomicInteger {
    private volatile int value;

    public AtomicInteger(int initialValue) {
        this.value = initialValue;
    }

    public AtomicInteger() {
    }

    public final int get() {
        return value;
    }

    public final void set(int newValue) {
        this.value = newValue;
    }

    public final native boolean compareAndSet(int expectedValue, int newValue);

    public final int getAndSet(int newValue) {
        for (;;) {
            int v = get();
            if (compareAndSet(v, newValue)) return v;
        }
    }

    public final int getAndIncrement() {
        for (;;) {
            int v = get();
            if (compareAndSet(v, v + 1)) return v;
        }
    }

    public final int getAndDecrement() {
        for (;;) {
            int v = get();
            if (compareAndSet(v, v - 1)) return v;
        }
    }

    public final int getAndAdd(int delta) {
        for (;;) {
            int v = get();
            if (compareAndSet(v, v + delta)) return v;
        }
    }

    public final int incrementAndGet() {
        for (;;) {
            int v = get();
            if (compareAndSet(v, v + 1)) return v + 1;
        }
    }

    public final int decrementAndGet() {
        for (;;) {
            int v = get();
            if (compareAndSet(v, v - 1)) return v - 1;
        }
    }

    public final int addAndGet(int delta) {
        for (;;) {
            int v = get();
            if (compareAndSet(v, v + delta)) return v + delta;
        }
    }
}
