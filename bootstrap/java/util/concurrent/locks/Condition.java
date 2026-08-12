package java.util.concurrent.locks;

// Minimal java.util.concurrent.locks.Condition — the condition-variable half of a Lock (the
// wait/notify of the explicit-lock world). Obtained from Lock.newCondition(); await/signal must be
// called holding the lock.
public interface Condition {
    void await() throws InterruptedException;

    void signal();

    void signalAll();
}
