package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

// A reentrant read/write lock: any number of readers may hold it at once, but a writer
// holds it alone. The JDK packs both counts into one AbstractQueuedSynchronizer word;
// KajiJDK keeps them as plain fields guarded by the intrinsic monitor of a private `sync`
// object, with `Thread.currentThread()` for owner identity. This header used to add "faithful on a
// runtime whose threads interleave between opcodes"; that is no longer the whole story, since
// `JVM_THREADS=os` is a real OS-thread substrate. The guard holds there too --an intrinsic monitor
// is a real mutex-- and the tests built on this lock pass on all three substrates. See
// `ReentrantLock`'s header, which carries the same correction.
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
    // The threads blocked in an acquisition, **separated by role**. The separation is not a luxury:
    // a reader and a writer waiting on the same lock are waiting for different things --the reader
    // for the writer to release, the writer for everybody to leave-- and a diagnostic that mixes
    // them does not say which of the two is the one not making progress.
    private final java.util.ArrayList<Thread> queuedReaders = new java.util.ArrayList<Thread>();
    private final java.util.ArrayList<Thread> queuedWriters = new java.util.ArrayList<Thread>();
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

    // They return the **nested type**, as the JDK does, and not the `Lock` interface. The note that
    // used to be here said it could not be done because of finding #108 --the class finder did not
    // resolve a nested type from another package-- and that finding is closed. The difference is
    // concrete: with the narrowed return, `rwl.writeLock().getHoldCount()` compiles; with `Lock` one
    // had to go through the whole lock.
    public ReadLock readLock() {
        return readerLock;
    }

    public WriteLock writeLock() {
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

    /**
     * Whether that thread is waiting to acquire either of the two locks.
     *
     * <p>Like all of this block's, it is a **snapshot**: by the time the answer arrives the thread
     * may have acquired or given up. They serve for diagnostics, not for deciding.
     *
     * @throws NullPointerException if `thread` is `null`
     */
    public final boolean hasQueuedThread(Thread thread) {
        if (thread == null) {
            throw new NullPointerException("thread");
        }
        boolean present;
        synchronized (sync) {
            present = queuedReaders.contains(thread) || queuedWriters.contains(thread);
        }
        return present;
    }

    /** Every waiting thread, readers and writers. */
    protected java.util.Collection<Thread> getQueuedThreads() {
        java.util.ArrayList<Thread> copy;
        synchronized (sync) {
            copy = new java.util.ArrayList<Thread>(queuedReaders);
            copy.addAll(queuedWriters);
        }
        return copy;
    }

    /** Those waiting to **read**. */
    protected java.util.Collection<Thread> getQueuedReaderThreads() {
        java.util.ArrayList<Thread> copy;
        synchronized (sync) {
            copy = new java.util.ArrayList<Thread>(queuedReaders);
        }
        return copy;
    }

    /** Those waiting to **write**. */
    protected java.util.Collection<Thread> getQueuedWriterThreads() {
        java.util.ArrayList<Thread> copy;
        synchronized (sync) {
            copy = new java.util.ArrayList<Thread>(queuedWriters);
        }
        return copy;
    }

    // The three condition ones demand that the condition belong to **this lock**: asking a lock
    // about a foreign condition has no right answer, and "none" would be worse than failing.
    private WriteCondition ownCondition(Condition condition) {
        if (condition == null) {
            throw new NullPointerException("condition");
        }
        if (!(condition instanceof WriteCondition)) {
            throw new IllegalArgumentException("not owner");
        }
        WriteCondition c = (WriteCondition) condition;
        if (!c.belongsTo(this)) {
            throw new IllegalArgumentException("not owner");
        }
        return c;
    }

    /** Whether anybody is waiting on that condition of this lock. */
    public boolean hasWaiters(Condition condition) {
        return this.ownCondition(condition).anyWaiting();
    }

    /** How many are waiting on that condition of this lock. */
    public int getWaitQueueLength(Condition condition) {
        return this.ownCondition(condition).waitingCount();
    }

    /** The threads waiting on that condition of this lock. */
    protected java.util.Collection<Thread> getWaitingThreads(Condition condition) {
        return this.ownCondition(condition).theWaiters();
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
        boolean wasInterrupted = false;
        synchronized (sync) {
            // Only a write lock held by *another* thread blocks a reader; the writer
            // itself may take the read lock (that is what makes downgrading work).
            if (writer != null && writer != me) {
                queued++;
                queuedReaders.add(me);
                try {
                    while (writer != null && writer != me) {
                        // Non-interruptible (`Lock.lock()`'s contract): it is caught and re-marked
                        // at the end. The `finally` takes the thread out of the queue whatever
                        // happens.
                        try {
                            sync.wait();
                        } catch (InterruptedException e) {
                            wasInterrupted = true;
                        }
                    }
                } finally {
                    queued--;
                    queuedReaders.remove(me);
                }
            }
            readers++;
            addReadHold(me, 1);
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }

    boolean tryAcquireRead(long ms, boolean timed) {
        Thread me = Thread.currentThread();
        boolean acquired;
        boolean interruptedFlag = false;
        synchronized (sync) {
            if (writer == null || writer == me) {
                readers++;
                addReadHold(me, 1);
                acquired = true;
            } else if (timed && ms > 0L) {
                // With the **remaining deadline**, not a single attempt: a wait that wakes for
                // another reason would return `false` with the whole deadline still ahead.
                // `System.nanoTime()` is the right clock -- it does not jump if somebody changes the
                // system time.
                long endNanos = System.nanoTime() + ms * 1000000L;
                queued++;
                queuedReaders.add(me);
                try {
                    long remain = endNanos - System.nanoTime();
                    while (!(writer == null || writer == me) && remain > 0L) {
                        try {
                            sync.wait(remain / 1000000L, (int) (remain % 1000000L));
                        } catch (InterruptedException e) {
                            interruptedFlag = true;
                        }
                        remain = endNanos - System.nanoTime();
                    }
                } finally {
                    queued--;
                    queuedReaders.remove(me);
                }
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
        boolean wasInterrupted = false;
        synchronized (sync) {
            if (writer == me) {
                writeHolds++;
            } else {
                if (writer != null || readers > 0) {
                    queued++;
                    queuedWriters.add(me);
                    try {
                        while (writer != null || readers > 0) {
                            try {
                                sync.wait();
                            } catch (InterruptedException e) {
                                wasInterrupted = true;
                            }
                        }
                    } finally {
                        queued--;
                        queuedWriters.remove(me);
                    }
                }
                writer = me;
                writeHolds = 1;
            }
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }

    boolean tryAcquireWrite(long ms, boolean timed) {
        Thread me = Thread.currentThread();
        boolean acquired;
        boolean interruptedFlag = false;
        synchronized (sync) {
            if (writer == me) {
                writeHolds++;
                acquired = true;
            } else if (writer == null && readers == 0) {
                writer = me;
                writeHolds = 1;
                acquired = true;
            } else if (timed && ms > 0L) {
                // With the remaining deadline, as in `tryAcquireRead`. See the note over there.
                long endNanos = System.nanoTime() + ms * 1000000L;
                queued++;
                queuedWriters.add(me);
                try {
                    long remain = endNanos - System.nanoTime();
                    while (!(writer == null && readers == 0) && remain > 0L) {
                        try {
                            sync.wait(remain / 1000000L, (int) (remain % 1000000L));
                        } catch (InterruptedException e) {
                            interruptedFlag = true;
                        }
                        remain = endNanos - System.nanoTime();
                    }
                } finally {
                    queued--;
                    queuedWriters.remove(me);
                }
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
        boolean wasInterrupted = false;
        synchronized (sync) {
            while (writer != null || readers > 0) {
                try {
                    sync.wait();
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                }
            }
            writer = me;
            writeHolds = holds;
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
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
