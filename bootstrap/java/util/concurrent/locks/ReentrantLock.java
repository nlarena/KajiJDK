package java.util.concurrent.locks;

// Minimal java.util.concurrent.locks.ReentrantLock — a mutual-exclusion lock the owner can
// re-acquire (reentrancy), released once per acquire. Built on AQS **exclusive** mode (the real
// JDK design, replacing the earlier monitor-based one): the hold count lives in AQS `state`
// (0 = free, n = held n times by the owner) and the blocking rides on the AQS queue + LockSupport.
public class ReentrantLock {
    // tryAcquire: grab a free lock with a CAS and record the owner, or — if we already own it —
    // bump the count (reentrancy). tryRelease: only the owner may release; decrement, and report
    // the lock freed exactly when the count hits zero (so AQS wakes the waiters just once).
    //
    // `owner` is a plain field, like the JDK's AbstractOwnableSynchronizer.exclusiveOwnerThread:
    // it's written only just after the state CAS succeeds and read by a contender only to compare
    // against *itself*, so a stale read can only ever say "not me" — which is the correct answer
    // for a non-owner. The owning thread always sees its own write (program order).
    private static final class Sync extends AbstractQueuedSynchronizer {
        private Thread owner;

        protected boolean tryAcquire(int acquires) {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    owner = current;
                    return true;
                }
                return false;
            }
            if (current == owner) {
                setState(c + acquires);
                return true;
            }
            return false;
        }

        protected boolean tryRelease(int releases) {
            if (Thread.currentThread() != owner) {
                throw new IllegalMonitorStateException();
            }
            int c = getState() - releases;
            boolean free = false;
            if (c == 0) {
                free = true;
                owner = null;
            }
            setState(c);
            return free;
        }

        boolean isHeld() {
            return getState() != 0;
        }

        boolean ownedByCurrentThread() {
            return owner == Thread.currentThread();
        }

        int holdCount() {
            return getState();
        }
    }

    private final Sync sync = new Sync();

    public void lock() {
        sync.acquire(1);
    }

    public void unlock() {
        sync.release(1);
    }

    public boolean isLocked() {
        return sync.isHeld();
    }

    public int getHoldCount() {
        return sync.ownedByCurrentThread() ? sync.holdCount() : 0;
    }

    public Condition newCondition() {
        return new MonitorCondition(this);
    }

    // --- Support for Condition.await(): fully release the lock, then re-acquire it. ---

    // Release the lock **completely** (whatever the current hold depth) and return that depth, so
    // `reacquire` can restore it. Caller must hold the lock. `release` runs tryRelease down to zero
    // and wakes a lock-waiter.
    int fullyRelease() {
        int holds = sync.holdCount();
        sync.release(holds);
        return holds;
    }

    // Re-acquire the lock (blocking until free) and restore the saved hold count in one shot —
    // tryAcquire CASes state 0 → holds, so the depth comes back exactly as it was.
    void reacquire(int holds) {
        sync.acquire(holds);
    }
}
