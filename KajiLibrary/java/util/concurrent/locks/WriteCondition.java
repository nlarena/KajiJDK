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

    WriteCondition(ReentrantReadWriteLock lock) {
        this.lock = lock;
    }

    // No `throws InterruptedException`: the body raises none (our monitor waits are
    // unchecked) and a narrower throws is a valid override — see finding #104.
    public void await() {
        int holds;
        synchronized (cvar) {
            holds = lock.fullyReleaseWrite();
            cvar.wait();
        }
        lock.reacquireWrite(holds);
    }

    public void awaitUninterruptibly() {
        int holds;
        synchronized (cvar) {
            holds = lock.fullyReleaseWrite();
            cvar.wait();
        }
        lock.reacquireWrite(holds);
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
