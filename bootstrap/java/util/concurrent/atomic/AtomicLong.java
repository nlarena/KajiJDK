package java.util.concurrent.atomic;

// Minimal java.util.concurrent.atomic.AtomicLong — AtomicInteger's 64-bit twin. `compareAndSet`
// is native (an atomic 8-byte CAS); the rest is the same retry loop over it.
public class AtomicLong {
    private volatile long value;

    public AtomicLong(long initialValue) {
        this.value = initialValue;
    }

    public AtomicLong() {
    }

    public final long get() {
        return value;
    }

    public final void set(long newValue) {
        this.value = newValue;
    }

    public final native boolean compareAndSet(long expectedValue, long newValue);

    public final long getAndSet(long newValue) {
        for (;;) { long v = get(); if (compareAndSet(v, newValue)) return v; }
    }

    public final long getAndIncrement() {
        for (;;) { long v = get(); if (compareAndSet(v, v + 1)) return v; }
    }

    public final long getAndDecrement() {
        for (;;) { long v = get(); if (compareAndSet(v, v - 1)) return v; }
    }

    public final long getAndAdd(long delta) {
        for (;;) { long v = get(); if (compareAndSet(v, v + delta)) return v; }
    }

    public final long incrementAndGet() {
        for (;;) { long v = get(); if (compareAndSet(v, v + 1)) return v + 1; }
    }

    public final long decrementAndGet() {
        for (;;) { long v = get(); if (compareAndSet(v, v - 1)) return v - 1; }
    }

    public final long addAndGet(long delta) {
        for (;;) { long v = get(); if (compareAndSet(v, v + delta)) return v + delta; }
    }
}
