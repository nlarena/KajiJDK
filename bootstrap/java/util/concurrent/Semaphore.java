package java.util.concurrent;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

// Minimal java.util.concurrent.Semaphore — a permit counter: `acquire()` takes one (blocking while
// none are free), `release()` hands one back. A `Semaphore(1)` is a mutex. Built on AQS **shared**
// mode: the permit count lives in AQS `state`, so acquire/release are lock-free CAS loops and
// blocking rides on LockSupport. (The earlier version was monitor-based; this is the real one.)
public class Semaphore {
    // The count is the AQS state. tryAcquireShared spins the count down (failing into a wait when it
    // would go negative); tryReleaseShared spins it back up. Both are CAS loops — no lock.
    private static final class Sync extends AbstractQueuedSynchronizer {
        Sync(int permits) {
            setState(permits);
        }

        int permits() {
            return getState();
        }

        protected int tryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 || compareAndSetState(available, remaining)) {
                    return remaining;
                }
            }
        }

        protected boolean tryReleaseShared(int releases) {
            for (;;) {
                int current = getState();
                int next = current + releases;
                if (compareAndSetState(current, next)) {
                    return true;
                }
            }
        }
    }

    private final Sync sync;

    public Semaphore(int permits) {
        this.sync = new Sync(permits);
    }

    public void acquire() throws InterruptedException {
        sync.acquireShared(1);
    }

    public void release() {
        sync.releaseShared(1);
    }

    public boolean tryAcquire() {
        return sync.tryAcquireShared(1) >= 0;
    }

    public int availablePermits() {
        return sync.permits();
    }
}
