package java.util.concurrent.locks;

// The {@link Condition} returned by a {@link ReentrantReadWriteLock}'s write lock. A
// top-level package-private class (not an inner one) holding its lock explicitly — the
// same shape as {@link ReentrantCondition}, and for the same reason: it avoids relying on
// enclosing-instance capture.
//
// The await protocol is lost-wakeup-free: the waiter enters the condition monitor (`cvar`)
// BEFORE releasing the write lock, and a signaller must take `cvar` to notify — so no
// signal can slip in between the release and the park.
class WriteCondition implements Condition {

    private final ReentrantReadWriteLock lock;
    private final Object cvar = new Object();
    // Those waiting on this condition. See `ReentrantCondition`'s note.
    private final java.util.ArrayList<Thread> waiting = new java.util.ArrayList<Thread>();

    WriteCondition(ReentrantReadWriteLock lock) {
        this.lock = lock;
    }

    // It declares `throws InterruptedException`, as the JDK does: `Object.wait()` is an
    // interruptible wait and swallowing that interruption takes from the caller the only way of
    // getting a thread out of the wait. The previous note avoided it because of finding #104, which
    // is closed.
    public void await() throws InterruptedException {
        int holds;
        synchronized (cvar) {
            waiting.add(Thread.currentThread());
            holds = lock.fullyReleaseWrite();
            try {
                cvar.wait();
            } finally {
                waiting.remove(Thread.currentThread());
            }
        }
        lock.reacquireWrite(holds);
    }

    /** It waits uninterruptibly; the interruption is re-marked at the end, it is not lost. */
    public void awaitUninterruptibly() {
        int holds;
        boolean wasInterrupted = false;
        synchronized (cvar) {
            waiting.add(Thread.currentThread());
            holds = lock.fullyReleaseWrite();
            boolean done = false;
            while (!done) {
                try {
                    cvar.wait();
                    done = true;
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                }
            }
            waiting.remove(Thread.currentThread());
        }
        lock.reacquireWrite(holds);
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /** It waits with a deadline in nanoseconds; it returns what was left. See `ReentrantCondition`. */
    public long awaitNanos(long nanosTimeout) throws InterruptedException {
        int holds;
        long leftOver;
        synchronized (cvar) {
            long startedAt = System.nanoTime();
            waiting.add(Thread.currentThread());
            holds = lock.fullyReleaseWrite();
            try {
                long millis = nanosTimeout / 1000000L;
                int nanos = (int) (nanosTimeout % 1000000L);
                if (millis > 0L || nanos > 0) {
                    cvar.wait(millis, nanos);
                }
            } finally {
                waiting.remove(Thread.currentThread());
            }
            leftOver = nanosTimeout - (System.nanoTime() - startedAt);
        }
        lock.reacquireWrite(holds);
        return leftOver;
    }

    /** It waits with a deadline; `false` if it ran out. */
    public boolean await(long time, java.util.concurrent.TimeUnit unit)
            throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return this.awaitNanos(unit.toNanos(time)) > 0L;
    }

    /** It waits until a calendar date; `false` if the date arrived first. */
    public boolean awaitUntil(java.util.Date deadline) throws InterruptedException {
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

    // ---- what the lock needs for its inspection queries ------------------------------------------

    boolean belongsTo(ReentrantReadWriteLock other) {
        return this.lock == other;
    }

    boolean anyWaiting() {
        boolean any;
        synchronized (cvar) {
            any = !waiting.isEmpty();
        }
        return any;
    }

    int waitingCount() {
        int n;
        synchronized (cvar) {
            n = waiting.size();
        }
        return n;
    }

    java.util.Collection<Thread> theWaiters() {
        java.util.ArrayList<Thread> copy;
        synchronized (cvar) {
            copy = new java.util.ArrayList<Thread>(waiting);
        }
        return copy;
    }

    public void signal() {
        if (!lock.writeHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        synchronized (cvar) {
            cvar.notify();
        }
    }

    public void signalAll() {
        if (!lock.writeHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        synchronized (cvar) {
            cvar.notifyAll();
        }
    }
}
