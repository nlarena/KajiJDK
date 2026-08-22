package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

// A reentrant read/write lock: any number of readers may hold it at once, but a writer
// holds it alone. The JDK packs both counts into one AbstractQueuedSynchronizer word;
// KajiJDK keeps them as plain fields guarded by the intrinsic monitor of a private `sync`
// object, with `Thread.currentThread()` for owner identity — faithful on a runtime whose
// threads interleave between opcodes.
//
// Reentrancy: a reader may re-acquire the read lock; a writer may re-acquire the write
// lock, and while holding it may also take the read lock (**downgrading**: take the read
// lock, then release the write lock). Upgrading read → write deadlocks, exactly as in the
// JDK, because the upgrade waits for a reader count that includes the caller's own hold.
//
// Style: every method is single-exit — no `return` from inside a `synchronized` block
// (finding #105: the compiler emits no `monitorexit` on that path, leaking the monitor).
// A `throw` inside the block is safe: the generated handler releases it.
public class ReentrantReadWriteLock implements ReadWriteLock, Serializable {

    // The monitor guarding every field below, and the acquire/release handshake.
    private final Object sync = new Object();
    // The thread holding the write lock, or null when unheld.
    private Thread writer;
    // Reentrant write-acquisition count (0 when the write lock is free).
    private int writeHolds;
    // Total read holds across all threads (a reader's reentrant holds each count).
    private int readers;
    // Per-thread read holds, as parallel arrays scanned linearly (few readers here, and
    // this avoids both boxing and a Map dependency).
    private Thread[] readOwners = new Thread[4];
    private int[] readCounts = new int[4];
    private int readOwnerCount;
    // Threads currently blocked in an acquire (for the queue queries).
    private int queued;
    // Fairness flag. Acquisition is already near-FIFO through the monitor's wait-set, so
    // the flag is stored and reported but does not change policy.
    private final boolean fair;

    private final ReadLock readerLock;
    private final WriteLock writerLock;

    public ReentrantReadWriteLock() {
        this.fair = false;
        this.readerLock = new ReadLock(this);
        this.writerLock = new WriteLock(this);
    }

    public ReentrantReadWriteLock(boolean fair) {
        this.fair = fair;
        this.readerLock = new ReadLock(this);
        this.writerLock = new WriteLock(this);
    }

    // Declared to return the `Lock` interface rather than the covariant nested type the
    // JDK declares. Both descriptors exist on the JDK class (it emits a `Lock`-returning
    // bridge), so this still matches the gate — and unlike the covariant form it actually
    // *runs*: finding #108 (the class finder cannot resolve a cross-package nested type
    // `Outer$Inner`, so a call whose return type is one is either erased to `Object` or,
    // when chained, dropped outright). Callers needing WriteLock's extra methods go
    // through the lock object itself (getWriteHoldCount / isWriteLockedByCurrentThread).
    public Lock readLock() {
        return readerLock;
    }

    public Lock writeLock() {
        return writerLock;
    }

    public final boolean isFair() {
        return fair;
    }

    protected Thread getOwner() {
        Thread o;
        synchronized (sync) {
            o = writer;
        }
        return o;
    }

    public int getReadLockCount() {
        int n;
        synchronized (sync) {
            n = readers;
        }
        return n;
    }

    public boolean isWriteLocked() {
        boolean held;
        synchronized (sync) {
            held = writer != null;
        }
        return held;
    }

    public boolean isWriteLockedByCurrentThread() {
        boolean mine;
        synchronized (sync) {
            mine = writer == Thread.currentThread();
        }
        return mine;
    }

    public int getWriteHoldCount() {
        int n;
        synchronized (sync) {
            n = writer == Thread.currentThread() ? writeHolds : 0;
        }
        return n;
    }

    public int getReadHoldCount() {
        int n;
        synchronized (sync) {
            n = readHoldsOf(Thread.currentThread());
        }
        return n;
    }

    public final boolean hasQueuedThreads() {
        boolean any;
        synchronized (sync) {
            any = queued > 0;
        }
        return any;
    }

    public final int getQueueLength() {
        int n;
        synchronized (sync) {
            n = queued;
        }
        return n;
    }

    // --- per-thread read-hold bookkeeping (callers hold `sync`) ---

    // This thread's read holds, or 0 if it holds none.
    private int readHoldsOf(Thread t) {
        int held = 0;
        for (int i = 0; i < readOwnerCount; i++) {
            if (readOwners[i] == t) {
                held = readCounts[i];
            }
        }
        return held;
    }

    // Add `delta` to this thread's read holds, adding or dropping its slot as needed.
    private void addReadHold(Thread t, int delta) {
        int slot = -1;
        for (int i = 0; i < readOwnerCount; i++) {
            if (readOwners[i] == t) {
                slot = i;
            }
        }
        if (slot < 0) {
            if (readOwnerCount == readOwners.length) {
                int n = readOwners.length * 2;
                Thread[] grownOwners = new Thread[n];
                int[] grownCounts = new int[n];
                for (int i = 0; i < readOwnerCount; i++) {
                    grownOwners[i] = readOwners[i];
                    grownCounts[i] = readCounts[i];
                }
                readOwners = grownOwners;
                readCounts = grownCounts;
            }
            slot = readOwnerCount;
            readOwners[slot] = t;
            readCounts[slot] = 0;
            readOwnerCount++;
        }
        readCounts[slot] = readCounts[slot] + delta;
        if (readCounts[slot] == 0) {
            // Drop the slot by swapping the last one into its place.
            readOwners[slot] = readOwners[readOwnerCount - 1];
            readCounts[slot] = readCounts[readOwnerCount - 1];
            readOwners[readOwnerCount - 1] = null;
            readCounts[readOwnerCount - 1] = 0;
            readOwnerCount--;
        }
    }

    // --- acquire/release, called by the two nested lock views ---

    void acquireRead() {
        Thread me = Thread.currentThread();
        synchronized (sync) {
            // Only a write lock held by *another* thread blocks a reader; the writer
            // itself may take the read lock (that is what makes downgrading work).
            if (writer != null && writer != me) {
                queued++;
                while (writer != null && writer != me) {
                    sync.wait();
                }
                queued--;
            }
            readers++;
            addReadHold(me, 1);
        }
    }

    boolean tryAcquireRead(long ms, boolean timed) {
        Thread me = Thread.currentThread();
        boolean acquired;
        synchronized (sync) {
            if (writer == null || writer == me) {
                readers++;
                addReadHold(me, 1);
                acquired = true;
            } else if (timed && ms > 0L) {
                // Best-effort timed acquire: park up to `ms`, then re-check once.
                queued++;
                sync.wait(ms);
                queued--;
                if (writer == null || writer == me) {
                    readers++;
                    addReadHold(me, 1);
                    acquired = true;
                } else {
                    acquired = false;
                }
            } else {
                acquired = false;
            }
        }
        return acquired;
    }

    void releaseRead() {
        Thread me = Thread.currentThread();
        synchronized (sync) {
            if (readHoldsOf(me) == 0) {
                throw new IllegalMonitorStateException();
            }
            readers--;
            addReadHold(me, -1);
            if (readers == 0) {
                // The last reader out lets a waiting writer through.
                sync.notifyAll();
            }
        }
    }

    void acquireWrite() {
        Thread me = Thread.currentThread();
        synchronized (sync) {
            if (writer == me) {
                writeHolds++;
            } else {
                if (writer != null || readers > 0) {
                    queued++;
                    while (writer != null || readers > 0) {
                        sync.wait();
                    }
                    queued--;
                }
                writer = me;
                writeHolds = 1;
            }
        }
    }

    boolean tryAcquireWrite(long ms, boolean timed) {
        Thread me = Thread.currentThread();
        boolean acquired;
        synchronized (sync) {
            if (writer == me) {
                writeHolds++;
                acquired = true;
            } else if (writer == null && readers == 0) {
                writer = me;
                writeHolds = 1;
                acquired = true;
            } else if (timed && ms > 0L) {
                queued++;
                sync.wait(ms);
                queued--;
                if (writer == null && readers == 0) {
                    writer = me;
                    writeHolds = 1;
                    acquired = true;
                } else {
                    acquired = false;
                }
            } else {
                acquired = false;
            }
        }
        return acquired;
    }

    void releaseWrite() {
        Thread me = Thread.currentThread();
        synchronized (sync) {
            if (writer != me) {
                throw new IllegalMonitorStateException();
            }
            writeHolds--;
            if (writeHolds == 0) {
                writer = null;
                sync.notifyAll();
            }
        }
    }

    // --- seam for the write lock's conditions (see WriteCondition) ---

    // Fully release the write lock whatever its depth, returning the saved count.
    int fullyReleaseWrite() {
        Thread me = Thread.currentThread();
        int saved;
        synchronized (sync) {
            if (writer != me) {
                throw new IllegalMonitorStateException();
            }
            saved = writeHolds;
            writer = null;
            writeHolds = 0;
            sync.notifyAll();
        }
        return saved;
    }

    // Re-acquire the write lock after an await, restoring the saved depth.
    void reacquireWrite(int holds) {
        Thread me = Thread.currentThread();
        synchronized (sync) {
            while (writer != null || readers > 0) {
                sync.wait();
            }
            writer = me;
            writeHolds = holds;
        }
    }

    boolean writeHeldByCurrentThread() {
        boolean mine;
        synchronized (sync) {
            mine = writer == Thread.currentThread();
        }
        return mine;
    }

    // The lock returned by {@link ReentrantReadWriteLock#readLock}. Shared: many threads
    // may hold it at once, as long as no other thread holds the write lock.
    public static class ReadLock implements Lock, Serializable {

        private final ReentrantReadWriteLock lock;

        protected ReadLock(ReentrantReadWriteLock lock) {
            this.lock = lock;
        }

        public void lock() {
            lock.acquireRead();
        }

        // No `throws` on this override — the body raises none and a narrower throws is a
        // valid override (also sidesteps finding #104).
        public void lockInterruptibly() {
            lock.acquireRead();
        }

        public boolean tryLock() {
            return lock.tryAcquireRead(0L, false);
        }

        public boolean tryLock(long time, TimeUnit unit) {
            return lock.tryAcquireRead(unit.toMillis(time), true);
        }

        public void unlock() {
            lock.releaseRead();
        }

        // A read lock has no exclusive owner, so it cannot support conditions.
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    // The lock returned by {@link ReentrantReadWriteLock#writeLock}. Exclusive: held by
    // one thread, and only while no thread holds the read lock.
    public static class WriteLock implements Lock, Serializable {

        private final ReentrantReadWriteLock lock;

        protected WriteLock(ReentrantReadWriteLock lock) {
            this.lock = lock;
        }

        public void lock() {
            lock.acquireWrite();
        }

        // See ReadLock.lockInterruptibly for why there is no `throws` here.
        public void lockInterruptibly() {
            lock.acquireWrite();
        }

        public boolean tryLock() {
            return lock.tryAcquireWrite(0L, false);
        }

        public boolean tryLock(long time, TimeUnit unit) {
            return lock.tryAcquireWrite(unit.toMillis(time), true);
        }

        public void unlock() {
            lock.releaseWrite();
        }

        public Condition newCondition() {
            return new WriteCondition(lock);
        }

        public boolean isHeldByCurrentThread() {
            return lock.writeHeldByCurrentThread();
        }

        public int getHoldCount() {
            return lock.getWriteHoldCount();
        }
    }
}
