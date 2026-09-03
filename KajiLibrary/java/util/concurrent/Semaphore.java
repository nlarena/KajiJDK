package java.util.concurrent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

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
    // Threads currently blocked in an acquire, in the order they blocked. A list of the threads
    // themselves rather than a counter: getQueuedThreads has to name them, and a count cannot be
    // turned back into names. Everything else the queue queries need is derivable from the list.
    private final ArrayList<Thread> waiters = new ArrayList<Thread>();
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

    public void acquire() throws InterruptedException {
        acquire(1);
    }

    public void acquire(int permits) throws InterruptedException {
        synchronized (sync) {
            if (this.permits < permits) {
                Thread me = Thread.currentThread();
                waiters.add(me);
                // El `finally` no es adorno: si interrumpen la espera, `wait` sale por excepcion y
                // el hilo quedaria listado como encolado para siempre -- getQueueLength contaria un
                // esperador que ya no existe, y hasQueuedThreads no volveria a dar false nunca.
                try {
                    while (this.permits < permits) {
                        sync.wait();
                    }
                } finally {
                    waiters.remove(me);
                }
            }
            this.permits = this.permits - permits;
        }
    }

    /**
     * Toma un permiso **sin** poder ser interrumpido.
     *
     * <p>La nota que estaba aca decia que era identico a `acquire()` porque KajiJDK no tenia
     * interrupcion. La tiene: la diferencia es real y es esta -- `acquire()` aborta si interrumpen,
     * esta sigue esperando y **remarca** el hilo al salir, para que la interrupcion no se pierda.
     */
    public void acquireUninterruptibly() {
        acquireUninterruptibly(1);
    }

    /** El de arriba, con varios permisos. */
    public void acquireUninterruptibly(int permits) {
        boolean interrumpido = false;
        boolean listo = false;
        while (!listo) {
            try {
                acquire(permits);
                listo = true;
            } catch (InterruptedException e) {
                interrumpido = true;
            }
        }
        if (interrumpido) {
            Thread.currentThread().interrupt();
        }
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

    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return tryAcquire(1, timeout, unit);
    }

    // Best-effort timed acquire: park once for the whole timeout, then re-check.
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
        boolean got;
        synchronized (sync) {
            if (this.permits < permits) {
                long ms = unit.toMillis(timeout);
                if (ms > 0L) {
                    Thread me = Thread.currentThread();
                    waiters.add(me);
                    try {
                        sync.wait(ms);
                    } finally {
                        waiters.remove(me);
                    }
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
            any = waiters.size() > 0;
        }
        return any;
    }

    public final int getQueueLength() {
        int n;
        synchronized (sync) {
            n = waiters.size();
        }
        return n;
    }

    /**
     * The threads blocked in an acquire right now.
     *
     * <p>A snapshot, and a copy: handing out the live list would let a caller iterating it collide
     * with a thread joining or leaving the queue. Best-effort by nature -- the answer is already
     * stale by the time it is read -- which is why the JDK makes it {@code protected} and documents
     * it as a monitoring aid rather than a synchronisation primitive.
     *
     * <p>A thread appears here only while it is *parked* for permits. A thread that entered
     * {@link #tryAcquire} and found none never blocked, so it was never queued.
     */
    protected Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> snapshot;
        synchronized (sync) {
            snapshot = new ArrayList<Thread>(waiters);
        }
        return snapshot;
    }
}
