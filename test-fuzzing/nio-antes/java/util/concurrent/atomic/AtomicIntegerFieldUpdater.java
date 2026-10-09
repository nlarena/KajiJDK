package java.util.concurrent.atomic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

// A reflection-based utility that gives atomic updates to designated {@code volatile int} fields of
// designated classes. `newUpdater(C.class, "f")` looks the field up by name once; every later call
// reads or writes *that* field on whatever instance of {@code C} it is handed.
//
// ---- how the atomicity is obtained here, and why it is honest ------------------------------------
//
// The JDK reaches the field through `Unsafe`, which hands it a real hardware compare-and-swap at a
// field offset. KajiJDK's VM exposes reflective field access as six raw seams on
// `java.lang.reflect.Field` -- `getInt0`/`getLong0`/`getReference0` and their writes -- and there is
// **no compare-and-swap seam among them**. A read-compare-write built out of `getInt0` + `setInt0`
// is three separate interpreter steps, and safepoints fall between steps, so on its own it is not
// atomic at all. That is measured, not assumed: four threads doing 20 000 unguarded
// `setInt(o, getInt(o) + 1)` each lose about half the increments, and they lose them on all three
// threading substrates -- `green` included, since a safepoint between two steps is enough. The
// control is kept in scratchpad/zzatomic/Control.java.
//
// So the five primitives below (`get`, `set`, `lazySet`, `compareAndSet`, `weakCompareAndSet`) each
// run inside `synchronized (FieldUpdaterLock.MONITOR)`: one process-wide monitor, private to this
// package, taken by every field updater. That is mutual exclusion, which is *stronger* than CAS --
// it serialises what CAS merely makes indivisible -- so every outcome it permits is an outcome the
// JDK also permits. Taking the monitor on the plain `get`/`set` too is deliberate: the monitor's
// acquire/release is what supplies the ordering the `volatile` field promises, since the raw seam
// itself carries no barrier.
//
// The compound operations (`getAndAdd`, `updateAndGet`, `accumulateAndGet`, ...) are **not**
// locked wholesale. They are the JDK's own retry loops over `get`/`compareAndSet`, for two reasons:
// this class is public and abstract, so its concrete methods must be correct for any subclass a
// user writes on top of the five primitives; and the loop calls the caller's function outside the
// critical section, which is both what the JDK's contract says ("may be re-applied when attempted
// updates fail due to contention") and what keeps user code from ever running under our monitor.
//
// ---- what is checked, and the one check that is not -----------------------------------------------
//
// `newUpdater` reproduces the JDK's rejections exactly, in the JDK's order: a missing field raises
// `RuntimeException` wrapping the `NoSuchFieldException`; a field of the wrong type raises
// `IllegalArgumentException("Must be integer type")`; a non-`volatile` field raises
// `IllegalArgumentException("Must be volatile type")`; a `static` field raises a bare
// `IllegalArgumentException`.
//
// The check that is **missing** is the JDK's caller-access check -- that whoever called
// `newUpdater` could have named the field itself. It needs the calling class, and KajiJDK has no
// way to obtain one: `StackWalker.getCallerClass()` throws `UnsupportedOperationException` because
// the VM exposes no stack introspection. So this class accepts a `private` field of another class
// where the JDK would refuse. That only *widens* what is accepted; nothing it accepts behaves
// differently from the JDK once accepted.
public abstract class AtomicIntegerFieldUpdater<T> {

    /**
     * An updater for the {@code volatile int} field named {@code fieldName} of {@code tclass}.
     *
     * @throws IllegalArgumentException if the field is not a non-static {@code volatile int}
     * @throws RuntimeException if the field is not found
     */
    public static <U> AtomicIntegerFieldUpdater<U> newUpdater(Class<U> tclass, String fieldName) {
        return new IntUpdater<U>(tclass, fieldName);
    }

    protected AtomicIntegerFieldUpdater() {
    }

    // ---- the five primitives (each one critical section wide) ----

    public abstract boolean compareAndSet(T obj, int expectedValue, int newValue);

    public abstract boolean weakCompareAndSet(T obj, int expectedValue, int newValue);

    public abstract void set(T obj, int newValue);

    public abstract void lazySet(T obj, int newValue);

    public abstract int get(T obj);

    // ---- everything else: the JDK's retry loops over the primitives ----

    public int getAndSet(T obj, int newValue) {
        int prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, newValue));
        return prev;
    }

    public int getAndIncrement(T obj) {
        return getAndAdd(obj, 1);
    }

    public int getAndDecrement(T obj) {
        return getAndAdd(obj, -1);
    }

    public int getAndAdd(T obj, int delta) {
        int prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, prev + delta));
        return prev;
    }

    public int incrementAndGet(T obj) {
        return getAndAdd(obj, 1) + 1;
    }

    public int decrementAndGet(T obj) {
        return getAndAdd(obj, -1) - 1;
    }

    public int addAndGet(T obj, int delta) {
        return getAndAdd(obj, delta) + delta;
    }

    public final int getAndUpdate(T obj, IntUnaryOperator updateFunction) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    public final int updateAndGet(T obj, IntUnaryOperator updateFunction) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    public final int getAndAccumulate(T obj, int x, IntBinaryOperator accumulatorFunction) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    public final int accumulateAndGet(T obj, int x, IntBinaryOperator accumulatorFunction) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    // The concrete updater `newUpdater` hands back. Package-private, like the JDK's own nested impl.
    static final class IntUpdater<T> extends AtomicIntegerFieldUpdater<T> {

        private final Class<?> tclass;
        private final Field field;

        IntUpdater(Class<T> tclass, String fieldName) {
            Field f;
            try {
                f = tclass.getDeclaredField(fieldName);
            } catch (Exception ex) {
                // The JDK wraps whatever the lookup threw -- NoSuchFieldException, or the NPE from
                // a null tclass/fieldName -- in a plain RuntimeException, message and all.
                throw new RuntimeException(ex);
            }
            if (f.getType() != Integer.TYPE) {
                throw new IllegalArgumentException("Must be integer type");
            }
            int mods = f.getModifiers();
            if (!Modifier.isVolatile(mods)) {
                throw new IllegalArgumentException("Must be volatile type");
            }
            if (Modifier.isStatic(mods)) {
                // The JDK reaches this one from `Unsafe.objectFieldOffset`, with no message.
                throw new IllegalArgumentException();
            }
            f.setAccessible(true);
            this.tclass = tclass;
            this.field = f;
        }

        // The JDK throws a bare ClassCastException for an object that is not a `tclass` -- null
        // included, since `isInstance(null)` is false.
        private void accessCheck(T obj) {
            if (!this.tclass.isInstance(obj)) {
                throw new ClassCastException();
            }
        }

        public int get(T obj) {
            accessCheck(obj);
            synchronized (FieldUpdaterLock.MONITOR) {
                return this.field.getInt(obj);
            }
        }

        public void set(T obj, int newValue) {
            accessCheck(obj);
            synchronized (FieldUpdaterLock.MONITOR) {
                this.field.setInt(obj, newValue);
            }
        }

        public void lazySet(T obj, int newValue) {
            set(obj, newValue);
        }

        public boolean compareAndSet(T obj, int expectedValue, int newValue) {
            accessCheck(obj);
            synchronized (FieldUpdaterLock.MONITOR) {
                if (this.field.getInt(obj) != expectedValue) {
                    return false;
                }
                this.field.setInt(obj, newValue);
                return true;
            }
        }

        // Mutual exclusion never fails spuriously, so the weak form is the strong one.
        public boolean weakCompareAndSet(T obj, int expectedValue, int newValue) {
            return compareAndSet(obj, expectedValue, newValue);
        }
    }
}
