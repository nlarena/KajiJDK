package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.Continuation -- a delimited continuation (Project Loom).
 *
 * <p>A continuation is a computation that can be **suspended halfway and resumed later**, perhaps
 * on another thread. It is the substrate of virtual threads: suspending one is keeping its
 * continuation, and resuming it is running it.
 *
 * <h2>Here it is a continuation that is never suspended, and that is a legitimate state</h2>
 *
 * <p>Suspending demands that the VM lift the frames of the stack and keep them in the heap. This VM
 * does not know how to do it (see {@link ContinuationSupport}), so **every continuation is
 * permanently pinned**. The word is the JDK's: a *pinned* continuation is one that cannot be
 * suspended right now.
 *
 * <p>The important thing is that the JDK **already defines what happens in that case**, because in
 * HotSpot it happens too --inside a synchronized block, or with a native frame in the middle--. So
 * no behaviour has to be invented: the one already specified is used.
 *
 * <ul>
 * <li>{@link #run()} runs the target **to the end**. It is exactly what happens when no
 *     {@code yield} succeeds: the computation is not cut, it goes on to the end.</li>
 * <li>{@link #yield} returns `false`, which is its documented way of saying "it could not".</li>
 * <li>{@link #isPinned} returns `true`, and {@link #onPinned} receives {@link Pinned#NATIVE}: the
 *     reason is that there is a frame that cannot be lifted.</li>
 * <li>{@link #tryPreempt} returns {@link PreemptStatus#PERM_FAIL_UNSUPPORTED} -- the constant the
 *     JDK has precisely for this.</li>
 * </ul>
 *
 * <p>A user who calls `run()` and does not use `yield` gets the right result. One who depends on
 * suspending receives a `false` they can look at, instead of a suspension that did not happen.
 *
 * <h2>What is left out</h2>
 *
 * <p>{@code getStackTrace()}, the three {@code stackWalker(...)} and {@code wrapWalk(...)}. The
 * five hand over **the frames of the continuation**, which is precisely what does not exist: with
 * no stack kept in the heap there is nothing to walk. Returning the stack of the current thread
 * would be worse than not being there -- it would read as that of the continuation and point
 * elsewhere.
 */
public class Continuation {

    /**
     * Why a continuation could not be suspended.
     *
     * <p>The four reasons belong to the VM and not to the program, and that is why the caller
     * cannot "fix them": the only thing they can do is not count on suspension.
     */
    public enum Pinned {
        /** There is a native frame on the stack; it cannot be lifted. */
        NATIVE,
        /** It is inside a synchronized block. */
        MONITOR,
        /** It is in a critical section of the VM. */
        CRITICAL_SECTION,
        /** An exception was being unwound. */
        EXCEPTION
    }

    /** The result of trying to preempt a continuation. */
    public enum PreemptStatus {
        /** It could. */
        SUCCESS(null),
        /** It cannot, and it is not going to be able to: this VM does not support preemption. */
        PERM_FAIL_UNSUPPORTED(null),
        /** It cannot because it is already suspending. */
        PERM_FAIL_YIELDING(null),
        /** It cannot because it is not mounted on any thread. */
        PERM_FAIL_NOT_MOUNTED(null),
        /** Not now: it is in a critical section. */
        TRANSIENT_FAIL_PINNED_CRITICAL_SECTION(Pinned.CRITICAL_SECTION),
        /** Not now: there is a native frame. */
        TRANSIENT_FAIL_PINNED_NATIVE(Pinned.NATIVE),
        /** Not now: there is a monitor held. */
        TRANSIENT_FAIL_PINNED_MONITOR(Pinned.MONITOR);

        private final Pinned reason;

        PreemptStatus(Pinned reason) {
            this.reason = reason;
        }

        /**
         * The pinning reason, or `null` if the failure was not due to being pinned.
         *
         * <p>The `PERM_FAIL_*` return `null` and the `TRANSIENT_FAIL_PINNED_*` the reason. The
         * difference is the one that matters to whoever retries: a transient failure may go away, a
         * permanent one does not.
         */
        public Pinned pinned() {
            return this.reason;
        }
    }

    // The stack of continuations mounted on the current thread. A `ThreadLocal` and not a field of
    // `Thread`, for the same reason as in `StackableScope`: `Thread` cannot be touched from this
    // package and the semantics are identical.
    private static final ThreadLocal<Continuation> MOUNTED = new ThreadLocal<Continuation>();

    private final ContinuationScope scope;
    private final Runnable target;
    private Continuation parent;
    private boolean done;

    public Continuation(ContinuationScope scope, Runnable target) {
        this.scope = scope;
        this.target = target;
    }

    /** The scope that delimits this continuation. */
    public ContinuationScope getScope() {
        return this.scope;
    }

    /** The continuation that encloses it on this thread, or `null`. */
    public Continuation getParent() {
        return this.parent;
    }

    /** Whether it has already finished. */
    public boolean isDone() {
        return this.done;
    }

    /**
     * Whether it was preempted.
     *
     * <p>Always `false`: preemption needs suspension, and here it cannot be done. It is consistent
     * with {@link #tryPreempt} never returning {@link PreemptStatus#SUCCESS}.
     */
    public boolean isPreempted() {
        return false;
    }

    /**
     * It runs the continuation until it is suspended or finishes -- here, always until it finishes.
     *
     * <p>It is `final` as in the JDK: the cycle of mounting, running and unmounting cannot be
     * redefined halfway without breaking the invariant of which continuation is mounted on the
     * thread. What is redefined are the hooks {@link #onContinue} and {@link #onPinned}.
     *
     * <p>The unmounting goes in a `finally`: if the target throws, the continuation has to leave
     * the stack of the thread all the same, or the thread is left believing it is still inside
     * something that already blew up.
     *
     * @throws IllegalStateException if it has already finished
     */
    public final void run() {
        if (this.done) {
            throw new IllegalStateException("this continuation has already finished");
        }
        this.parent = Continuation.MOUNTED.get();
        Continuation.MOUNTED.set(this);
        this.onContinue();
        try {
            if (this.target != null) {
                this.target.run();
            }
        } finally {
            Continuation.MOUNTED.set(this.parent);
            this.done = true;
        }
    }

    /** Notice that the continuation starts or resumes. To be overridden. */
    protected void onContinue() {
    }

    /** Notice that a {@link #yield} failed because of being pinned. To be overridden. */
    protected void onPinned(Pinned reason) {
    }

    /**
     * It tries to suspend the continuation of the given scope.
     *
     * <p>It returns `false` **always** in this VM, and notifies through {@link #onPinned} with
     * {@link Pinned#NATIVE}. The `false` is not an error: it is the answer the contract defines for
     * when it could not, and the one that happens in HotSpot every time there is a native frame in
     * the middle.
     */
    public static boolean yield(ContinuationScope scope) {
        Continuation current = Continuation.MOUNTED.get();
        if (current != null) {
            current.onPinned(Pinned.NATIVE);
        }
        return false;
    }

    /**
     * Whether the continuation of that scope is pinned.
     *
     * <p>Always `true`. Asking it before trying to suspend is the normal use, and here the answer
     * avoids the attempt.
     */
    public static boolean isPinned(ContinuationScope scope) {
        return true;
    }

    /** The mounted continuation of that scope on the current thread, or `null`. */
    public static Continuation getCurrentContinuation(ContinuationScope scope) {
        Continuation c = Continuation.MOUNTED.get();
        while (c != null) {
            if (scope == null || scope.equals(c.scope)) {
                return c;
            }
            c = c.parent;
        }
        return null;
    }

    /**
     * It tries to preempt the continuation running on that thread.
     *
     * @return always {@link PreemptStatus#PERM_FAIL_UNSUPPORTED} in this VM
     */
    public PreemptStatus tryPreempt(Thread thread) {
        return PreemptStatus.PERM_FAIL_UNSUPPORTED;
    }

    /**
     * It prevents suspending until the matching {@link #unpin}.
     *
     * <p>It does nothing, and that is the right thing: where nothing can be suspended, there is
     * nothing to prevent.
     *
     * <p><strong>Here it is not `native`, and the JDK does declare it so.</strong> A `native` with
     * no registered implementation on this VM does not throw an exception -- it brings the process
     * down. An empty method does the same as the JDK's would (nothing observable); a `native` one
     * would kill whoever calls it.
     */
    public static void pin() {
    }

    /** The symmetric of {@link #pin}, and for the same reason it does nothing either. */
    public static void unpin() {
    }
}
