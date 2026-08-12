package java.util.concurrent.locks;

// Minimal java.util.concurrent.locks.ReentrantReadWriteLock — many concurrent readers OR one
// exclusive writer, on a single AQS whose `state` is split: the **high 16 bits** hold the shared
// (read) count and the **low 16 bits** the exclusive (write) hold count. The write lock rides AQS
// **exclusive** mode (`acquire`/`release`), the read lock rides AQS **shared** mode
// (`acquireShared`/`releaseShared`). A writer may take the read lock it already holds (downgrade);
// a reader can't upgrade to write while others read. (Simplification vs. the JDK: read holds aren't
// tracked per-thread — that needs `ThreadLocal` — so read reentrancy isn't counted, which is fine
// for the classic reader/writer use where a thread holds at most one read hold at a time.)
public class ReentrantReadWriteLock {
    private static final int SHARED_SHIFT = 16;
    private static final int SHARED_UNIT = 1 << SHARED_SHIFT; // one reader = +1 in the high half
    private static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

    private static int sharedCount(int c) {
        return c >>> SHARED_SHIFT; // readers
    }

    private static int exclusiveCount(int c) {
        return c & EXCLUSIVE_MASK; // writer hold count
    }

    private static final class Sync extends AbstractQueuedSynchronizer {
        // The writing thread, like ReentrantLock's owner: written just after the state CAS and read
        // by a contender only to compare against *itself*, so a stale read can only say "not me".
        private Thread owner;

        // Write lock (exclusive). Grab only when no readers and no other writer hold; a writer that
        // already owns it re-enters (bumps the low half).
        protected boolean tryAcquire(int acquires) {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c != 0) {
                // Held by someone. If there are readers (w == 0) or the writer isn't us, we wait.
                if (exclusiveCount(c) == 0 || current != owner) {
                    return false;
                }
                setState(c + acquires); // reentrant write
                return true;
            }
            if (!compareAndSetState(c, c + acquires)) {
                return false;
            }
            owner = current;
            return true;
        }

        protected boolean tryRelease(int releases) {
            if (Thread.currentThread() != owner) {
                throw new IllegalMonitorStateException();
            }
            int nextc = getState() - releases;
            boolean free = exclusiveCount(nextc) == 0;
            if (free) {
                owner = null;
            }
            setState(nextc);
            return free;
        }

        // Read lock (shared). Block while a *different* thread holds the write lock; otherwise bump
        // the reader count with a CAS, retrying on contention so we only report "wait" when a writer
        // truly holds. `>= 0` tells AQS we got in.
        protected int tryAcquireShared(int unused) {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0 && owner != current) {
                    return -1;
                }
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    return 1;
                }
            }
        }

        protected boolean tryReleaseShared(int unused) {
            for (;;) {
                int c = getState();
                int nextc = c - SHARED_UNIT;
                if (compareAndSetState(c, nextc)) {
                    // Wake blocked writers only when the last reader leaves.
                    return sharedCount(nextc) == 0;
                }
            }
        }

        boolean isHeldExclusively() {
            return exclusiveCount(getState()) != 0 && owner == Thread.currentThread();
        }
    }

    private final Sync sync = new Sync();
    private final ReadLock readerLock = new ReadLock(this);
    private final WriteLock writerLock = new WriteLock(this);

    public ReadLock readLock() {
        return readerLock;
    }

    public WriteLock writeLock() {
        return writerLock;
    }

    // The read view: `lock()` shares, `unlock()` releases a reader. Non-interruptible.
    public static final class ReadLock {
        private final Sync sync;

        ReadLock(ReentrantReadWriteLock outer) {
            this.sync = outer.sync;
        }

        public void lock() {
            sync.acquireShared(1);
        }

        public void unlock() {
            sync.releaseShared(1);
        }
    }

    // The write view: `lock()` takes exclusive ownership, `unlock()` releases it.
    public static final class WriteLock {
        private final Sync sync;

        WriteLock(ReentrantReadWriteLock outer) {
            this.sync = outer.sync;
        }

        public void lock() {
            sync.acquire(1);
        }

        public void unlock() {
            sync.release(1);
        }

        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }
    }
}
