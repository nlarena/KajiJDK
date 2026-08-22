package java.lang.ref;

import java.util.concurrent.ThreadFactory;

/**
 * Runs an action once an object becomes unreachable — the supported replacement for
 * {@code finalize()}.
 *
 * <p>It is {@link PhantomReference} plus a thread, packaged so that the dangerous part is hard to
 * get wrong. The rule that makes it safe is stated in one line and violated constantly:
 * <strong>the cleaning action must not refer to the object being cleaned</strong>. If it does, the
 * action keeps the object reachable, the object is never collected, and the cleanup never runs —
 * a leak that looks like a cleanup. That is why {@link #register} takes the object and the action
 * as two separate arguments instead of a method on the object, and why the action normally closes
 * over a small "state" object holding just the handle or address to release.
 *
 * <p>Why it replaced finalization: a finalizer runs on an unspecified thread at an unspecified
 * time, can resurrect the object, delays collection by a whole cycle, and cannot be cancelled.
 * A cleaner runs on a thread you supply, cannot resurrect anything (a phantom referent is
 * unreadable), and can be cancelled through {@link Cleanable#clean()}.
 *
 * <p><strong>In this VM</strong> the cleaning thread is started on demand and drains the queue in
 * a loop. Registration is real and {@link Cleanable#clean()} works; whether an action fires by
 * itself depends on the collector reaching the referent, which it does for the weak edge every
 * {@code Reference} subclass carries.
 */
public final class Cleaner {

    private final ReferenceQueue queue;
    private final ThreadFactory factory;
    private boolean started;

    private Cleaner(ThreadFactory factory) {
        this.queue = new ReferenceQueue();
        this.factory = factory;
    }

    /**
     * Creates a cleaner with a thread of its own.
     *
     * @return the new cleaner
     */
    public static Cleaner create() {
        return new Cleaner(null);
    }

    /**
     * Creates a cleaner whose thread comes from the given factory.
     *
     * <p>Supplying the factory is how a caller controls the thread's name, priority and daemon
     * status — the things a finalizer never let anyone decide.
     *
     * @param threadFactory the factory that supplies the cleaning thread
     * @return the new cleaner
     */
    public static Cleaner create(ThreadFactory threadFactory) {
        return new Cleaner(threadFactory);
    }

    /**
     * Registers an object with a cleaning action.
     *
     * @param obj the object to watch; when it becomes unreachable, {@code action} runs
     * @param action the cleaning action, which <strong>must not</strong> refer to {@code obj}
     * @return a handle that can run the action early, or cancel it
     */
    public Cleanable register(Object obj, Runnable action) {
        start();
        return new CleanerCleanable(obj, action, queue);
    }

    // The draining thread is started on first use rather than in the constructor: a cleaner that
    // never registers anything should not cost a thread.
    private void start() {
        if (!started) {
            started = true;
            Runnable drain = new CleanerLoop(queue);
            Thread t;
            if (factory == null) {
                t = new Thread(drain);
            } else {
                t = factory.newThread(drain);
            }
            t.start();
        }
    }

    /**
     * The handle {@link Cleaner#register} returns.
     *
     * <p>Calling {@link #clean()} explicitly is the normal path, not the exception: a
     * {@code close()} method should run the cleanup immediately, and leave the cleaner as the
     * safety net for the case where nobody called {@code close()}.
     */
    public interface Cleanable {

        /**
         * Runs the cleaning action now, at most once, and unregisters it.
         */
        void clean();
    }

    /**
     * The registration: a phantom reference to the watched object that also carries its action.
     *
     * <p>Top-level and package-private rather than nested — the project's idiom — and it deliberately
     * does <em>not</em> expose the referent, since {@link PhantomReference#get()} is always null.
     */
    static final class CleanerCleanable extends PhantomReference implements Cleanable {

        private Runnable action;

        CleanerCleanable(Object referent, Runnable action, ReferenceQueue queue) {
            super(referent, queue);
            this.action = action;
        }

        public void clean() {
            // At most once: the field is dropped before running, so a concurrent enqueue that reaches
            // this object later finds nothing left to do.
            Runnable pending = action;
            action = null;
            if (pending != null) {
                pending.run();
            }
        }
    }

    /**
     * The cleaning thread's body: take references off the queue and run whatever each one carries.
     */
    static final class CleanerLoop implements Runnable {

        private final ReferenceQueue queue;

        CleanerLoop(ReferenceQueue queue) {
            this.queue = queue;
        }

        public void run() {
            // `poll()` y no `remove()`: nuestro `ReferenceQueue` todavia no tiene la forma
            // bloqueante, asi que el lazo cede el procesador cuando no hay nada. Cuando exista
            // `remove()` esto pasa a ser una espera de verdad en vez de una vuelta en vacio.
            boolean running = true;
            while (running) {
                Reference ref = queue.poll();
                if (ref instanceof CleanerCleanable) {
                    CleanerCleanable c = (CleanerCleanable) ref;
                    c.clean();
                } else {
                    Thread.yield();
                }
            }
        }
    }
}
