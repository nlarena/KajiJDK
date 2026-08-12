package java.lang;

// A variable whose value is per-thread: each thread that touches a ThreadLocal sees its own
// independent value. Implemented as an association list hanging off the *current* Thread (see
// Thread.threadLocals): get/set always operate on Thread.currentThread(), so a thread only ever
// reads or writes its own list — no sharing, no locking. Keys are compared by identity (`==`),
// which is GC-safe: if the collector moves a ThreadLocal object, it forwards every reference to
// it (including the ones stored in entries) consistently, so `==` still holds.
public class ThreadLocal<T> {
    // One (ThreadLocal -> value) binding in a thread's list. Package-private: Thread holds the head.
    static final class Entry {
        final ThreadLocal<?> key;
        Object value;
        Entry next;

        Entry(ThreadLocal<?> key, Object value, Entry next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    // The value for a thread that has never set one. Override to supply a non-null default.
    protected T initialValue() {
        return null;
    }

    // This thread's value, initializing it from initialValue() (and recording that) on first read.
    @SuppressWarnings("unchecked")
    public T get() {
        Thread t = Thread.currentThread();
        for (Entry e = t.threadLocals; e != null; e = e.next) {
            if (e.key == this) {
                return (T) e.value;
            }
        }
        T initial = initialValue();
        t.threadLocals = new Entry(this, initial, t.threadLocals);
        return initial;
    }

    // Set this thread's value, replacing any existing binding.
    public void set(T value) {
        Thread t = Thread.currentThread();
        for (Entry e = t.threadLocals; e != null; e = e.next) {
            if (e.key == this) {
                e.value = value;
                return;
            }
        }
        t.threadLocals = new Entry(this, value, t.threadLocals);
    }

    // Drop this thread's binding; a later get() will re-run initialValue().
    public void remove() {
        Thread t = Thread.currentThread();
        Entry prev = null;
        for (Entry e = t.threadLocals; e != null; e = e.next) {
            if (e.key == this) {
                if (prev == null) {
                    t.threadLocals = e.next;
                } else {
                    prev.next = e.next;
                }
                return;
            }
            prev = e;
        }
    }
}
