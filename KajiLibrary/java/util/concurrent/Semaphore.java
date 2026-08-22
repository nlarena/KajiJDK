package java.util.concurrent;

import java.io.Serializable;

// A counting semaphore: a pool of permits handed out by {@link #acquire} and returned by
// {@link #release}. Guarding a resource with N permits limits concurrent use to N; with
// one permit it degenerates into a mutex that (unlike a lock) any thread may release.
//
// Permits are a plain int guarded by the intrinsic monitor of a private `sync` object.
// Every method is single-exit (finding #105) and the `throws InterruptedException`
// clauses are omitted (see {@link CountDownLatch} for both notes).
public class Semaphore implements Serializable {

    private final Object sync = new Object();
    private int permits;
    // Threads currently blocked in an acquire (for the queue queries).
    private int queued;
    // Fairness flag: stored and reported, but the cooperative scheduler already hands the
    // monitor out in near-FIFO order, so it does not change policy here.
    private final boolean fair;

    public Semaphore(int permits) {
        this.permits = permits;
        this.fair = false;
    }

    public Semaphore(int permits, boolean fair) {
        this.permits = permits;
        this.fair = fair;
    }

    public void acquire() {
        acquire(1);
    }

    public void acquire(int permits) {
        synchronized (sync) {
            if (this.permits < permits) {
                queued++;
                while (this.permits < permits) {
                    sync.wait();
                }
                queued--;
            }
            this.permits = this.permits - permits;
        }
    }

    // KajiJDK has no interruption, so this is exactly {@link #acquire}.
    public void acquireUninterruptibly() {
        acquire(1);
    }

    public void acquireUninterruptibly(int permits) {
        acquire(permits);
    }

    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    public boolean tryAcquire(int permits) {
        boolean got;
        synchronized (sync) {
            if (this.permits >= permits) {
                this.permits = this.permits - permits;
                got = true;
            } else {
                got = false;
            }
        }
        return got;
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) {
        return tryAcquire(1, timeout, unit);
    }

    // Best-effort timed acquire: park once for the whole timeout, then re-check.
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        boolean got;
        synchronized (sync) {
            if (this.permits < permits) {
                long ms = unit.toMillis(timeout);
                if (ms > 0L) {
                    queued++;
                    sync.wait(ms);
                    queued--;
                }
            }
            if (this.permits >= permits) {
                this.permits = this.permits - permits;
                got = true;
            } else {
                got = false;
            }
        }
        return got;
    }

    public void release() {
        release(1);
    }

    public void release(int permits) {
        synchronized (sync) {
            this.permits = this.permits + permits;
            // Wake everyone: waiters want different permit counts, so only each waiter
            // can tell whether its own request can now be met.
            sync.notifyAll();
        }
    }

    public int availablePermits() {
        int n;
        synchronized (sync) {
            n = permits;
        }
        return n;
    }

    // Take every available permit at once and report how many that was.
    public int drainPermits() {
        int drained;
        synchronized (sync) {
            drained = permits;
            permits = 0;
        }
        return drained;
    }

    // Shrink the pool without acquiring — for a subclass that needs to retire permits.
    protected void reducePermits(int reduction) {
        synchronized (sync) {
            permits = permits - reduction;
        }
    }

    public boolean isFair() {
        return fair;
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
}
