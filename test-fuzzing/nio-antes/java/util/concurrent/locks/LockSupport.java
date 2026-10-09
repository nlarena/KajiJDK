package java.util.concurrent.locks;

// The blocking and waking primitive the JDK builds the whole of `java.util.concurrent` on:
// `park()` puts the current thread to sleep, `unpark(t)` wakes it. What makes it usable --and what
// distinguishes it from `wait`/`notify`-- is the **permit**: an `unpark` arriving before the `park`
// is not lost, it is stored, and the next `park` consumes it and returns at once. That is why a
// caller can check their condition and then fall asleep with no window between the two.
//
// # What holds this up in KajiJDK
//
// `park` and `unpark` are **genuinely native**: the VM intercepts them at `invokestatic` (they are
// scheduler operations, not leaf calls) and keeps the permit per thread. It is not an emulation nor
// a spin loop. `java/ParkTest.java` checks it with all three thread substrates, the real parallel
// one included.
//
// # The timed waits
//
// `parkNanos` and `parkUntil` **are here**, and they are sound: the deadline is kept by the VM, not
// by a detour on this side. This file used to say the opposite --that the four overloads could not
// be written honestly-- and that was true while the only intrinsic was a bare `park()`. What was
// done was to lift the block: the VM now has a `park` with a deadline, with the **same** permit as
// the deadline-less `park()`.
//
// That they share the permit is what matters, and it is exactly what made emulating it from Java
// impossible: an `Object.wait(ms)` on this side would not be woken by an `unpark`, and an `unpark`
// would not consume that wait. They would be two permit systems, and `unpark(t)` --which the VM
// keeps-- would touch only one. Inside the VM there is a single one.
//
// **The deadline is measured on the opcode clock**, like every timed wait on this VM
// (`Thread.sleep`, `Object.wait(ms)`, `join(ms)`): there is no wall clock here. A `parkNanos` does
// not wait real nanoseconds but their equivalent in executed instructions. It is a property of the
// whole VM and not of this method, but it is worth bearing in mind before using the deadline to
// measure anything. What does hold, and it is what a `Lock` cares about: the wait **ends**, an
// `unpark` also ends it, and a permit that arrived earlier skips it.
//
// `park(Object blocker)` **is** here, and it blocks correctly, but it **does not record the
// blocker**: the VM keeps the call before a single instruction of the body runs, so there is nowhere
// to note it. `setCurrentBlocker`/`getBlocker` are an honest and complete pair between themselves
// --what one stores is what the other returns--; what is missing is `park(Object)`'s side effect,
// and it is said here rather than simulated.
//
// # A fidelity defect of the VM's, worth knowing before using this
//
// Interrupting a thread parked in `park()` **throws `InterruptedException`** instead of making
// `park` return with the interrupt flag set. The exception is also undeclared (`park` has no
// `throws`). Repro in `scratchpad/zzlocks/ParkIntr.java`: the real JDK returns 1, our VM kills the
// thread. That is why `AbstractQueuedSynchronizer` does **not** lean on `park` for its queue: it
// uses each node's monitor, which does throw where it should.
public final class LockSupport {

    // The blockers noted with `setCurrentBlocker`, per thread. A map and not a field of `Thread`
    // because `Thread` does not have that field and this package cannot add it. The entry is
    // **removed** when the blocker goes back to `null`, which is what every reasonable caller does
    // on leaving the wait, so the map does not grow with dead threads.
    private static final java.util.HashMap<Thread, Object> BLOCKERS =
            new java.util.HashMap<Thread, Object>();

    private LockSupport() {
    }

    /**
     * It puts the current thread to sleep until it is woken with {@link #unpark}, unless it
     * already has a stored permit, in which case it consumes it and returns at once.
     *
     * <p>It may return **for no reason** (a spurious wake-up), as in the JDK: the caller has to
     * check their condition in a loop, never assume that a return means anything.
     */
    public static native void park();

    /**
     * The same as {@link #park()}, with an object saying *why* one is waiting.
     *
     * <p>The blocker is for diagnostics only and **this does not record it** (the VM keeps the call;
     * see the class's header). The blocking itself is identical to `park()`'s.
     */
    public static native void park(Object blocker);

    /**
     * It gives `thread` a permit: if it is parked, it wakes it; if not, the permit is stored and
     * its next {@link #park()} returns at once.
     *
     * <p>`unpark(null)`, or on a thread that has not started or has already finished, does
     * nothing.
     */
    public static native void unpark(Thread thread);

    /**
     * It puts the current thread to sleep until it is woken with {@link #unpark} or until `nanos`
     * have passed, whichever comes first.
     *
     * <p>It consumes the permit if there already was one, just like {@link #park()}. A zero or
     * negative `nanos` returns at once **without** consuming it: it is what the JDK does --
     * `parkNanos(0)` is not a wait of zero, it is not waiting.
     *
     * <p>See the class's header on what units the deadline runs in.
     */
    public static native void parkNanos(long nanos);

    /**
     * The same as {@link #parkNanos(long)}, with an object saying *why* one is waiting.
     *
     * <p>The blocker is for diagnostics only and **this does not record it**, for the same reason as
     * {@link #park(Object)}: the VM keeps the call before an instruction of the body runs. The
     * blocking and the deadline are identical to the other form's.
     */
    public static native void parkNanos(Object blocker, long nanos);

    /**
     * It puts the current thread to sleep until it is woken or until that absolute instant, in
     * milliseconds since the epoch -- {@link System#currentTimeMillis}'s scale.
     *
     * <p>It is written on {@link #parkNanos(long)} and is not a separate intrinsic, and that is on
     * purpose: an absolute deadline **is** a relative deadline computed once. Writing it this way
     * leaves a single timed wait in the VM instead of two that could drift apart.
     *
     * <p>An already expired deadline returns at once without consuming the permit.
     */
    public static void parkUntil(long deadline) {
        long left = deadline - System.currentTimeMillis();
        if (left > 0L) {
            LockSupport.parkNanos(left * 1000000L);
        }
    }

    /**
     * The same as {@link #parkUntil(long)}, with a diagnostic blocker.
     *
     * <p>See {@link #park(Object)}: the blocker is not recorded.
     */
    public static void parkUntil(Object blocker, long deadline) {
        long left = deadline - System.currentTimeMillis();
        if (left > 0L) {
            LockSupport.parkNanos(blocker, left * 1000000L);
        }
    }

    /**
     * It notes the current thread's blocker. `null` clears it.
     *
     * <p>It is what a `Lock` does before parking, so that a thread dump says what is being waited
     * on.
     */
    public static void setCurrentBlocker(Object blocker) {
        Thread self = Thread.currentThread();
        synchronized (BLOCKERS) {
            if (blocker == null) {
                BLOCKERS.remove(self);
            } else {
                BLOCKERS.put(self, blocker);
            }
        }
    }

    /**
     * The blocker noted for `thread`, or `null` if there is none.
     *
     * <p>It is a **snapshot** and serves only for diagnostics: by the time the answer arrives, the
     * thread may have stopped waiting. The JDK's javadoc says the same.
     *
     * @throws NullPointerException if `thread` is `null`
     */
    public static Object getBlocker(Thread thread) {
        if (thread == null) {
            throw new NullPointerException("thread");
        }
        Object b;
        synchronized (BLOCKERS) {
            b = BLOCKERS.get(thread);
        }
        return b;
    }
}
