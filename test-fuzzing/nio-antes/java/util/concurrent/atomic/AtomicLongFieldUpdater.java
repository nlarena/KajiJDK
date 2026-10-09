package java.util.concurrent.atomic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

// The {@code long} counterpart of {@link AtomicIntegerFieldUpdater}; that class's header explains
// the whole design and is not repeated here. The short of it: KajiJDK's VM offers no
// compare-and-swap seam on a reflective field, so the five primitives below run inside
// `synchronized (FieldUpdaterLock.MONITOR)` -- mutual exclusion, which is stronger than the CAS the
// JDK uses -- and the compound operations are the JDK's own retry loops on top of them, so that the
// caller's function never runs while the monitor is held.
//
// One thing that is *more* than a stylistic echo here: a 64-bit field is the case where the JDK
// itself does not promise that a plain read or write is atomic. Since every access below is taken
// under the monitor, a `long` is never torn, which is the guarantee this class owes.
//
// As in the {@code int} updater, the JDK's caller-access check is absent -- it needs the calling
// class, and `StackWalker.getCallerClass()` is unsupported on this VM. All the other rejections
// (missing field, wrong type, non-volatile, static) match the JDK in kind, message and order.
public abstract class AtomicLongFieldUpdater<T> {

    /**
     * An updater for the {@code volatile long} field named {@code fieldName} of {@code tclass}.
     *
     * @throws IllegalArgumentException if the field is not a non-static {@code volatile long}
     * @throws RuntimeException if the field is not found
     */
    public static <U> AtomicLongFieldUpdater<U> newUpdater(Class<U> tclass, String fieldName) {
        return new LongUpdater<U>(tclass, fieldName);
    }

    protected AtomicLongFieldUpdater() {
    }

    // ---- the five primitives (each one critical section wide) ----

    public abstract boolean compareAndSet(T obj, long expectedValue, long newValue);

    public abstract boolean weakCompareAndSet(T obj, long expectedValue, long newValue);

    public abstract void set(T obj, long newValue);

    public abstract void lazySet(T obj, long newValue);

    public abstract long get(T obj);

    // ---- everything else: the JDK's retry loops over the primitives ----

    public long getAndSet(T obj, long newValue) {
        long prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, newValue));
        return prev;
    }

    public long getAndIncrement(T obj) {
        return getAndAdd(obj, 1L);
    }

    public long getAndDecrement(T obj) {
        return getAndAdd(obj, -1L);
    }

    public long getAndAdd(T obj, long delta) {
        long prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, prev + delta));
        return prev;
    }

    public long incrementAndGet(T obj) {
        return getAndAdd(obj, 1L) + 1L;
    }

    public long decrementAndGet(T obj) {
        return getAndAdd(obj, -1L) - 1L;
    }

    public long addAndGet(T obj, long delta) {
        return getAndAdd(obj, delta) + delta;
    }

    public final long getAndUpdate(T obj, LongUnaryOperator updateFunction) {
        long prev;
        long next;
        do {
            prev = get(obj);
            next = updateFunction.applyAsLong(prev);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    public final long updateAndGet(T obj, LongUnaryOperator updateFunction) {
        long prev;
        long next;
        do {
            prev = get(obj);
            next = updateFunction.applyAsLong(prev);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    public final long getAndAccumulate(T obj, long x, LongBinaryOperator accumulatorFunction) {
        long prev;
        long next;
        do {
            prev = get(obj);
            next = accumulatorFunction.applyAsLong(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    public final long accumulateAndGet(T obj, long x, LongBinaryOperator accumulatorFunction) {
        long prev;
        long next;
        do {
            prev = get(obj);
            next = accumulatorFunction.applyAsLong(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    static final class LongUpdater<T> extends AtomicLongFieldUpdater<T> {

        private final Class<?> tclass;
        private final Field field;

        LongUpdater(Class<T> tclass, String fieldName) {
            Field f;
            try {
                f = tclass.getDeclaredField(fieldName);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            if (f.getType() != Long.TYPE) {
                throw new IllegalArgumentException("Must be long type");
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
            this.field = f;
        }

        private void accessCheck(T obj) {
            if (!this.tclass.isInstance(obj)) {
                throw new ClassCastException();
            }
        }

        public long get(T obj) {
            accessCheck(obj);
            synchronized (FieldUpdaterLock.MONITOR) {
                return this.field.getLong(obj);
            }
        }

        public void set(T obj, long newValue) {
            accessCheck(obj);
            synchronized (FieldUpdaterLock.MONITOR) {
                this.field.setLong(obj, newValue);
            }
        }

        public void lazySet(T obj, long newValue) {
            set(obj, newValue);
        }

        public boolean compareAndSet(T obj, long expectedValue, long newValue) {
            accessCheck(obj);
            synchronized (FieldUpdaterLock.MONITOR) {
                if (this.field.getLong(obj) != expectedValue) {
                    return false;
                }
                this.field.setLong(obj, newValue);
                return true;
            }
        }

        public boolean weakCompareAndSet(T obj, long expectedValue, long newValue) {
            return compareAndSet(obj, expectedValue, newValue);
        }
    }
}
