package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

// A reentrant mutual-exclusion lock — the explicit-lock counterpart of a `synchronized`
// block. The JDK builds this on the AbstractQueuedSynchronizer; KajiJDK builds it
// directly on the intrinsic monitor of a private `sync` object plus `Thread.currentThread()`
// for owner identity. On the single-carrier cooperative scheduler this is faithful: a
// `wait`/`notify` handshake on `sync` serializes acquisition, the owner field makes it
// reentrant, and there is no true parallelism to make the coarse guard observably differ
// from a lock-free one.
//
// NOTE on style: every method is written single-exit — no `return` from inside a
// `synchronized (sync)` block. Finding #105: the frozen javac does not emit the
// `monitorexit` that a `return` inside a synchronized block requires (it handles only the
// fall-through and exceptional exits), so an early return would leak the monitor. A `throw`
// inside the block is safe — the compiler-generated exception handler releases it.
public class ReentrantLock implements Lock, Serializable {

    // The monitor guarding all state below and the acquire/release handshake.
    private final Object sync = new Object();
    // The thread that holds the lock, or null when free.
    private Thread owner;
    // Reentrant acquisition count (0 when free).
    private int holdCount;
    // Los hilos bloqueados intentando adquirir. Antes era un contador; hace falta la lista porque
    // `getQueuedThreads` pide los hilos, y el contador sale de ella.
    private final java.util.ArrayList<Thread> encolados = new java.util.ArrayList<Thread>();
    private int queued;
    // Fairness flag. On the cooperative scheduler acquisition is already close to FIFO
    // via the monitor wait-set; the flag is honoured as state but does not change policy.
    private final boolean fair;

    public ReentrantLock() {
        this.fair = false;
    }

    public ReentrantLock(boolean fair) {
        this.fair = fair;
    }

    public void lock() {
        Thread me = Thread.currentThread();
        boolean interrumpido = false;
        synchronized (sync) {
            if (owner == me) {
                holdCount++;
            } else {
                if (owner != null) {
                    queued++;
                    encolados.add(Thread.currentThread());
                    while (owner != null) {
                    // No interrumpible (contrato de `Lock.lock()`): se atrapa, se sigue
                    // esperando, y se remarca el hilo al final. Abortar aca dejaria el
                    // lock a medio adquirir.
                        try {
                            sync.wait();
                        } catch (InterruptedException e) {
                            interrumpido = true;
                        }
                    }
                    queued--;
                    encolados.remove(Thread.currentThread());
                }
                owner = me;
                holdCount = 1;
            }
        }
        if (interrumpido) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Adquiere el lock, **abortando si interrumpen** al hilo.
     *
     * <p>Es la contraparte de `lock()`, que no aborta. La nota que estaba aca decia que la VM no
     * tenia interrupcion de hilos y que este metodo era identico a `lock()`; las dos cosas dejaron
     * de ser ciertas -- `Thread.interrupt()` existe y despierta las esperas, asi que este metodo
     * puede hacer lo que su nombre promete.
     *
     * @throws InterruptedException si interrumpen al hilo mientras espera
     */
    public void lockInterruptibly() throws InterruptedException {
        Thread me = Thread.currentThread();
        // Se comprueba **antes** de esperar: un hilo ya interrumpido no debe entrar a la espera.
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        synchronized (sync) {
            if (owner == me) {
                holdCount++;
            } else {
                if (owner != null) {
                    queued++;
                    encolados.add(me);
                    try {
                        while (owner != null) {
                            sync.wait();
                        }
                    } finally {
                        // El `finally` importa: si la espera se corta por interrupcion, el hilo tiene
                        // que salir de la cola igual. Sin esto, un `getQueuedThreads` posterior
                        // mostraria un hilo que ya no espera nada.
                        queued--;
                        encolados.remove(me);
                    }
                }
                owner = me;
                holdCount = 1;
            }
        }
    }

    public boolean tryLock() {
        Thread me = Thread.currentThread();
        boolean acquired;
        synchronized (sync) {
            if (owner == null) {
                owner = me;
                holdCount = 1;
                acquired = true;
            } else if (owner == me) {
                holdCount++;
                acquired = true;
            } else {
                acquired = false;
            }
        }
        return acquired;
    }

    /**
     * Adquiere el lock esperando como mucho ese plazo.
     *
     * @throws InterruptedException si interrumpen al hilo mientras espera
     */
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        Thread me = Thread.currentThread();
        boolean acquired;
        synchronized (sync) {
            if (owner == null) {
                owner = me;
                holdCount = 1;
                acquired = true;
            } else if (owner == me) {
                holdCount++;
                acquired = true;
            } else {
                long ms = unit.toMillis(time);
                if (ms <= 0L) {
                    acquired = false;
                } else {
                    // Con **plazo restante**, no un solo intento. La nota anterior decia que la
                    // biblioteca no tenia reloj para recalcular; `System.nanoTime()` existe. Sin el
                    // bucle, una espera que despertaba por otra razon --otro hilo soltando y
                    // retomando el lock-- devolvia `false` con el plazo entero por delante.
                    long finNanos = System.nanoTime() + ms * 1000000L;
                    queued++;
                    encolados.add(me);
                    try {
                        long restanNanos = finNanos - System.nanoTime();
                        while (owner != null && restanNanos > 0L) {
                            sync.wait(restanNanos / 1000000L, (int) (restanNanos % 1000000L));
                            restanNanos = finNanos - System.nanoTime();
                        }
                    } finally {
                        queued--;
                        encolados.remove(me);
                    }
                    if (owner == null) {
                        owner = me;
                        holdCount = 1;
                        acquired = true;
                    } else {
                        acquired = false;
                    }
                }
            }
        }
        return acquired;
    }

    public void unlock() {
        Thread me = Thread.currentThread();
        synchronized (sync) {
            if (owner != me) {
                // Safe inside the block: the exception path releases the monitor.
                throw new IllegalMonitorStateException();
            }
            holdCount--;
            if (holdCount == 0) {
                owner = null;
                sync.notify();
            }
        }
    }

    public Condition newCondition() {
        return new ReentrantCondition(this);
    }

    public int getHoldCount() {
        int held;
        synchronized (sync) {
            held = owner == Thread.currentThread() ? holdCount : 0;
        }
        return held;
    }

    public boolean isHeldByCurrentThread() {
        boolean held;
        synchronized (sync) {
            held = owner == Thread.currentThread();
        }
        return held;
    }

    public boolean isLocked() {
        boolean locked;
        synchronized (sync) {
            locked = owner != null;
        }
        return locked;
    }

    public final boolean isFair() {
        return fair;
    }

    protected Thread getOwner() {
        Thread o;
        synchronized (sync) {
            o = owner;
        }
        return o;
    }

    public final boolean hasQueuedThreads() {
        boolean any;
        synchronized (sync) {
            any = queued > 0;
        }
        return any;
    }

    /**
     * Si ese hilo esta esperando para adquirir este lock.
     *
     * <p>Es una **foto**, y el javadoc del JDK insiste en eso: el hilo puede haber adquirido o
     * abandonado para cuando la respuesta llegue. Sirve para diagnosticar, no para decidir.
     *
     * @throws NullPointerException si `thread` es `null`
     */
    public final boolean hasQueuedThread(Thread thread) {
        if (thread == null) {
            throw new NullPointerException("thread");
        }
        boolean esta;
        synchronized (sync) {
            esta = encolados.contains(thread);
        }
        return esta;
    }

    /** Los hilos que esperan para adquirir. Una copia: la lista interna no sale de aca. */
    protected java.util.Collection<Thread> getQueuedThreads() {
        java.util.ArrayList<Thread> copia;
        synchronized (sync) {
            copia = new java.util.ArrayList<Thread>(encolados);
        }
        return copia;
    }

    // ---- inspeccion de las condiciones -------------------------------------------------------------
    //
    // Las tres piden que la condicion sea **de este lock**: preguntarle a un lock por una condicion
    // ajena no tiene respuesta correcta, y devolver "ninguno" seria peor que fallar.

    private ReentrantCondition mia(Condition condition) {
        if (condition == null) {
            throw new NullPointerException("condition");
        }
        if (!(condition instanceof ReentrantCondition)) {
            throw new IllegalArgumentException("not owner");
        }
        ReentrantCondition c = (ReentrantCondition) condition;
        if (!c.perteneceA(this)) {
            throw new IllegalArgumentException("not owner");
        }
        return c;
    }

    /** Si alguien espera en esa condicion de este lock. */
    public boolean hasWaiters(Condition condition) {
        return this.mia(condition).hayEsperando();
    }

    /** Cuantos esperan en esa condicion de este lock. */
    public int getWaitQueueLength(Condition condition) {
        return this.mia(condition).cuantosEsperan();
    }

    /** Los hilos que esperan en esa condicion de este lock. */
    protected java.util.Collection<Thread> getWaitingThreads(Condition condition) {
        return this.mia(condition).losQueEsperan();
    }

    public final int getQueueLength() {
        int n;
        synchronized (sync) {
            n = queued;
        }
        return n;
    }

    // (toString is omitted: the JDK's builds on super.toString(), which the bytecode
    // generator does not support yet — a subset is fine for the gate.)

    // --- package-private seam for ReentrantCondition.await/signal ---

    // Fully release the lock (whatever the reentrant depth), returning the saved count so
    // the awaiter can restore it on re-acquisition. Wakes one blocked acquirer. Caller
    // must own the lock.
    int fullyRelease() {
        Thread me = Thread.currentThread();
        int saved;
        synchronized (sync) {
            if (owner != me) {
                throw new IllegalMonitorStateException();
            }
            saved = holdCount;
            owner = null;
            holdCount = 0;
            sync.notify();
        }
        return saved;
    }

    // Re-acquire the lock after an await, restoring the saved reentrant count.
    void reacquire(int holds) {
        Thread me = Thread.currentThread();
        boolean interrumpido = false;
        synchronized (sync) {
            if (owner != me) {
                while (owner != null) {
                    try {
                        sync.wait();
                    } catch (InterruptedException e) {
                        interrumpido = true;
                    }
                }
            }
            owner = me;
            holdCount = holds;
        }
        if (interrumpido) {
            Thread.currentThread().interrupt();
        }
    }
}
