package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

// A reentrant mutual-exclusion lock — the explicit-lock counterpart of a `synchronized`
// block. The JDK builds this on the AbstractQueuedSynchronizer; KajiJDK builds it
// directly on the intrinsic monitor of a private `sync` object plus `Thread.currentThread()`
// for owner identity: a `wait`/`notify` handshake on `sync` serializes acquisition and the owner
// field makes it reentrant.
//
// This header used to justify that with "there is no true parallelism to make the coarse guard
// observably differ from a lock-free one". That reason is gone: `JVM_THREADS=os` is a real
// OS-thread substrate. The design stands on a better one -- an intrinsic monitor is a real mutex
// under real parallelism too, only a coarser one -- and on measurement: the five behavioural tests
// built on this lock (`CountDownLatch`, `CyclicBarrier`, `ArrayBlockingQueue`, `DelayQueue`,
// `Semaphore`) pass on all three substrates, the parallel one included. What is given up is
// scalability, which the contract does not promise, not correctness.
//
// NOTE on style: every method is written single-exit — no `return` from inside a
// `synchronized (sync)` block. Finding #105: the frozen javac does not emit the
// `monitorexit` that a `return` inside a synchronized block requires (it handles only the
// fall-through and exceptional exits), so an early return would leak the monitor. A `throw`
// inside the block is safe — the compiler-generated exception handler releases it.
public class ReentrantLock implements Lock, Serializable {

    // The monitor guarding all state below and the acquire/release handshake.
    private final Object sync = new Object();
    // The thread that holds the lock, or null when free.
    private Thread owner;
    // Reentrant acquisition count (0 when free).
    private int holdCount;
    // The threads blocked trying to acquire. It used to be a counter; the list is needed because
    // `getQueuedThreads` asks for the threads, and the count comes out of it.
    private final java.util.ArrayList<Thread> queuedThreads = new java.util.ArrayList<Thread>();
    private int queued;
    // Fairness flag. On the cooperative scheduler acquisition is already close to FIFO
    // via the monitor wait-set; the flag is honoured as state but does not change policy.
    private final boolean fair;

    public ReentrantLock() {
        this.fair = false;
    }

    public ReentrantLock(boolean fair) {
        this.fair = fair;
    }

    public void lock() {
        Thread me = Thread.currentThread();
        boolean wasInterrupted = false;
        synchronized (sync) {
            if (owner == me) {
                holdCount++;
            } else {
                if (owner != null) {
                    queued++;
                    queuedThreads.add(Thread.currentThread());
                    while (owner != null) {
                    // Non-interruptible (`Lock.lock()`'s contract): it is caught, the wait goes
                    // on, and the thread is re-marked at the end. Aborting here would leave the
                    // lock half acquired.
                        try {
                            sync.wait();
                        } catch (InterruptedException e) {
                            wasInterrupted = true;
                        }
                    }
                    queued--;
                    queuedThreads.remove(Thread.currentThread());
                }
                owner = me;
                holdCount = 1;
            }
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * It acquires the lock, **aborting if the thread is interrupted**.
     *
     * <p>It is the counterpart of `lock()`, which does not abort. The note that used to be here said
     * the VM had no thread interruption and that this method was identical to `lock()`; both stopped
     * being true -- `Thread.interrupt()` exists and wakes the waits, so this method can do what its
     * name promises.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void lockInterruptibly() throws InterruptedException {
        Thread me = Thread.currentThread();
        // It is checked **before** waiting: an already interrupted thread must not enter the wait.
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        synchronized (sync) {
            if (owner == me) {
                holdCount++;
            } else {
                if (owner != null) {
                    queued++;
                    queuedThreads.add(me);
                    try {
                        while (owner != null) {
                            sync.wait();
                        }
                    } finally {
                        // The `finally` matters: if the wait is cut short by an interruption, the
                        // thread has to leave the queue all the same. Without this, a later
                        // `getQueuedThreads` would show a thread that is no longer waiting for
                        // anything.
                        queued--;
                        queuedThreads.remove(me);
                    }
                }
                owner = me;
                holdCount = 1;
            }
        }
    }

    public boolean tryLock() {
        Thread me = Thread.currentThread();
        boolean acquired;
        synchronized (sync) {
            if (owner == null) {
                owner = me;
                holdCount = 1;
                acquired = true;
            } else if (owner == me) {
                holdCount++;
                acquired = true;
            } else {
                acquired = false;
            }
        }
        return acquired;
    }

    /**
     * It acquires the lock, waiting at most that deadline.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        Thread me = Thread.currentThread();
        boolean acquired;
        synchronized (sync) {
            if (owner == null) {
                owner = me;
                holdCount = 1;
                acquired = true;
            } else if (owner == me) {
                holdCount++;
                acquired = true;
            } else {
                long ms = unit.toMillis(time);
                if (ms <= 0L) {
                    acquired = false;
                } else {
                    // With the **remaining deadline**, not a single attempt. The previous note said
                    // the library had no clock to recompute with; `System.nanoTime()` exists. Without
                    // the loop, a wait that woke for another reason --another thread releasing and
                    // retaking the lock-- returned `false` with the whole deadline still ahead.
                    long endNanos = System.nanoTime() + ms * 1000000L;
                    queued++;
                    queuedThreads.add(me);
                    try {
                        long remainingNanos = endNanos - System.nanoTime();
                        while (owner != null && remainingNanos > 0L) {
                            sync.wait(remainingNanos / 1000000L, (int) (remainingNanos % 1000000L));
                            remainingNanos = endNanos - System.nanoTime();
                        }
                    } finally {
                        queued--;
                        queuedThreads.remove(me);
                    }
                    if (owner == null) {
                        owner = me;
                        holdCount = 1;
                        acquired = true;
                    } else {
                        acquired = false;
                    }
                }
            }
        }
        return acquired;
    }

    public void unlock() {
        Thread me = Thread.currentThread();
        synchronized (sync) {
            if (owner != me) {
                // Safe inside the block: the exception path releases the monitor.
                throw new IllegalMonitorStateException();
            }
            holdCount--;
            if (holdCount == 0) {
                owner = null;
                sync.notify();
            }
        }
    }

    public Condition newCondition() {
        return new ReentrantCondition(this);
    }

    public int getHoldCount() {
        int held;
        synchronized (sync) {
            held = owner == Thread.currentThread() ? holdCount : 0;
        }
        return held;
    }

    public boolean isHeldByCurrentThread() {
        boolean held;
        synchronized (sync) {
            held = owner == Thread.currentThread();
        }
        return held;
    }

    public boolean isLocked() {
        boolean locked;
        synchronized (sync) {
            locked = owner != null;
        }
        return locked;
    }

    public final boolean isFair() {
        return fair;
    }

    protected Thread getOwner() {
        Thread o;
        synchronized (sync) {
            o = owner;
        }
        return o;
    }

    public final boolean hasQueuedThreads() {
        boolean any;
        synchronized (sync) {
            any = queued > 0;
        }
        return any;
    }

    /**
     * Whether that thread is waiting to acquire this lock.
     *
     * <p>It is a **snapshot**, and the JDK's javadoc insists on it: the thread may have acquired or
     * given up by the time the answer arrives. It serves for diagnostics, not for deciding.
     *
     * @throws NullPointerException if `thread` is `null`
     */
    public final boolean hasQueuedThread(Thread thread) {
        if (thread == null) {
            throw new NullPointerException("thread");
        }
        boolean present;
        synchronized (sync) {
            present = queuedThreads.contains(thread);
        }
        return present;
    }

    /** The threads waiting to acquire. A copy: the internal list does not leave here. */
    protected java.util.Collection<Thread> getQueuedThreads() {
        java.util.ArrayList<Thread> copy;
        synchronized (sync) {
            copy = new java.util.ArrayList<Thread>(queuedThreads);
        }
        return copy;
    }

    // ---- condition inspection ----------------------------------------------------------------------
    //
    // All three demand that the condition belong to **this lock**: asking a lock about a foreign
    // condition has no right answer, and returning "none" would be worse than failing.

    private ReentrantCondition ownCondition(Condition condition) {
        if (condition == null) {
            throw new NullPointerException("condition");
        }
        if (!(condition instanceof ReentrantCondition)) {
            throw new IllegalArgumentException("not owner");
        }
        ReentrantCondition c = (ReentrantCondition) condition;
        if (!c.belongsTo(this)) {
            throw new IllegalArgumentException("not owner");
        }
        return c;
    }

    /** Whether anybody is waiting on that condition of this lock. */
    public boolean hasWaiters(Condition condition) {
        return this.ownCondition(condition).anyWaiting();
    }

    /** How many are waiting on that condition of this lock. */
    public int getWaitQueueLength(Condition condition) {
        return this.ownCondition(condition).waitingCount();
    }

    /** The threads waiting on that condition of this lock. */
    protected java.util.Collection<Thread> getWaitingThreads(Condition condition) {
        return this.ownCondition(condition).theWaiters();
    }

    public final int getQueueLength() {
        int n;
        synchronized (sync) {
            n = queued;
        }
        return n;
    }

    // (toString is omitted: the JDK's builds on super.toString(), which the bytecode
    // generator does not support yet — a subset is fine for the gate.)

    // --- package-private seam for ReentrantCondition.await/signal ---

    // Fully release the lock (whatever the reentrant depth), returning the saved count so
    // the awaiter can restore it on re-acquisition. Wakes one blocked acquirer. Caller
    // must own the lock.
    int fullyRelease() {
        Thread me = Thread.currentThread();
        int saved;
        synchronized (sync) {
            if (owner != me) {
                throw new IllegalMonitorStateException();
            }
            saved = holdCount;
            owner = null;
            holdCount = 0;
            sync.notify();
        }
        return saved;
    }

    // Re-acquire the lock after an await, restoring the saved reentrant count.
    void reacquire(int holds) {
        Thread me = Thread.currentThread();
        boolean wasInterrupted = false;
        synchronized (sync) {
            if (owner != me) {
                while (owner != null) {
                    try {
                        sync.wait();
                    } catch (InterruptedException e) {
                        wasInterrupted = true;
                    }
                }
            }
            owner = me;
            holdCount = holds;
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
