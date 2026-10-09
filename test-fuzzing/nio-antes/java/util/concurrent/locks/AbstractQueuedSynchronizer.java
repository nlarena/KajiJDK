package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

// The frame the JDK builds almost all of its synchronizers on. The subclass keeps its meaning in
// an `int state` --which it touches only through the three accessors here-- and implements the
// `try*` methods; this gives everything else: the wait queue, the blocking, the waking, the
// interruption and the deadline. Two modes:
//
//   exclusive (`acquire`/`release`)          — one owner at a time, like a `ReentrantLock`.
//   shared    (`acquireShared`/`releaseShared`) — several at once, like a `Semaphore` or a
//                                                `CountDownLatch`.
//
// ---------------------------------------------------------------------------------------------
// THE DECISION: `ReentrantLock` and `ReentrantReadWriteLock` were NOT rebuilt on this class
// ---------------------------------------------------------------------------------------------
//
// In the JDK both lean on AQS. Here they do not, and it stays that way on purpose. The reasons, in
// order of weight:
//
//  1. **The relationship is invisible from the contract.** In the JDK, AQS enters `ReentrantLock`
//     through a nested `private static class Sync extends AbstractQueuedSynchronizer`. No `public`
//     or `protected` member of `ReentrantLock` mentions it, returns it, or takes it. Which is to
//     say that "being built on AQS" is not part of what the package promises: it is an
//     implementation choice, and the house rule says the internals are free.
//
//  2. **What is there today works, and works measured.** `ReentrantLock` is 20/20 and
//     `ReentrantReadWriteLock` 22/22, and half of `java.util.concurrent` hangs off them:
//     `CountDownLatch`, `CyclicBarrier`, `ArrayBlockingQueue`, `DelayQueue`, `Semaphore`. That is
//     five behavioural tests that pass today with all three thread substrates, the real parallel
//     one included. Rewriting them on a newborn class risks all five **without moving the number
//     or a member**: both locks' contracts are already complete.
//
//  3. **That does not leave this class as untested scaffolding.** `java/AqsLockTest.java` builds
//     the mutex from AQS's own javadoc (`tryAcquire` with `compareAndSetState`, `tryRelease` with
//     `setState`) and does 300 guarded non-atomic increments; `java/AqsSharedTest.java` covers the
//     shared mode, the deadline, the interruption and the queue queries. Both run on both VMs. The
//     `ConditionObject` is tested in `scratchpad/zzlocks/AqsCondProbe.java`, which is there and not
//     in `java/` because the frozen javac cannot instantiate an inherited inner class (repro with
//     ablation in `scratchpad/zzlocks/InnerHer.java`); it is compiled with the JDK's javac and the
//     `.class` is run on both VMs like the others.
//
//     And there is a stronger check than those three: this package's `.class` files can be put into
//     the real JVM with `--patch-module java.base=<dir>`, and the three tests give the same thing
//     there -- which is to say this AQS also runs on operating-system threads and real parallelism,
//     not only on our scheduler.
//
// The argument on the other side --that rebuilding them would be more faithful-- is true and is not
// enough: the fidelity gained cannot be observed from outside, and the fidelity risked can.
//
// ---------------------------------------------------------------------------------------------
// HOW IT BLOCKS, AND WHY NOT WITH `LockSupport.park`
// ---------------------------------------------------------------------------------------------
//
// The JDK parks its waiters with `LockSupport.park`. Here each queue node is **its own monitor**:
// the thread sleeps in `node.wait()` and whoever releases it does
// `synchronized (node) { node.released = true; node.notifyAll(); }`. The node's permit
// (`released`) gives the same guarantee as `park`'s --a signal arriving before the thread falls
// asleep is not lost, because both things happen inside the same monitor-- and it also gives the
// two `park` cannot give on this VM:
//
//   - **a deadline**: `Object.wait(ms)` exists; a `park` with a deadline does not. Without this
//     there would be neither `tryAcquireNanos` nor `tryAcquireSharedNanos`.
//   - **interruption with the right shape**: `wait` throws `InterruptedException`, which is what
//     `acquireInterruptibly` needs. Our `park`, interrupted, throws too -- but it throws *always*,
//     including where the contract says it has to return, and without declaring it (see
//     `LockSupport`'s header). A non-interruptible `acquire(int)` built on it would die with an
//     undeclared `InterruptedException`.
//
// `LockSupport` stays exactly what it is --the VM's primitive, complete and tested-- and this class
// does not lean on it. It is an internal detail; the contract does not say what one sleeps with.
//
// ---------------------------------------------------------------------------------------------
// THE QUEUE
// ---------------------------------------------------------------------------------------------
//
// A doubly linked FIFO, guarded by an internal monitor (`sync`), and **only the queue's first tries
// to acquire**. That is what makes `getFirstQueuedThread`, `hasQueuedPredecessors` and
// `getQueueLength` tell the truth: over a Treiber stack --which would be shorter to write-- "the
// first" means nothing. That a **newly arrived** thread can cut in ahead of the queue is not a
// defect but the JDK's behaviour: `acquire` tries `tryAcquire` before queueing, and a subclass that
// wants to be fair avoids it by consulting `hasQueuedPredecessors`.
//
// A note on style, the same as in `ReentrantLock`: **no `return` inside a `synchronized` block**
// (finding #105 -- the frozen javac does not emit that exit's `monitorexit` and the monitor is
// leaked). Everything is computed into a local inside and returned outside. A `throw` inside IS
// safe: the handler the compiler generates releases it.
public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer
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
    private int state;

    // The FIFO queue of waiters, and its length (so as not to walk it when asked).
    private SyncWaiter first;
    private SyncWaiter last;
    private int queuedThreads;

    // Whether anybody ever had to queue. It is exactly `hasContended()`.
    private boolean was;

    /** An initial `state` of zero. For subclasses' use only. */
    protected AbstractQueuedSynchronizer() {
    }

    // ---- the state -------------------------------------------------------------------------

    /** The synchronization state's current value. */
    protected final int getState() {
        int s;
        synchronized (sync) {
            s = state;
        }
        return s;
    }

    /** It sets the synchronization state. */
    protected final void setState(int newState) {
        synchronized (sync) {
            state = newState;
        }
    }

    /**
     * It sets the state to `update` **if and only if** it holds `expect`, in one indivisible step.
     *
     * @return `true` if it changed it
     */
    protected final boolean compareAndSetState(int expect, int update) {
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
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /** It tries to release in exclusive mode; `true` if the synchronizer was left free. */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /** It tries to acquire in shared mode; negative if one has to wait. */
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /** It tries to release in shared mode; `true` if it may let some waiter through. */
    protected boolean tryReleaseShared(int arg) {
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
    public final void acquire(int arg) {
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
    public final void acquireInterruptibly(int arg) throws InterruptedException {
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
    public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
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
    public final boolean release(int arg) {
        boolean free = tryRelease(arg);
        if (free) {
            signalFirst();
        }
        return free;
    }

    // ---- shared mode ---------------------------------------------------------------------

    /** It acquires in shared mode, without honouring interruptions. */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0) {
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
    public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(arg) < 0) {
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
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        boolean ok = tryAcquireShared(arg) >= 0;
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
    public final boolean releaseShared(int arg) {
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
    private int awaitOn(int arg, boolean shared, boolean interruptible, boolean timedWait,
                        long endNanos) {
        SyncWaiter node = enqueue(shared);
        int result = -1;
        boolean wasInterrupted = false;
        while (result < 0) {
            boolean logrado = false;
            if (isFirst(node)) {
                logrado = shared ? tryAcquireShared(arg) >= 0 : tryAcquire(arg);
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
    AbstractQueuedSynchronizer thisSynchronizer() {
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
            int savedState = this.releaseAllState();
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
            int savedState = this.releaseAllState();
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
            int savedState = this.releaseAllState();
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
        private int releaseAllState() {
            int savedState = getState();
            if (!thisSynchronizer().release(savedState)) {
                throw new IllegalMonitorStateException();
            }
            return savedState;
        }

        // ---- what the synchronizer needs in order to answer its queries ---------------------

        boolean belongsTo(AbstractQueuedSynchronizer other) {
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
