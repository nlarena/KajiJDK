package java.lang;

import java.util.NoSuchElementException;
import java.util.function.Supplier;


/**
 * A value bound for the DURATION of an operation, visible to whatever that operation calls, and
 * unbound again when it returns.
 *
 * <p>It is what a {@link ThreadLocal} is usually being misused as. A thread local is a mutable
 * per-thread variable with no lifetime at all: anyone can set it, nobody has to clear it, and a
 * value left behind on a pooled thread leaks into the next task that runs there. A scoped value
 * inverts all three — it is immutable once bound, the binding belongs to a {@code run}/{@code
 * call} that must return for the program to continue, and unbinding is not something a caller
 * can forget because it is not something a caller does.
 *
 * <pre>{@code
 * static final ScopedValue<User> CURRENT = ScopedValue.newInstance();
 *
 * ScopedValue.where(CURRENT, user).run(() -> handle(request));
 * // inside handle, and anything it calls: CURRENT.get() is `user`
 * }</pre>
 *
 * <p>Bindings NEST rather than overwrite: rebinding the same value inside an inner scope hides
 * the outer binding for that scope and restores it on the way out, which is why a chain and not
 * a slot.
 *
 * @implNote A KajiLibrary subset of the JDK preview API. The JDK confines a binding to the
 *           thread AND its structured children, so a subtask forked inside the scope inherits
 *           it; here the chain is per thread and a new thread starts unbound. The rest — the
 *           immutability, the nesting, the automatic unbinding — is the same.
 */
public final class ScopedValue<T> {

    // The innermost binding of the calling thread, or null when nothing is bound. A ThreadLocal
    // is the storage and not the model: what makes this a scoped value is that only run() and
    // call() ever write it, and always in pairs.
    static final ThreadLocal<Carrier> BOUND = new ThreadLocal<Carrier>();

    private ScopedValue() {
    }

    /** A fresh, unbound scoped value. Identity is all that distinguishes one from another. */
    public static <T> ScopedValue<T> newInstance() {
        return new ScopedValue<T>();
    }

    /** Begins a set of bindings. Nothing is bound until {@code run} or {@code call} is invoked. */
    public static <T> Carrier where(ScopedValue<T> key, T value) {
        return Carrier.of(key, value);
    }

    /**
     * The value bound in the current scope.
     *
     * @throws NoSuchElementException if nothing is bound — unbound is a programming error here,
     *         not a null to be checked for
     */
    public T get() {
        Object found = ScopedValue.lookup(this, ScopedValue.absent());
        if (found == ScopedValue.absent()) {
            throw new NoSuchElementException("scoped value not bound");
        }
        return ScopedValue.cast(found);
    }

    public boolean isBound() {
        return ScopedValue.lookup(this, ScopedValue.absent()) != ScopedValue.absent();
    }

    /** The bound value, or {@code other} if there is none. */
    public T orElse(T other) {
        Object found = ScopedValue.lookup(this, ScopedValue.absent());
        if (found == ScopedValue.absent()) {
            return other;
        }
        return ScopedValue.cast(found);
    }

    /**
     * The bound value, or the exception {@code exceptionSupplier} makes.
     *
     * <p>The exception is built lazily and only when needed, which is the whole reason the
     * parameter is a supplier: an unbound scoped value is often not an error, and constructing
     * a throwable that is then discarded costs a stack trace.
     */
    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (exceptionSupplier == null) {
            throw new NullPointerException();
        }
        Object found = ScopedValue.lookup(this, ScopedValue.absent());
        if (found == ScopedValue.absent()) {
            throw exceptionSupplier.get();
        }
        return ScopedValue.cast(found);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    // ---- the plumbing ----

    // A sentinel that no caller can produce, so a value legitimately bound to null is still
    // distinguishable from nothing being bound at all.
    private static final Object ABSENT = new Object();

    private static Object absent() {
        return ScopedValue.ABSENT;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    // Walks the calling thread chain from the innermost binding out. Innermost first is what
    // makes an inner rebinding hide the outer one.
    static Object lookup(ScopedValue<?> key, Object missing) {
        Carrier at = ScopedValue.BOUND.get();
        while (at != null) {
            if (at.key() == key) {
                return at.value();
            }
            at = at.previous();
        }
        return missing;
    }

    /**
     * One or more bindings, waiting for an operation to run under them.
     *
     * <p>Immutable and chained: {@code where} returns a NEW carrier holding the extra binding
     * and pointing at this one, so a carrier can be built once and reused, and two operations
     * built from the same prefix cannot disturb each other.
     */
    public static final class Carrier {

        private final ScopedValue<?> key;
        private final Object value;
        private final Carrier prev;

        private Carrier(ScopedValue<?> key, Object value, Carrier prev) {
            this.key = key;
            this.value = value;
            this.prev = prev;
        }

        static <T> Carrier of(ScopedValue<T> key, T value) {
            if (key == null) {
                throw new NullPointerException();
            }
            return new Carrier(key, value, null);
        }

        /** Adds another binding, innermost last. */
        public <T> Carrier where(ScopedValue<T> key, T value) {
            if (key == null) {
                throw new NullPointerException();
            }
            return new Carrier(key, value, this);
        }

        /**
         * The value this carrier binds for {@code key}, WITHOUT running anything.
         *
         * @throws NoSuchElementException if this carrier does not bind it
         */
        public <T> T get(ScopedValue<T> key) {
            Carrier at = this;
            while (at != null) {
                if (at.key == key) {
                    return ScopedValue.cast(at.value);
                }
                at = at.prev;
            }
            throw new NoSuchElementException("scoped value not bound");
        }

        /**
         * Runs {@code op} with these bindings in place, and returns what it produced.
         *
         * <p>The restore is in a {@code finally} and not after the call, which is the entire
         * safety argument of the class: however the operation leaves — normally, by throwing,
         * by an error — the previous bindings are back before this method returns.
         */
        public <R, X extends Throwable> R call(CallableOp<? extends R, X> op) throws X {
            if (op == null) {
                throw new NullPointerException();
            }
            Carrier previous = ScopedValue.BOUND.get();
            ScopedValue.BOUND.set(this.linkedTo(previous));
            try {
                return op.call();
            } finally {
                ScopedValue.BOUND.set(previous);
            }
        }

        /** Runs {@code op} with these bindings in place. */
        public void run(Runnable op) {
            if (op == null) {
                throw new NullPointerException();
            }
            Carrier previous = ScopedValue.BOUND.get();
            ScopedValue.BOUND.set(this.linkedTo(previous));
            try {
                op.run();
            } finally {
                ScopedValue.BOUND.set(previous);
            }
        }

        // Re-roots this chain on top of whatever was already bound, so an inner scope sees the
        // outer bindings too. Copying is unavoidable: the carrier is immutable and shareable,
        // so it cannot be made to point at one particular caller stack.
        private Carrier linkedTo(Carrier outer) {
            if (this.prev == null) {
                return new Carrier(this.key, this.value, outer);
            }
            return new Carrier(this.key, this.value, this.prev.linkedTo(outer));
        }

        ScopedValue<?> key() {
            return this.key;
        }

        Object value() {
            return this.value;
        }

        Carrier previous() {
            return this.prev;
        }
    }

    /**
     * The operation {@link Carrier#call} runs.
     *
     * <p>Not {@link java.util.concurrent.Callable}: this one is generic in what it throws, so an
     * operation that throws nothing does not force its caller into a {@code catch (Exception)}.
     */
    public interface CallableOp<T, X extends Throwable> {

        T call() throws X;
    }
}
