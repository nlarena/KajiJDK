package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

// `AbstractQueuedSynchronizer`'s twin with the state in a `long` instead of an `int`. It is the
// same class, member for member, with `long` where the other says `int`: it exists because there
// are synchronizers whose meaning does not fit in 32 bits --the textbook case is `StampedLock`,
// which packs a reader count and a write sequence number into the same word-- and splitting it into
// two fields would lose the atomicity of reading them together.
//
// **The whole explanation of how this works is in `AbstractQueuedSynchronizer`'s header**, and it is
// not repeated here: the decision not to rebuild `ReentrantLock`/`ReentrantReadWriteLock` on the
// frame, why the FIFO queue and not a stack, why one sleeps on each node's monitor and not in
// `LockSupport.park`, and finding #105's style rule (no `return` inside a `synchronized`).
//
// In the JDK the two classes share no code either: the long one is a generated copy of the short
// one. Here it is the same, and for the same reason -- Java has no way of parameterising a class by
// a primitive type, and an `AbstractQueuedSynchronizer<T>` with a boxed `Long` would change the
// subclass's `try*` contract, which is exactly what has to be respected.
//
// The one difference that is **not** the type: this one's constructor is `public` and the short
// one's `protected`. It is like that in the JDK and it is copied as it stands; an access modifier is
// part of the contract, not a detail.
public abstract class AbstractQueuedLongSynchronizer extends AbstractOwnableSynchronizer
        implements Serializable {

    // A queue wait's possible outcomes. An `int` and not three `boolean`s because the wait has
    // exactly four endings, and naming them rules out the impossible combination.
    private static final int ACQUIRED = 0;
    private static final int ACQUIRED_INTERRUPTED = 1;
    private static final int INTERRUPTED = 2;
    private static final int TIMED_OUT = 3;

    // The internal monitor. It guards `state` and the queue's links, and nothing else.
    //
    // Two rules make it safe, and they hold for every line of this class:
    //   - it is **never** held while a subclass's `try*` runs (which is going to call
    //     `compareAndSetState`, that is, take it again, and which may do anything at all);
    //   - it is **never** held while a thread sleeps.
    // With those two, the only lock order that exists is `sync` and then a node's monitor, never
    // the other way round, so no deadlock between the two is possible.
    private final Object sync = new Object();

    // The synchronization state. Its meaning is set by the subclass.
    private long state;

    // The FIFO queue of waiters, and its length (so as not to walk it when asked).
    private SyncWaiter first;
    private SyncWaiter last;
    private int queuedThreads;

    // Whether anybody ever had to queue. It is exactly `hasContended()`.
    private boolean was;

    /** An initial `state` of zero. For subclasses' use only. */
    public AbstractQueuedLongSynchronizer() {
    }

    // ---- the state -------------------------------------------------------------------------

    /** The synchronization state's current value. */
    protected final long getState() {
        long s;
        synchronized (sync) {
            s = state;
        }
        return s;
    }

    /** It sets the synchronization state. */
    protected final void setState(long newState) {
        synchronized (sync) {
            state = newState;
        }
    }

    /**
     * It sets the state to `update` **if and only if** it holds `expect`, in one indivisible step.
     *
     * @return `true` if it changed it
     */
    protected final boolean compareAndSetState(long expect, long update) {
        boolean ok;
        synchronized (sync) {
            ok = state == expect;
            if (ok) {
                state = update;
            }
        }
        return ok;
    }

    // ---- what the subclass implements --------------------------------------------------------
    //
    // Concrete and not abstract, as in the JDK: a subclass implements only the mode it uses, and the
    // other pair has to remain instantiable. They throw `UnsupportedOperationException` because
    // getting here means somebody asked for a mode the subclass does not support -- and that is the
    // right answer, not `false`, which would read as "I could not yet" and would queue the thread
    // forever.

    /** It tries to acquire in exclusive mode. */
    protected boolean tryAcquire(long arg) {
        throw new UnsupportedOperationException();
    }

    /** It tries to release in exclusive mode; `true` if the synchronizer was left free. */
    protected boolean tryRelease(long arg) {
        throw new UnsupportedOperationException();
    }

    /** It tries to acquire in shared mode; negative if one has to wait. */
    protected long tryAcquireShared(long arg) {
        throw new UnsupportedOperationException();
    }

    /** It tries to release in shared mode; `true` if it may let some waiter through. */
    protected boolean tryReleaseShared(long arg) {
        throw new UnsupportedOperationException();
    }

    /** Whether the current thread holds it exclusively. Only `ConditionObject` needs it. */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    // ---- exclusive mode ---------------------------------------------------------------------

    /**
     * It acquires in exclusive mode, **without** honouring interruptions.
     *
     * <p>If an interruption arrives while waiting it is neither lost nor does it cut the
     * acquisition short: it is noted and the thread's flag is set again before returning. Aborting
     * here would leave the caller without the lock and believing it holds it.
     */
    public final void acquire(long arg) {
        if (!tryAcquire(arg)) {
            if (awaitOn(arg, false, false, false, 0L) == ACQUIRED_INTERRUPTED) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * It acquires in exclusive mode, aborting if the thread is interrupted.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public final void acquireInterruptibly(long arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!tryAcquire(arg)) {
            if (awaitOn(arg, false, true, false, 0L) == INTERRUPTED) {
                throw new InterruptedException();
            }
        }
    }

    /**
     * It acquires in exclusive mode, waiting at most `nanosTimeout`.
     *
     * @return `false` if the deadline ran out without acquiring
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public final boolean tryAcquireNanos(long arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        boolean ok = tryAcquire(arg);
        if (!ok) {
            int r = awaitOn(arg, false, true, true, System.nanoTime() + nanosTimeout);
            if (r == INTERRUPTED) {
                throw new InterruptedException();
            }
            ok = r != TIMED_OUT;
        }
        return ok;
    }

    /**
     * It releases in exclusive mode and, if the synchronizer was left free, hands the turn to the
     * queue's first.
     *
     * @return whatever `tryRelease` returned
     */
    public final boolean release(long arg) {
        boolean free = tryRelease(arg);
        if (free) {
            signalFirst();
        }
        return free;
    }

    // ---- shared mode ---------------------------------------------------------------------

    /** It acquires in shared mode, without honouring interruptions. */
    public final void acquireShared(long arg) {
        if (tryAcquireShared(arg) < 0L) {
            if (awaitOn(arg, true, false, false, 0L) == ACQUIRED_INTERRUPTED) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * It acquires in shared mode, aborting if the thread is interrupted.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public final void acquireSharedInterruptibly(long arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(arg) < 0L) {
            if (awaitOn(arg, true, true, false, 0L) == INTERRUPTED) {
                throw new InterruptedException();
            }
        }
    }

    /**
     * It acquires in shared mode, waiting at most `nanosTimeout`.
     *
     * @return `false` if the deadline ran out without acquiring
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public final boolean tryAcquireSharedNanos(long arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        boolean ok = tryAcquireShared(arg) >= 0L;
        if (!ok) {
            int r = awaitOn(arg, true, true, true, System.nanoTime() + nanosTimeout);
            if (r == INTERRUPTED) {
                throw new InterruptedException();
            }
            ok = r != TIMED_OUT;
        }
        return ok;
    }

    /** It releases in shared mode and wakes the queue's first. */
    public final boolean releaseShared(long arg) {
        boolean step = tryReleaseShared(arg);
        if (step) {
            signalFirst();
        }
        return step;
    }

    // ---- the engine: queue up, sleep, retry --------------------------------------------------

    /**
     * The only place this class waits. It queues the current thread and does not return until it
     * has acquired, been interrupted (and that was asked to cut the wait short), or the deadline has
     * expired.
     *
     * <p>The subclass's `try*` is called **without** the internal monitor held -- otherwise a
     * `compareAndSetState` inside it would take it again and, worse, foreign code would be running
     * with one of the synchronizer's locks in hand.
     */
    private int awaitOn(long arg, boolean shared, boolean interruptible, boolean timedWait,
                        long endNanos) {
        SyncWaiter node = enqueue(shared);
        int result = -1;
        boolean wasInterrupted = false;
        while (result < 0) {
            boolean logrado = false;
            if (isFirst(node)) {
                logrado = shared ? tryAcquireShared(arg) >= 0L : tryAcquire(arg);
            }
            if (logrado) {
                dequeue(node);
                result = wasInterrupted ? ACQUIRED_INTERRUPTED : ACQUIRED;
            } else if (timedWait && endNanos - System.nanoTime() <= 0L) {
                dequeue(node);
                result = TIMED_OUT;
            } else if (sleepOn(node, timedWait, endNanos)) {
                if (interruptible) {
                    dequeue(node);
                    result = INTERRUPTED;
                } else {
                    // Non-interruptible: it is noted and the wait goes on. The flag is set again
                    // above, when the acquisition finishes.
                    wasInterrupted = true;
                }
            }
        }
        return result;
    }

    /**
     * It sleeps on the node's monitor until it is released (or the deadline expires, or it is
     * interrupted).
     *
     * @return `true` if an interruption cut it short
     */
    private boolean sleepOn(SyncWaiter node, boolean timedWait, long endNanos) {
        boolean cutShort = false;
        synchronized (node) {
            // Checking the permit and falling asleep happen inside the same monitor the releaser
            // uses: that is why there is no lost wake-up.
            if (!node.released) {
                try {
                    if (timedWait) {
                        long remain = endNanos - System.nanoTime();
                        if (remain > 0L) {
                            long ms = remain / 1000000L;
                            // A `wait(0)` waits forever; a deadline of less than a millisecond is
                            // rounded up to the smallest that can be asked for.
                            if (ms <= 0L) {
                                ms = 1L;
                            }
                            node.wait(ms);
                        }
                    } else {
                        node.wait();
                    }
                } catch (InterruptedException e) {
                    cutShort = true;
                }
            }
            node.released = false;
        }
        return cutShort;
    }

    // ---- the queue ----------------------------------------------------------------------------

    private SyncWaiter enqueue(boolean shared) {
        SyncWaiter node = new SyncWaiter();
        node.thread = Thread.currentThread();
        node.shared = shared;
        synchronized (sync) {
            node.inQueue = true;
            node.prev = last;
            if (last == null) {
                first = node;
            } else {
                last.next = node;
            }
            last = node;
            queuedThreads++;
            was = true;
        }
        return node;
    }

    private boolean isFirst(SyncWaiter node) {
        boolean r;
        synchronized (sync) {
            r = first == node;
        }
        return r;
    }

    /**
     * It takes the node out of the queue and hands the turn to whoever is left first.
     *
     * <p>It is called both when the node **managed** to acquire and when it **gives up** (deadline
     * or interruption), and in both cases the new first has to be signalled: in the first because in
     * shared mode there may be room left for the next, and in the second because the one leaving may
     * have been the only one entitled to try. Not signalling in the give-up case leaves the queue
     * stuck -- it is this frame's classic mistake.
     */
    private void dequeue(SyncWaiter node) {
        SyncWaiter newFirst = null;
        synchronized (sync) {
            if (node.inQueue) {
                node.inQueue = false;
                if (node.prev == null) {
                    first = node.next;
                } else {
                    node.prev.next = node.next;
                }
                if (node.next == null) {
                    last = node.prev;
                } else {
                    node.next.prev = node.prev;
                }
                node.next = null;
                node.prev = null;
                node.thread = null;
                queuedThreads--;
                newFirst = first;
            }
        }
        if (newFirst != null) {
            signalOne(newFirst);
        }
    }

    private void signalFirst() {
        SyncWaiter n;
        synchronized (sync) {
            n = first;
        }
        if (n != null) {
            signalOne(n);
        }
    }

    // It leaves the permit on the node and wakes it. `notifyAll` and not `notify` because the same
    // monitor is used by the `ConditionObject` for its own wait.
    private void signalOne(SyncWaiter node) {
        synchronized (node) {
            node.released = true;
            node.notifyAll();
        }
    }

    // ---- queue inspection --------------------------------------------------------------------
    //
    // They are all **snapshots**, and the JDK's javadoc insists on it: they serve for diagnostics
    // and for heuristics, never for deciding. By the time the answer arrives, the queue may be
    // another.

    /** Whether some thread is waiting to acquire. */
    public final boolean hasQueuedThreads() {
        boolean any;
        synchronized (sync) {
            any = first != null;
        }
        return any;
    }

    /** Whether **anybody** ever had to queue. It never goes back to `false`. */
    public final boolean hasContended() {
        boolean h;
        synchronized (sync) {
            h = was;
        }
        return h;
    }

    /** The queue's first, or `null` if it is empty. */
    public final Thread getFirstQueuedThread() {
        Thread t;
        synchronized (sync) {
            t = first == null ? null : first.thread;
        }
        return t;
    }

    /**
     * Whether that thread is in the queue.
     *
     * @throws NullPointerException if `thread` is `null`
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null) {
            throw new NullPointerException("thread");
        }
        boolean present = false;
        synchronized (sync) {
            SyncWaiter n = first;
            while (n != null && !present) {
                present = n.thread == thread;
                n = n.next;
            }
        }
        return present;
    }

    /**
     * Whether some thread is waiting **ahead of** the current one.
     *
     * <p>It is the query a fair subclass makes inside its `tryAcquire`: acquiring only if it
     * returns `false` turns the "whoever arrives cuts in" policy into strict FIFO.
     */
    public final boolean hasQueuedPredecessors() {
        Thread self = Thread.currentThread();
        boolean any;
        synchronized (sync) {
            any = first != null && first.thread != self;
        }
        return any;
    }

    /** How many are waiting to acquire. */
    public final int getQueueLength() {
        int n;
        synchronized (sync) {
            n = queuedThreads;
        }
        return n;
    }

    /** The threads waiting to acquire. A copy; the internal queue does not leave here. */
    public final Collection<Thread> getQueuedThreads() {
        return this.collect(false, false);
    }

    /** Those waiting in exclusive mode. */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        return this.collect(true, false);
    }

    /** Those waiting in shared mode. */
    public final Collection<Thread> getSharedQueuedThreads() {
        return this.collect(true, true);
    }

    private Collection<Thread> collect(boolean filterBy, boolean shared) {
        ArrayList<Thread> out = new ArrayList<Thread>();
        synchronized (sync) {
            SyncWaiter n = first;
            while (n != null) {
                if ((!filterBy || n.shared == shared) && n.thread != null) {
                    out.add(n.thread);
                }
                n = n.next;
            }
        }
        return out;
    }

    // ---- condition inspection ----------------------------------------------------------------
    //
    // All four demand that the condition belong to **this** synchronizer. Asking one about a
    // foreign condition has no right answer, and returning "none" would be worse than failing.

    /**
     * Whether that condition was created on this synchronizer.
     *
     * @throws NullPointerException if `condition` is `null`
     */
    public final boolean owns(ConditionObject condition) {
        if (condition == null) {
            throw new NullPointerException("condition");
        }
        return condition.belongsTo(this);
    }

    /**
     * Whether anybody is waiting on that condition.
     *
     * @throws IllegalMonitorStateException if the current thread does not hold it exclusively
     * @throws IllegalArgumentException if the condition does not belong to this synchronizer
     */
    public final boolean hasWaiters(ConditionObject condition) {
        return this.ownCondition(condition).anyWaiting();
    }

    /**
     * How many are waiting on that condition.
     *
     * @throws IllegalMonitorStateException if the current thread does not hold it exclusively
     * @throws IllegalArgumentException if the condition does not belong to this synchronizer
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        return this.ownCondition(condition).waitingCount();
    }

    /**
     * The threads waiting on that condition.
     *
     * @throws IllegalMonitorStateException if the current thread does not hold it exclusively
     * @throws IllegalArgumentException if the condition does not belong to this synchronizer
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        return this.ownCondition(condition).theWaiters();
    }

    private ConditionObject ownCondition(ConditionObject condition) {
        if (condition == null) {
            throw new NullPointerException("condition");
        }
        if (!condition.belongsTo(this)) {
            throw new IllegalArgumentException("not owner");
        }
        if (!isHeldExclusively()) {
            throw new IllegalMonitorStateException();
        }
        return condition;
    }

    // The synchronizer's `this`, seen from the inner class. It exists because the
    // `ConditionObject` needs to compare its owner and to call `acquire`/`release`, and an
    // unqualified call from inside the inner class already resolves to the outer one.
    AbstractQueuedLongSynchronizer thisSynchronizer() {
        return this;
    }

    // =========================================================================================
    // The condition
    // =========================================================================================

    /**
     * A {@link Condition} over a synchronizer in exclusive mode.
     *
     * <p>The protocol is the usual one: `await` **releases the whole synchronizer** --whatever the
     * reentrant depth, saving the `state` to restore it-- and sleeps; `signal` wakes one, which
     * acquires again before returning. Each waiter sleeps on its own node, just as in the
     * acquisition queue, with the same guarantee against the lost wake-up.
     */
    public class ConditionObject implements Condition, Serializable {

        // Those waiting on this condition, in arrival order. It is guarded by the list's monitor
        // (`queue`), which is different from the synchronizer's internal monitor and from each
        // node's -- and it is never taken while holding either of those two.
        private final ArrayList<SyncWaiter> queue = new ArrayList<SyncWaiter>();

        public ConditionObject() {
        }

        /**
         * It releases the synchronizer and waits until it is signalled.
         *
         * @throws InterruptedException if the thread is interrupted while waiting
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            SyncWaiter node = this.addTo();
            long savedState = this.releaseAllState();
            boolean cutShort = false;
            boolean signalled = false;
            while (!signalled && !cutShort) {
                synchronized (node) {
                    if (!node.released) {
                        try {
                            node.wait();
                        } catch (InterruptedException e) {
                            cutShort = true;
                        }
                    }
                    signalled = node.released;
                }
            }
            this.removeFrom(node);
            thisSynchronizer().acquire(savedState);
            if (cutShort) {
                // If the signal also arrived, the interruption cannot be thrown without losing it:
                // the flag is set again and the caller will see it at their next wait. It is the
                // split the JDK makes between THROW_IE and REINTERRUPT.
                if (signalled) {
                    Thread.currentThread().interrupt();
                } else {
                    throw new InterruptedException();
                }
            }
        }

        /**
         * It waits **without** being interruptible.
         *
         * <p>The interruption is not lost: it is caught, the wait goes on, and at the end the
         * thread's flag is set again.
         */
        public final void awaitUninterruptibly() {
            SyncWaiter node = this.addTo();
            long savedState = this.releaseAllState();
            boolean wasInterrupted = false;
            boolean signalled = false;
            while (!signalled) {
                synchronized (node) {
                    if (!node.released) {
                        try {
                            node.wait();
                        } catch (InterruptedException e) {
                            wasInterrupted = true;
                        }
                    }
                    signalled = node.released;
                }
            }
            this.removeFrom(node);
            thisSynchronizer().acquire(savedState);
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * It waits with a deadline in nanoseconds and returns **what was left over**.
         *
         * <p>Returning the remainder and not a `boolean` is what makes it useful in a loop: a wait
         * can wake with no signal and has to wait again, but only for the rest.
         *
         * @return the nanoseconds left over; zero or less if the deadline ran out
         * @throws InterruptedException if the thread is interrupted while waiting
         */
        public final long awaitNanos(long nanosTimeout) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            long end = System.nanoTime() + nanosTimeout;
            SyncWaiter node = this.addTo();
            long savedState = this.releaseAllState();
            boolean cutShort = false;
            boolean signalled = false;
            boolean expired = false;
            while (!signalled && !cutShort && !expired) {
                synchronized (node) {
                    if (!node.released) {
                        long remain = end - System.nanoTime();
                        if (remain <= 0L) {
                            expired = true;
                        } else {
                            long ms = remain / 1000000L;
                            if (ms <= 0L) {
                                ms = 1L;
                            }
                            try {
                                node.wait(ms);
                            } catch (InterruptedException e) {
                                cutShort = true;
                            }
                        }
                    }
                    signalled = node.released;
                }
                if (!signalled && !cutShort && end - System.nanoTime() <= 0L) {
                    expired = true;
                }
            }
            this.removeFrom(node);
            thisSynchronizer().acquire(savedState);
            if (cutShort) {
                if (signalled) {
                    Thread.currentThread().interrupt();
                } else {
                    throw new InterruptedException();
                }
            }
            return end - System.nanoTime();
        }

        /**
         * It waits with a deadline.
         *
         * @return `false` if the deadline ran out before the signal
         * @throws InterruptedException if the thread is interrupted while waiting
         */
        public final boolean await(long time, TimeUnit unit) throws InterruptedException {
            if (unit == null) {
                throw new NullPointerException("unit");
            }
            return this.awaitNanos(unit.toNanos(time)) > 0L;
        }

        /**
         * It waits until a date.
         *
         * <p>Here the wall clock **is** used, and it has to be: the deadline is expressed as a
         * moment on the calendar, not as a duration. The consequence is the contract's: if somebody
         * moves the system clock, this wait moves with it.
         *
         * @return `false` if the date arrived before the signal
         * @throws InterruptedException if the thread is interrupted while waiting
         */
        public final boolean awaitUntil(java.util.Date deadline) throws InterruptedException {
            if (deadline == null) {
                throw new NullPointerException("deadline");
            }
            long remaining = deadline.getTime() - System.currentTimeMillis();
            if (remaining <= 0L) {
                return false;
            }
            this.awaitNanos(remaining * 1000000L);
            return System.currentTimeMillis() < deadline.getTime();
        }

        /**
         * It wakes whoever has been waiting longest.
         *
         * @throws IllegalMonitorStateException if the current thread does not hold it exclusively
         */
        public final void signal() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            SyncWaiter n = null;
            synchronized (queue) {
                if (!queue.isEmpty()) {
                    n = queue.get(0);
                }
            }
            if (n != null) {
                synchronized (n) {
                    n.released = true;
                    n.notifyAll();
                }
            }
        }

        /**
         * It wakes everyone waiting.
         *
         * @throws IllegalMonitorStateException if the current thread does not hold it exclusively
         */
        public final void signalAll() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            ArrayList<SyncWaiter> copy;
            synchronized (queue) {
                copy = new ArrayList<SyncWaiter>(queue);
            }
            for (int i = 0; i < copy.size(); i++) {
                SyncWaiter n = copy.get(i);
                synchronized (n) {
                    n.released = true;
                    n.notifyAll();
                }
            }
        }

        // ---- the machinery ------------------------------------------------------------------

        private SyncWaiter addTo() {
            SyncWaiter node = new SyncWaiter();
            node.thread = Thread.currentThread();
            synchronized (queue) {
                queue.add(node);
            }
            return node;
        }

        private void removeFrom(SyncWaiter node) {
            synchronized (queue) {
                queue.remove(node);
            }
            node.thread = null;
        }

        /**
         * It releases the **whole** synchronizer, saving the `state` to restore it afterwards.
         *
         * @throws IllegalMonitorStateException if the current thread does not hold it
         */
        private long releaseAllState() {
            long savedState = getState();
            if (!thisSynchronizer().release(savedState)) {
                throw new IllegalMonitorStateException();
            }
            return savedState;
        }

        // ---- what the synchronizer needs in order to answer its queries ---------------------

        boolean belongsTo(AbstractQueuedLongSynchronizer other) {
            return thisSynchronizer() == other;
        }

        boolean anyWaiting() {
            boolean any;
            synchronized (queue) {
                any = !queue.isEmpty();
            }
            return any;
        }

        int waitingCount() {
            int n;
            synchronized (queue) {
                n = queue.size();
            }
            return n;
        }

        Collection<Thread> theWaiters() {
            ArrayList<Thread> out = new ArrayList<Thread>();
            synchronized (queue) {
                for (int i = 0; i < queue.size(); i++) {
                    Thread t = queue.get(i).thread;
                    if (t != null) {
                        out.add(t);
                    }
                }
            }
            return out;
        }
    }
}
