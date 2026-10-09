package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

// A tool for controlling access to a shared resource by multiple threads — the
// flexible, non-block-structured alternative to a `synchronized` block: acquire and
// release can span methods, and acquisition can be conditional or timed.
public interface Lock {

    // Acquire the lock, blocking until it is available.
    void lock();

    // Acquire unless the thread is interrupted. (KajiJDK has no thread interruption, so
    // this behaves as {@link #lock}; the declaration is kept for source compatibility.)
    void lockInterruptibly() throws InterruptedException;

    // Acquire only if free at the moment of call; returns whether it was acquired.
    boolean tryLock();

    // Acquire within the given waiting time; returns whether it was acquired.
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    // Release the lock.
    void unlock();

    // A new {@link Condition} bound to this lock.
    Condition newCondition();
}
