package java.lang;

import java.util.HashMap;
import java.util.function.Supplier;

// KajiLibrary's java.lang.ThreadLocal — one variable, a separate value per thread. It is the
// answer to a specific problem: an object that is not safe to share (a parser, a date format, a
// counter) but that you do not want to thread through every call as a parameter. Instead of
// locking it, you give each thread its own.
//
// The confusing part is that a ThreadLocal is not a value, it is a *key*. `new ThreadLocal<T>()`
// creates the key; the values live one-per-thread and are found by looking that key up. So a
// static field is the normal home for one — a ThreadLocal allocated per call would hand every
// caller a fresh, empty variable.
//
// STORAGE, AND HOW IT DIFFERS FROM THE JDK. The JDK hangs the table off the *Thread* (a
// package-private `threadLocals` field), keyed by ThreadLocal with weak references, so when a
// thread dies its whole table dies with it. We invert it: each ThreadLocal owns a map keyed by
// Thread. The behaviour is the same, the lifetime is not — our map keeps a strong reference to
// every Thread that ever touched this variable, so a long-lived ThreadLocal accumulates entries
// for dead threads until remove() is called. That is the same leak thread pools hit in the JDK
// (a pooled thread never dies, so its entries never go away), just easier to trigger; remove()
// in a finally block is the discipline either way.
//
// The map is guarded by a monitor because threads touch it concurrently — but note that they
// only ever contend on the table, never on the values, which is the whole point of the class.
// Single-exit style throughout (finding #105).
public class ThreadLocal<T> {

    private final Object sync = new Object();

    // Thread → this variable's value for that thread. Threads have no equals/hashCode of their
    // own, so this is identity-keyed, which is exactly what we want.
    private final HashMap<Thread, T> values = new HashMap<Thread, T>();

    public ThreadLocal() {
    }

    // The value a thread sees before it has set one. Returns null here; override it (or use
    // withInitial) to give each thread a freshly built object — the standard idiom, since
    // returning one shared object would defeat the purpose.
    protected T initialValue() {
        return null;
    }

    public T get() {
        Thread self = Thread.currentThread();
        T result;
        boolean present;
        synchronized (sync) {
            present = values.containsKey(self);
            result = values.get(self);
        }
        // initialValue() is user code and must not run under our lock — it may allocate, block,
        // or touch other ThreadLocals. Nothing else can be racing for *this* thread's entry, so
        // computing it outside the critical section is safe.
        if (!present) {
            result = initialValue();
            synchronized (sync) {
                values.put(self, result);
            }
        }
        return result;
    }

    public void set(T value) {
        Thread self = Thread.currentThread();
        synchronized (sync) {
            values.put(self, value);
        }
    }

    // Forget this thread's value; the next get() runs initialValue() again. On a pooled thread
    // this is not an optimisation but a correctness requirement: without it the next task to run
    // on that thread inherits the previous task's value.
    public void remove() {
        Thread self = Thread.currentThread();
        synchronized (sync) {
            values.remove(self);
        }
    }

    // The lambda-friendly factory: `ThreadLocal.withInitial(() -> new StringBuilder())`, instead
    // of an anonymous subclass overriding initialValue().
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<S>(supplier);
    }
}

// The subclass withInitial() returns: initialValue() forwards to the supplier. Top-level and
// package-private rather than a nested class, per finding #13.
class SuppliedThreadLocal<S> extends ThreadLocal<S> {

    private final Supplier<? extends S> supplier;

    SuppliedThreadLocal(Supplier<? extends S> supplier) {
        this.supplier = supplier;
    }

    protected S initialValue() {
        return supplier.get();
    }
}
