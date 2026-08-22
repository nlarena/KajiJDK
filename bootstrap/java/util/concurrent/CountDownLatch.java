package java.util.concurrent;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

// Minimal java.util.concurrent.CountDownLatch — a one-shot gate: threads `await()` until the count
// reaches zero, which `countDown()` walks it toward. Built on AQS **shared** mode: the count is the
// AQS `state`, `await` is acquireShared (which only succeeds once the count hits zero), and
// `countDown` is releaseShared (which, on reaching zero, opens the gate for everyone at once).
// (The earlier version was monitor-based; this is the real one.)
public class CountDownLatch {
    // "Acquired" means the gate is open — state == 0. tryAcquireShared therefore returns 1 (got in)
    // only at zero; tryReleaseShared decrements and reports true exactly on the 1→0 transition, the
    // single moment the gate opens.
    private static final class Sync extends AbstractQueuedSynchronizer {
        Sync(int count) {
            setState(count);
        }

        int count() {
            return getState();
        }

        protected int tryAcquireShared(int ignored) {
            return getState() == 0 ? 1 : -1;
        }

        protected boolean tryReleaseShared(int ignored) {
            for (;;) {
                int current = getState();
                if (current == 0) {
                    return false;
                }
                int next = current - 1;
                if (compareAndSetState(current, next)) {
                    return next == 0;
                }
            }
        }
    }

    private final Sync sync;

    public CountDownLatch(int count) {
        this.sync = new Sync(count);
    }

    public void await() throws InterruptedException {
        sync.acquireShared(1);
    }

    public void countDown() {
        sync.releaseShared(1);
    }

    public long getCount() {
        return sync.count();
    }
}
