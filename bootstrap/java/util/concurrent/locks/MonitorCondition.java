package java.util.concurrent.locks;

// A Condition for the monitor-based ReentrantLock. `await()` **fully releases** the lock (saving
// the hold count), blocks on this condition's own monitor until signalled, then re-acquires the
// lock at its old count. Holding the condition monitor across the release closes the lost-wakeup
// window — a `signal()` can't slip in between the release and the `wait()`, because it must take
// the same monitor. On interruption it still re-acquires the lock before re-throwing (§Condition).
class MonitorCondition implements Condition {
    private final ReentrantLock lock;
    private final Object monitor = new Object();

    MonitorCondition(ReentrantLock lock) {
        this.lock = lock;
    }

    public void await() throws InterruptedException {
        int holds;
        boolean interrupted = false;
        synchronized (monitor) {
            holds = lock.fullyRelease();
            try {
                monitor.wait();
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        lock.reacquire(holds);
        if (interrupted) {
            throw new InterruptedException();
        }
    }

    public void signal() {
        synchronized (monitor) {
            monitor.notify();
        }
    }

    public void signalAll() {
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }
}
