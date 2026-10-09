package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

// A read/write lock with **three** modes, and the third is what justifies the class: besides the
// usual blocking read and write, it offers the **optimistic read**, which takes nothing.
// `tryOptimisticRead()` returns a snapshot of the state; the reader reads the fields it cares about
// and then asks `validate(stamp)`. If there was no write in between, the answer is `true` and the
// read was worth it, without a single word of shared memory having been written. If there was, it is
// `false` and one has to retry --or take the real lock.
//
// Every operation returns a **stamp** (`long`), and that stamp is what is handed to `unlock`. It is
// not reentrant: asking twice for the write lock from the same thread deadlocks. And it has no
// conditions: the views' `newCondition()` throws `UnsupportedOperationException`, just as in the
// JDK.
//
// ---------------------------------------------------------------------------------------------
// THE STAMP, WHICH IS THE WHOLE DESIGN
// ---------------------------------------------------------------------------------------------
//
// There is a single `long state`, with the JDK's bit layout:
//
//     bits 0..6   reader count (0..126); 127 is reserved for the overflow marker
//     bit  7      WBIT — there is a writer
//     bits 8..63  sequence number: **it goes up by one every time a write is released**
//
// Out of that come the two operations that make the optimistic read work:
//
//     tryOptimisticRead()  ->  state & SBITS      (the sequence and the write bit, no readers)
//     validate(stamp)      ->  (stamp & SBITS) == (state & SBITS)
//
// And out of that comes why **the stamp can genuinely be validated**, which is the only thing that
// makes this method worth anything: releasing a write does `state += WBIT`, and that sum clears
// bit 7 and **carries one into the sequence**. Which is to say that after any completed write the
// `& SBITS` is different, and the old stamp stops validating. A `tryOptimisticRead` that returned a
// number nobody then compares with anything would be worse than not having it: whoever uses it will
// believe they are asking the lock something.
//
// The zero stamp is the universal "I did not get it", and it cannot collide with a good one because
// the state starts at ORIGIN (256) and the sequence never returns to zero except on wrap-around, in
// which case `releaseWriteState` sends it back to ORIGIN.
//
// ---------------------------------------------------------------------------------------------
// HOW IT BLOCKS, AND WHAT THAT COSTS
// ---------------------------------------------------------------------------------------------
//
// The JDK brings its own CLH queue inside this class. Here the state is guarded by an internal
// monitor (`sync`) and **the wait is not**: whoever cannot get in registers a node of their own
// (`SyncWaiter`), releases `sync` and sleeps on **their node's** monitor; whoever releases sets the
// flag on every registered node and wakes them, and each competes again. It is correct and it is
// **unfair** -- which is exactly what the JDK's javadoc promises for this class ("this class does
// not favor readers over writers, nor does it support fairness"). What is given up against the CLH
// queue is performance under contention, not semantics.
//
// That the wait is **not** a `sync.wait()`/`sync.notifyAll()` on the state's monitor is not taste:
// it is that that does not work. Our VM leaves the thread inside a monitor's wait set when a
// **timed wait expires**, and then a later `notifyAll()` by that same thread on that same monitor
// wakes itself and the VM deadlocks with nobody runnable. A `StampedLock` does exactly that --a
// `tryReadLock(t, u)` that expires and then an `unlockWrite`-- so this class's first version hung
// reproducibly. Minimal repro with ablation in `scratchpad/zzlocks/WaitStale.java`. With a **fresh
// node per waiting episode**, the monitor one sleeps on is never notified again by its own owner,
// and the defect cannot be touched.
//
// It does not lean on `AbstractQueuedSynchronizer` on purpose, just as the JDK does not: the three
// modes and the conversions between them do not fit the `tryAcquire`/`tryRelease` contract.
//
// A note on style, this package's usual one: **no `return` inside a `synchronized` block**
// (finding #105 -- the frozen javac does not emit that exit's `monitorexit`). A `throw` inside IS
// safe.
public class StampedLock implements Serializable {

    // One reader.
    private static final long RUNIT = 1L;
    // The writer's bit.
    private static final long WBIT = 128L;
    // The reader count's bits.
    private static final long RBITS = 127L;
    // The most readers that fit in those bits; past that it goes to the separate counter.
    private static final long RFULL = 126L;
    // Every "it is held" bit: readers and writer.
    private static final long ABITS = 255L;
    // The bits that make up the stamp: the writer's and the sequence, without the reader count. It
    // is `~RBITS`, written as a literal so it can be seen to be the top 57 plus bit 7.
    private static final long SBITS = -128L;
    // The initial state. It is not zero so that no legitimate stamp can be zero.
    private static final long ORIGIN = 256L;

    // The internal monitor: it guards `state`, `overflow`, the waiter list and the views. It is
    // **not** where one sleeps: each waiter sleeps on its own node's monitor.
    private final Object sync = new Object();

    private long state = ORIGIN;

    // Readers that did not fit in the seven bits. With this the count has no ceiling, which is what
    // the JDK does with its `readerOverflow`.
    private int overflow;

    // Those waiting to get in. It is guarded by `sync`; each element is the node of a thread asleep
    // (or about to fall asleep) on its own monitor.
    private final java.util.ArrayList<SyncWaiter> waiting = new java.util.ArrayList<SyncWaiter>();

    // The three views, created the first time they are asked for.
    private Lock readView;
    private Lock writeView;
    private ReadWriteLock readWriteView;

    /** A fresh lock, not held. */
    public StampedLock() {
    }

    // ---- writing --------------------------------------------------------------------------------

    /**
     * It takes the write lock, waiting as long as it takes.
     *
     * <p>It does **not** honour interruptions: if one arrives, it is noted and the thread's flag is
     * set again on return. Aborting here would leave the caller without the lock and believing it
     * holds it.
     *
     * @return a write stamp (never zero)
     */
    public long writeLock() {
        long stampValue = 0L;
        boolean wasInterrupted = false;
        while (stampValue == 0L) {
            SyncWaiter node = null;
            synchronized (sync) {
                if ((state & ABITS) == 0L) {
                    stampValue = this.takeWriter();
                } else {
                    node = this.register();
                }
            }
            if (node != null) {
                if (this.sleepOn(node, false, 0L)) {
                    wasInterrupted = true;
                }
                this.take(node);
            }
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return stampValue;
    }

    /**
     * It takes the write lock, aborting if the thread is interrupted.
     *
     * @return a write stamp (never zero)
     * @throws InterruptedException if the thread is interrupted
     */
    public long writeLockInterruptibly() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        long stampValue = 0L;
        while (stampValue == 0L) {
            SyncWaiter node = null;
            synchronized (sync) {
                if ((state & ABITS) == 0L) {
                    stampValue = this.takeWriter();
                } else {
                    node = this.register();
                }
            }
            if (node != null) {
                boolean cutShort = this.sleepOn(node, false, 0L);
                this.take(node);
                if (cutShort) {
                    throw new InterruptedException();
                }
            }
        }
        return stampValue;
    }

    /**
     * It takes the write lock only if it is free right now.
     *
     * @return the stamp, or **zero** if it did not get it
     */
    public long tryWriteLock() {
        long stampValue;
        synchronized (sync) {
            stampValue = (state & ABITS) != 0L ? 0L : this.takeWriter();
        }
        return stampValue;
    }

    /**
     * It takes the write lock, waiting at most that deadline.
     *
     * @return the stamp, or **zero** if the deadline ran out
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public long tryWriteLock(long time, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        long end = System.nanoTime() + unit.toNanos(time);
        long stampValue = 0L;
        boolean keepGoing = true;
        while (keepGoing) {
            SyncWaiter node = null;
            synchronized (sync) {
                if ((state & ABITS) == 0L) {
                    stampValue = this.takeWriter();
                } else {
                    node = this.register();
                }
            }
            if (node == null) {
                keepGoing = false;
            } else if (end - System.nanoTime() <= 0L) {
                this.take(node);
                keepGoing = false;
            } else {
                boolean cutShort = this.sleepOn(node, true, end);
                this.take(node);
                if (cutShort) {
                    throw new InterruptedException();
                }
            }
        }
        return stampValue;
    }

    /**
     * It releases the write lock.
     *
     * @throws IllegalMonitorStateException if the stamp is not the current write's
     */
    public void unlockWrite(long stamp) {
        synchronized (sync) {
            if ((stamp & WBIT) == 0L || state != stamp) {
                throw new IllegalMonitorStateException();
            }
            this.releaseWriteState();
            this.wakeAll();
        }
    }

    /**
     * It releases the write lock **with no stamp**, if anybody holds it.
     *
     * <p>It is the emergency exit the JDK documents: it serves for recovering from an error, not for
     * ordinary use, because it does not check that whoever releases is whoever took it.
     *
     * @return `false` if there was no writer
     */
    public boolean tryUnlockWrite() {
        boolean had;
        synchronized (sync) {
            had = (state & WBIT) != 0L;
            if (had) {
                this.releaseWriteState();
                this.wakeAll();
            }
        }
        return had;
    }

    // ---- reading --------------------------------------------------------------------------------

    /**
     * It takes the read lock, waiting as long as it takes. It does not honour interruptions.
     *
     * @return a read stamp (never zero)
     */
    public long readLock() {
        long stampValue = 0L;
        boolean wasInterrupted = false;
        while (stampValue == 0L) {
            SyncWaiter node = null;
            synchronized (sync) {
                if ((state & WBIT) == 0L) {
                    stampValue = this.takeReader();
                } else {
                    node = this.register();
                }
            }
            if (node != null) {
                if (this.sleepOn(node, false, 0L)) {
                    wasInterrupted = true;
                }
                this.take(node);
            }
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return stampValue;
    }

    /**
     * It takes the read lock, aborting if the thread is interrupted.
     *
     * @return a read stamp (never zero)
     * @throws InterruptedException if the thread is interrupted
     */
    public long readLockInterruptibly() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        long stampValue = 0L;
        while (stampValue == 0L) {
            SyncWaiter node = null;
            synchronized (sync) {
                if ((state & WBIT) == 0L) {
                    stampValue = this.takeReader();
                } else {
                    node = this.register();
                }
            }
            if (node != null) {
                boolean cutShort = this.sleepOn(node, false, 0L);
                this.take(node);
                if (cutShort) {
                    throw new InterruptedException();
                }
            }
        }
        return stampValue;
    }

    /**
     * It takes the read lock only if there is no writer right now.
     *
     * @return the stamp, or **zero** if it did not get it
     */
    public long tryReadLock() {
        long stampValue;
        synchronized (sync) {
            stampValue = (state & WBIT) != 0L ? 0L : this.takeReader();
        }
        return stampValue;
    }

    /**
     * It takes the read lock, waiting at most that deadline.
     *
     * @return the stamp, or **zero** if the deadline ran out
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public long tryReadLock(long time, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        long end = System.nanoTime() + unit.toNanos(time);
        long stampValue = 0L;
        boolean keepGoing = true;
        while (keepGoing) {
            SyncWaiter node = null;
            synchronized (sync) {
                if ((state & WBIT) == 0L) {
                    stampValue = this.takeReader();
                } else {
                    node = this.register();
                }
            }
            if (node == null) {
                keepGoing = false;
            } else if (end - System.nanoTime() <= 0L) {
                this.take(node);
                keepGoing = false;
            } else {
                boolean cutShort = this.sleepOn(node, true, end);
                this.take(node);
                if (cutShort) {
                    throw new InterruptedException();
                }
            }
        }
        return stampValue;
    }

    /**
     * It releases the read lock.
     *
     * @throws IllegalMonitorStateException if the stamp does not correspond to a current read
     */
    public void unlockRead(long stamp) {
        synchronized (sync) {
            if ((stamp & RBITS) == 0L
                    || (state & SBITS) != (stamp & SBITS)
                    || (state & RBITS) == 0L) {
                throw new IllegalMonitorStateException();
            }
            this.releaseReadState();
            this.wakeAll();
        }
    }

    /**
     * It releases **one** read with no stamp, if there is one.
     *
     * @return `false` if there were no readers
     */
    public boolean tryUnlockRead() {
        boolean had;
        synchronized (sync) {
            had = (state & RBITS) != 0L;
            if (had) {
                this.releaseReadState();
                this.wakeAll();
            }
        }
        return had;
    }

    // ---- optimistic reading ---------------------------------------------------------------------

    /**
     * A snapshot of the state, **taking nothing**.
     *
     * <p>The use is always the same: the stamp is asked for, the data is read, and then
     * {@link #validate} is called. If it returns `false` the data read is worth nothing and one has
     * to retry or take {@link #readLock}.
     *
     * @return the stamp, or **zero** if there is a writer (in which case there is nothing to
     *     optimise)
     */
    public long tryOptimisticRead() {
        long stampValue;
        synchronized (sync) {
            stampValue = (state & WBIT) != 0L ? 0L : (state & SBITS);
        }
        return stampValue;
    }

    /**
     * Whether **no write has completed** since `stamp` was taken.
     *
     * <p>A zero stamp never validates: the state starts at `ORIGIN`, so `state & SBITS` cannot be
     * zero.
     */
    public boolean validate(long stamp) {
        boolean valid;
        synchronized (sync) {
            valid = (stamp & SBITS) == (state & SBITS);
        }
        return valid;
    }

    // ---- conversions -------------------------------------------------------------------------
    //
    // All three follow the same shape: if the stamp is still valid and the current mode allows the
    // move, it is done in **one single step** --without releasing and retaking, which is where a
    // writer would slip in-- and the new stamp is returned. Otherwise zero, and the old stamp is
    // still worth what it was.

    /**
     * It turns the stamp into a write one, if that can be done without releasing.
     *
     * @return the write stamp, or **zero** if it could not be done
     */
    public long tryConvertToWriteLock(long stamp) {
        long result = 0L;
        synchronized (sync) {
            if ((state & SBITS) == (stamp & SBITS)) {
                long a = stamp & ABITS;
                long m = state & ABITS;
                if (m == 0L) {
                    // Nobody holds it and the (optimistic) stamp is still valid: the write is taken.
                    if (a == 0L) {
                        result = this.takeWriter();
                    }
                } else if (m == WBIT) {
                    // It is ours already.
                    if (a == m) {
                        result = stamp;
                    }
                } else if (m == RUNIT && a != 0L) {
                    // We are the only reader: it moves to writer without opening the window.
                    state = state - RUNIT + WBIT;
                    result = state;
                }
            }
        }
        return result;
    }

    /**
     * It turns the stamp into a read one, if that can be done without releasing.
     *
     * @return the read stamp, or **zero** if it could not be done
     */
    public long tryConvertToReadLock(long stamp) {
        long result = 0L;
        boolean signalNode = false;
        synchronized (sync) {
            if ((state & SBITS) == (stamp & SBITS)) {
                long a = stamp & ABITS;
                long m = state & ABITS;
                if (m == 0L) {
                    if (a == 0L) {
                        result = this.takeReader();
                    }
                } else if (m == WBIT) {
                    if (a == m) {
                        // It releases the write and takes the read in the same operation: the
                        // `+WBIT` clears the bit and advances the sequence, the `+RUNIT` counts the
                        // reader.
                        state = state + WBIT + RUNIT;
                        result = state;
                        signalNode = true;
                    }
                } else if (a != 0L && a < WBIT) {
                    // It is a read one already.
                    result = stamp;
                }
            }
            if (signalNode) {
                this.wakeAll();
            }
        }
        return result;
    }

    /**
     * It releases whatever the stamp holds and returns an optimistic read stamp.
     *
     * @return the observation stamp, or **zero** if the stamp was no longer valid
     */
    public long tryConvertToOptimisticRead(long stamp) {
        long result = 0L;
        boolean signalNode = false;
        synchronized (sync) {
            if ((state & SBITS) == (stamp & SBITS)) {
                long a = stamp & ABITS;
                long m = state & ABITS;
                if (m == 0L) {
                    if (a == 0L) {
                        result = state & SBITS;
                    }
                } else if (m == WBIT) {
                    if (a == m) {
                        this.releaseWriteState();
                        result = state & SBITS;
                        signalNode = true;
                    }
                } else if (a != 0L && a < WBIT) {
                    this.releaseReadState();
                    result = state & SBITS;
                    signalNode = true;
                }
            }
            if (signalNode) {
                this.wakeAll();
            }
        }
        return result;
    }

    /**
     * It releases whatever the stamp holds.
     *
     * @throws IllegalMonitorStateException if the stamp is not a held lock's
     */
    public void unlock(long stamp) {
        long a = stamp & ABITS;
        if (a == WBIT) {
            this.unlockWrite(stamp);
        } else if (a != 0L && a < WBIT) {
            this.unlockRead(stamp);
        } else {
            throw new IllegalMonitorStateException();
        }
    }

    // ---- state queries -----------------------------------------------------------------------
    //
    // They are **snapshots**, like all of this package's: they serve for diagnostics, not for
    // deciding.

    /** Whether there is a writer. */
    public boolean isWriteLocked() {
        boolean r;
        synchronized (sync) {
            r = (state & WBIT) != 0L;
        }
        return r;
    }

    /** Whether there is at least one reader. */
    public boolean isReadLocked() {
        boolean r;
        synchronized (sync) {
            r = (state & RBITS) != 0L;
        }
        return r;
    }

    /** How many readers there are, counting the overflow's. */
    public int getReadLockCount() {
        long n;
        synchronized (sync) {
            n = (state & RBITS) + (long) overflow;
        }
        return (int) n;
    }

    // ---- the stamps, seen from outside -------------------------------------------------------
    //
    // All four are static and only look at the bits: they ask no lock anything, so a `StampedLock`'s
    // stamp can be classified without having the lock at hand.

    /** Whether the stamp is a held lock's (read or write). */
    public static boolean isLockStamp(long stamp) {
        return (stamp & ABITS) != 0L;
    }

    /** Whether the stamp is a write one. */
    public static boolean isWriteLockStamp(long stamp) {
        return (stamp & ABITS) == WBIT;
    }

    /** Whether the stamp is a read one. */
    public static boolean isReadLockStamp(long stamp) {
        return (stamp & RBITS) != 0L;
    }

    /** Whether the stamp is an optimistic read one (a valid one holding nothing). */
    public static boolean isOptimisticReadStamp(long stamp) {
        return (stamp & ABITS) == 0L && stamp != 0L;
    }

    // ---- the `Lock` views --------------------------------------------------------------------

    /**
     * A {@link Lock} view that takes and releases the read.
     *
     * <p>Its `newCondition()` throws `UnsupportedOperationException`: this lock has no conditions,
     * and returning one that does not work would be lying.
     */
    public Lock asReadLock() {
        Lock v;
        synchronized (sync) {
            if (readView == null) {
                readView = new ReadView();
            }
            v = readView;
        }
        return v;
    }

    /** A {@link Lock} view that takes and releases the write. */
    public Lock asWriteLock() {
        Lock v;
        synchronized (sync) {
            if (writeView == null) {
                writeView = new WriteView();
            }
            v = writeView;
        }
        return v;
    }

    /** Both views together, as a {@link ReadWriteLock}. */
    public ReadWriteLock asReadWriteLock() {
        ReadWriteLock v;
        synchronized (sync) {
            if (readWriteView == null) {
                readWriteView = new ReadWriteView();
            }
            v = readWriteView;
        }
        return v;
    }

    // ---- the state machinery -----------------------------------------------------------------
    //
    // All four assume the caller **already holds** `sync`. They do not take it themselves so that
    // the complete operation --check and change-- is one indivisible step.

    private long takeWriter() {
        state = state + WBIT;
        return state;
    }

    private void releaseWriteState() {
        long next = state + WBIT;
        // The sum clears bit 7 and carries one into the sequence: that is why every earlier stamp
        // stops validating. If the sequence wrapped around it goes back to ORIGIN, so that no
        // legitimate stamp can be zero.
        state = next == 0L ? ORIGIN : next;
    }

    private long takeReader() {
        if ((state & RBITS) < RFULL) {
            state = state + RUNIT;
        } else {
            // No more fit in the seven bits: they go to the separate counter, and the stamp is left
            // with the reader bits full (which is still non-zero, and that is the only thing
            // `unlockRead` needs to look at).
            overflow++;
        }
        return state;
    }

    private void releaseReadState() {
        if (overflow > 0 && (state & RBITS) == RFULL) {
            overflow--;
        } else {
            state = state - RUNIT;
        }
    }

    // ---- the wait ----------------------------------------------------------------------------
    //
    // A **fresh node per episode**: it registers, releases `sync`, sleeps on the node's monitor, and
    // on waking is taken out. The node is not reused, and that is the property that dodges the VM
    // defect described in the header -- a monitor on which a timed wait may have expired is never
    // notified again by the thread that waited on it.
    //
    // The lock order is always `sync` and then a node's monitor, never the other way round: the
    // waiter releases `sync` **before** taking its own. That is why there is no deadlock.

    // It registers a node for the current thread. The caller already holds `sync`.
    private SyncWaiter register() {
        SyncWaiter node = new SyncWaiter();
        node.thread = Thread.currentThread();
        waiting.add(node);
        return node;
    }

    // It takes it out of the list. It takes `sync` itself.
    private void take(SyncWaiter node) {
        synchronized (sync) {
            waiting.remove(node);
        }
        node.thread = null;
    }

    /**
     * It sleeps on the node's monitor until it is woken (or the deadline expires, or it is
     * interrupted).
     *
     * @return `true` if an interruption cut it short
     */
    private boolean sleepOn(SyncWaiter node, boolean timedWait, long endNanos) {
        boolean cutShort = false;
        synchronized (node) {
            // Checking the flag and falling asleep happen inside the same monitor `wakeAll` uses:
            // that is why a signal that arrived earlier is not lost.
            if (timedWait) {
                long remain = endNanos - System.nanoTime();
                while (!node.released && !cutShort && remain > 0L) {
                    try {
                        node.wait(this.inMillis(remain));
                    } catch (InterruptedException e) {
                        cutShort = true;
                    }
                    remain = endNanos - System.nanoTime();
                }
            } else if (!node.released) {
                try {
                    node.wait();
                } catch (InterruptedException e) {
                    cutShort = true;
                }
            }
        }
        return cutShort;
    }

    // It wakes everyone registered: each competes again for the state. The caller already holds
    // `sync`. Waking everyone and not one is right here: releasing a write can let **many** readers
    // through, and this class promises no fairness.
    private void wakeAll() {
        for (int i = 0; i < waiting.size(); i++) {
            SyncWaiter node = waiting.get(i);
            synchronized (node) {
                node.released = true;
                node.notifyAll();
            }
        }
    }

    // A deadline in nanos, turned into the milliseconds `Object.wait` asks for. Never zero: a
    // `wait(0)` waits forever, and a deadline of less than a millisecond is rounded up to the
    // smallest that can be asked for.
    private long inMillis(long nanos) {
        long ms = nanos / 1000000L;
        if (ms <= 0L) {
            ms = 1L;
        }
        return ms;
    }

    // =========================================================================================
    // The views
    // =========================================================================================

    final class ReadView implements Lock {

        public void lock() {
            readLock();
        }

        public void lockInterruptibly() throws InterruptedException {
            readLockInterruptibly();
        }

        public boolean tryLock() {
            return tryReadLock() != 0L;
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return tryReadLock(time, unit) != 0L;
        }

        public void unlock() {
            if (!tryUnlockRead()) {
                throw new IllegalMonitorStateException();
            }
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class WriteView implements Lock {

        public void lock() {
            writeLock();
        }

        public void lockInterruptibly() throws InterruptedException {
            writeLockInterruptibly();
        }

        public boolean tryLock() {
            return tryWriteLock() != 0L;
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return tryWriteLock(time, unit) != 0L;
        }

        public void unlock() {
            if (!tryUnlockWrite()) {
                throw new IllegalMonitorStateException();
            }
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class ReadWriteView implements ReadWriteLock {

        public Lock readLock() {
            return asReadLock();
        }

        public Lock writeLock() {
            return asWriteLock();
        }
    }
}
