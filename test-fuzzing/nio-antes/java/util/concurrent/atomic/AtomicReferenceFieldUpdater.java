package java.util.concurrent.atomic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

// The reference counterpart of {@link AtomicIntegerFieldUpdater}; that class's header explains the
// whole design and is not repeated here. The short of it: KajiJDK's VM offers no compare-and-swap
// seam on a reflective field, so the five primitives below run inside
// `synchronized (FieldUpdaterLock.MONITOR)` -- mutual exclusion, which is stronger than the CAS the
// JDK uses -- and the compound operations are the JDK's own retry loops on top of them, so that the
// caller's function never runs while the monitor is held.
//
// Two rejections are specific to this class and both are the JDK's. The field's declared type must
// be **exactly** `vclass` -- not a subtype and not a supertype -- and a mismatch is a bare
// `ClassCastException`, checked before the `volatile` check. And every value written or offered as
// the expected value is checked against `vclass` (`null` always passes), also with a bare
// `ClassCastException`: the erased generic signature cannot stop a raw-typed caller from handing in
// the wrong thing, so the check is made at runtime.
//
// As in the {@code int} updater, the JDK's caller-access check is absent -- it needs the calling
// class, and `StackWalker.getCallerClass()` is unsupported on this VM.
public abstract class AtomicReferenceFieldUpdater<T, V> {

    /**
     * An updater for the {@code volatile} field of type {@code vclass} named {@code fieldName} of
     * {@code tclass}.
     *
     * @throws ClassCastException if the field's declared type is not exactly {@code vclass}
     * @throws IllegalArgumentException if the field is not a non-static {@code volatile} field
     * @throws RuntimeException if the field is not found
     */
    public static <U, W> AtomicReferenceFieldUpdater<U, W> newUpdater(Class<U> tclass,
            Class<W> vclass, String fieldName) {
        return new ReferenceUpdater<U, W>(tclass, vclass, fieldName);
    }

    protected AtomicReferenceFieldUpdater() {
    }

    // ---- the five primitives (each one critical section wide) ----

    public abstract boolean compareAndSet(T obj, V expectedValue, V newValue);

    public abstract boolean weakCompareAndSet(T obj, V expectedValue, V newValue);

    public abstract void set(T obj, V newValue);

    public abstract void lazySet(T obj, V newValue);

    public abstract V get(T obj);

    // ---- everything else: the JDK's retry loops over the primitives ----

    public V getAndSet(T obj, V newValue) {
        V prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, newValue));
        return prev;
    }

    public final V getAndUpdate(T obj, UnaryOperator<V> updateFunction) {
        V prev;
        V next;
        do {
            prev = get(obj);
            next = updateFunction.apply(prev);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    public final V updateAndGet(T obj, UnaryOperator<V> updateFunction) {
        V prev;
        V next;
        do {
            prev = get(obj);
            next = updateFunction.apply(prev);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    public final V getAndAccumulate(T obj, V x, BinaryOperator<V> accumulatorFunction) {
        V prev;
        V next;
        do {
            prev = get(obj);
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    public final V accumulateAndGet(T obj, V x, BinaryOperator<V> accumulatorFunction) {
        V prev;
        V next;
        do {
            prev = get(obj);
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    static final class ReferenceUpdater<T, V> extends AtomicReferenceFieldUpdater<T, V> {

        private final Class<?> tclass;
        private final Class<?> vclass;
        private final Field field;

        ReferenceUpdater(Class<T> tclass, Class<V> vclass, String fieldName) {
            Field f;
            try {
                f = tclass.getDeclaredField(fieldName);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            // Exact type identity, and before the volatile check -- both are the JDK's order.
            if (f.getType() != vclass) {
                throw new ClassCastException();
            }
            int mods = f.getModifiers();
            if (!Modifier.isVolatile(mods)) {
                throw new IllegalArgumentException("Must be volatile type");
            }
            if (Modifier.isStatic(mods)) {
                throw new IllegalArgumentException();
            }
            f.setAccessible(true);
            this.tclass = tclass;
            this.vclass = vclass;
            this.field = f;
        }

        private void accessCheck(T obj) {
            if (!this.tclass.isInstance(obj)) {
                throw new ClassCastException();
            }
        }

        private void valueCheck(V v) {
            if (v != null && !this.vclass.isInstance(v)) {
                throw new ClassCastException();
            }
        }

        @SuppressWarnings("unchecked")
        public V get(T obj) {
            accessCheck(obj);
            synchronized (FieldUpdaterLock.MONITOR) {
                return (V) this.field.get(obj);
            }
        }

        public void set(T obj, V newValue) {
            accessCheck(obj);
            valueCheck(newValue);
            synchronized (FieldUpdaterLock.MONITOR) {
                this.field.set(obj, newValue);
            }
        }

        public void lazySet(T obj, V newValue) {
            set(obj, newValue);
        }

        public boolean compareAndSet(T obj, V expectedValue, V newValue) {
            accessCheck(obj);
            valueCheck(newValue);
            synchronized (FieldUpdaterLock.MONITOR) {
                // Reference identity, not `equals` -- the same rule the JDK's CAS follows.
                if (this.field.get(obj) != expectedValue) {
                    return false;
                }
                this.field.set(obj, newValue);
                return true;
            }
        }

        public boolean weakCompareAndSet(T obj, V expectedValue, V newValue) {
            return compareAndSet(obj, expectedValue, newValue);
        }
    }
}
