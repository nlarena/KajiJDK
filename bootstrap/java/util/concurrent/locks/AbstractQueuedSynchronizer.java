package java.util.concurrent.locks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

// Minimal java.util.concurrent.locks.AbstractQueuedSynchronizer — the framework almost every
// j.u.c. lock is built on. A subclass keeps its meaning in an int `state` (mutated only through
// the CAS helpers) and implements the try-methods; AQS supplies the blocking. Two modes:
//
//   exclusive (acquire/release): one owner at a time — ReentrantLock. tryAcquire returns
//     true if it took ownership, tryRelease true if it fully let go.
//   shared    (acquireShared/releaseShared): several owners at once — Semaphore, CountDownLatch.
//     tryAcquireShared returns >= 0 if it got in (< 0 if it must wait), tryReleaseShared true if
//     the release may let a waiter through.
//
// The waiter queue is a lock-free Treiber stack (CAS push), and a release wakes **all** of them
// (they re-run their try-method; losers re-enqueue and re-park). That's simpler than the JDK's CLH
// FIFO queue — a thundering herd instead of hand-off, and no fairness — but it's easy to see
// correct: LockSupport's permit means an unpark can't be lost between a failed try and the park,
// and draining the whole stack on release means no waiter is ever left un-signalled. It also makes
// shared mode fall out for free: since every waiter is woken, permit propagation needs no special
// "signal the next node too" dance — everyone re-contends.
public class AbstractQueuedSynchronizer {
    private final AtomicInteger state = new AtomicInteger(0);
    private final AtomicReference<WaiterNode> waiters = new AtomicReference<WaiterNode>();

    protected final int getState() {
        return state.get();
    }

    protected final void setState(int newState) {
        state.set(newState);
    }

    protected final boolean compareAndSetState(int expect, int update) {
        return state.compareAndSet(expect, update);
    }

    // Concrete (not abstract), like the real AQS: a subclass overrides the mode(s) it uses. The
    // base versions are never reached in a correct subclass.
    protected boolean tryAcquire(int arg) {
        return false;
    }

    protected boolean tryRelease(int arg) {
        return false;
    }

    protected int tryAcquireShared(int arg) {
        return -1;
    }

    protected boolean tryReleaseShared(int arg) {
        return false;
    }

    // --- Exclusive mode ---

    public final void acquire(int arg) {
        while (!tryAcquire(arg)) {
            WaiterNode node = enqueue();
            // Re-check after enqueuing: if the holder released while we were pushing, its unpark may
            // have arrived before we parked — take the lock now rather than block on a stale state.
            if (tryAcquire(arg)) {
                node.thread = null;
                return;
            }
            LockSupport.park();
        }
    }

    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            wakeAll();
            return true;
        }
        return false;
    }

    // --- Shared mode ---

    public final void acquireShared(int arg) {
        while (tryAcquireShared(arg) < 0) {
            WaiterNode node = enqueue();
            if (tryAcquireShared(arg) >= 0) {
                node.thread = null;
                return;
            }
            LockSupport.park();
        }
    }

    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            wakeAll();
            return true;
        }
        return false;
    }

    // --- Shared machinery ---

    // Push a node for the current thread onto the Treiber stack and return it.
    private WaiterNode enqueue() {
        WaiterNode node = new WaiterNode();
        node.thread = Thread.currentThread();
        for (;;) {
            WaiterNode h = waiters.get();
            node.next = h;
            if (waiters.compareAndSet(h, node)) {
                return node;
            }
        }
    }

    // Detach the whole waiter stack and unpark every thread on it; each re-runs its try-method.
    private void wakeAll() {
        WaiterNode node = waiters.getAndSet(null);
        while (node != null) {
            Thread t = node.thread;
            if (t != null) {
                LockSupport.unpark(t);
            }
            node = node.next;
        }
    }
}
