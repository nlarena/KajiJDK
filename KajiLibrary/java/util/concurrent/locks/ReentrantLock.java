package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

// A reentrant mutual-exclusion lock — the explicit-lock counterpart of a `synchronized`
// block. The JDK builds this on the AbstractQueuedSynchronizer; KajiJDK builds it
// directly on the intrinsic monitor of a private `sync` object plus `Thread.currentThread()`
// for owner identity. On the single-carrier cooperative scheduler this is faithful: a
// `wait`/`notify` handshake on `sync` serializes acquisition, the owner field makes it
// reentrant, and there is no true parallelism to make the coarse guard observably differ
// from a lock-free one.
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
    // Number of threads currently blocked trying to acquire (for the queue queries).
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
        synchronized (sync) {
            if (owner == me) {
                holdCount++;
            } else {
                if (owner != null) {
                    queued++;
                    while (owner != null) {
                        sync.wait();
                    }
                    queued--;
                }
                owner = me;
                holdCount = 1;
            }
        }
    }

    // No `throws` on this override (body raises none; narrower is valid) — also sidesteps
    // finding #104 (frozen javac ignores a classpath method's Exceptions attribute).
    public void lockInterruptibly() {
        // No thread interruption in the VM — identical to lock().
        lock();
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

    // No `throws` on this override (body raises none; narrower is valid) — see #104.
    public boolean tryLock(long time, TimeUnit unit) {
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
                    // Best-effort timed acquire: park up to `ms`, then re-check once. (No
                    // remaining-time loop — the library has no wall clock to recompute.)
                    queued++;
                    sync.wait(ms);
                    queued--;
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
        synchronized (sync) {
            if (owner != me) {
                while (owner != null) {
                    sync.wait();
                }
            }
            owner = me;
            holdCount = holds;
        }
    }
}
