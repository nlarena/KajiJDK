package java.util.concurrent.locks;

// The {@link Condition} returned by {@link ReentrantLock#newCondition}. A top-level
// package-private class (not an inner class) that holds its owning lock explicitly —
// avoiding enclosing-instance capture, which the compiler does not yet generate reliably.
//
// The await protocol is lost-wakeup-free: the waiter enters the condition monitor
// (`cvar`) BEFORE releasing the lock, so a signaller — which must take `cvar` to notify —
// cannot slip its signal in between the release and the park. `cvar.wait()` then atomically
// releases `cvar` and parks; the signal can only land while the waiter is parked.
class ReentrantCondition implements Condition {

    private final ReentrantLock lock;
    private final Object cvar = new Object();

    ReentrantCondition(ReentrantLock lock) {
        this.lock = lock;
    }

    // No `throws InterruptedException`: the body raises none (our monitor waits are
    // unchecked), and a narrower throws is a valid override. This also sidesteps finding
    // #104 (the frozen javac's class reader ignores a classpath method's Exceptions
    // attribute, so a matching `throws` on the override reads as *wider* and is rejected).
    public void await() {
        int holds;
        synchronized (cvar) {
            holds = lock.fullyRelease();
            cvar.wait();
        }
        lock.reacquire(holds);
    }

    public void awaitUninterruptibly() {
        int holds;
        synchronized (cvar) {
            holds = lock.fullyRelease();
            cvar.wait();
        }
        lock.reacquire(holds);
    }

    public void signal() {
        if (!lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        synchronized (cvar) {
            cvar.notify();
        }
    }

    public void signalAll() {
        if (!lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        synchronized (cvar) {
            cvar.notifyAll();
        }
    }
}
