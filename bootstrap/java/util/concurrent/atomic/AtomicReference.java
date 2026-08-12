package java.util.concurrent.atomic;

// Minimal java.util.concurrent.atomic.AtomicReference — the lock-free reference cell. Its
// `compareAndSet` compares by **identity** (native); `get`/`set` are a `volatile` field.
public class AtomicReference<V> {
    private volatile V value;

    public AtomicReference(V initialValue) {
        this.value = initialValue;
    }

    public AtomicReference() {
    }

    public final V get() {
        return value;
    }

    public final void set(V newValue) {
        this.value = newValue;
    }

    public final native boolean compareAndSet(V expectedValue, V newValue);

    public final V getAndSet(V newValue) {
        for (;;) { V v = get(); if (compareAndSet(v, newValue)) return v; }
    }
}
