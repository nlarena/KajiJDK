package java.util.concurrent.locks;

// The {@link Condition} returned by a {@link ReentrantReadWriteLock}'s write lock. A
// top-level package-private class (not an inner one) holding its lock explicitly — the
// same shape as {@link ReentrantCondition}, and for the same reason: it avoids relying on
// enclosing-instance capture.
//
// The await protocol is lost-wakeup-free: the waiter enters the condition monitor (`cvar`)
// BEFORE releasing the write lock, and a signaller must take `cvar` to notify — so no
// signal can slip in between the release and the park.
class WriteCondition implements Condition {

    private final ReentrantReadWriteLock lock;
    private final Object cvar = new Object();
    // Quienes esperan en esta condicion. Ver la nota de `ReentrantCondition`.
    private final java.util.ArrayList<Thread> esperando = new java.util.ArrayList<Thread>();

    WriteCondition(ReentrantReadWriteLock lock) {
        this.lock = lock;
    }

    // Declara `throws InterruptedException`, como el JDK: `Object.wait()` es una espera
    // interrumpible y tragarse esa interrupcion le quita al que llama la unica forma de sacar a un
    // hilo de la espera. La nota anterior lo evitaba por el finding #104, que se cerro.
    public void await() throws InterruptedException {
        int holds;
        synchronized (cvar) {
            esperando.add(Thread.currentThread());
            holds = lock.fullyReleaseWrite();
            try {
                cvar.wait();
            } finally {
                esperando.remove(Thread.currentThread());
            }
        }
        lock.reacquireWrite(holds);
    }

    /** Espera sin poder ser interrumpida; la interrupcion se remarca al final, no se pierde. */
    public void awaitUninterruptibly() {
        int holds;
        boolean interrumpido = false;
        synchronized (cvar) {
            esperando.add(Thread.currentThread());
            holds = lock.fullyReleaseWrite();
            boolean listo = false;
            while (!listo) {
                try {
                    cvar.wait();
                    listo = true;
                } catch (InterruptedException e) {
                    interrumpido = true;
                }
            }
            esperando.remove(Thread.currentThread());
        }
        lock.reacquireWrite(holds);
        if (interrumpido) {
            Thread.currentThread().interrupt();
        }
    }

    /** Espera con plazo en nanosegundos; devuelve lo que sobro. Ver `ReentrantCondition`. */
    public long awaitNanos(long nanosTimeout) throws InterruptedException {
        int holds;
        long sobrante;
        synchronized (cvar) {
            long arranque = System.nanoTime();
            esperando.add(Thread.currentThread());
            holds = lock.fullyReleaseWrite();
            try {
                long millis = nanosTimeout / 1000000L;
                int nanos = (int) (nanosTimeout % 1000000L);
                if (millis > 0L || nanos > 0) {
                    cvar.wait(millis, nanos);
                }
            } finally {
                esperando.remove(Thread.currentThread());
            }
            sobrante = nanosTimeout - (System.nanoTime() - arranque);
        }
        lock.reacquireWrite(holds);
        return sobrante;
    }

    /** Espera con plazo; `false` si se agoto. */
    public boolean await(long time, java.util.concurrent.TimeUnit unit)
            throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return this.awaitNanos(unit.toNanos(time)) > 0L;
    }

    /** Espera hasta una fecha del calendario; `false` si llego antes la fecha. */
    public boolean awaitUntil(java.util.Date deadline) throws InterruptedException {
        if (deadline == null) {
            throw new NullPointerException("deadline");
        }
        long falta = deadline.getTime() - System.currentTimeMillis();
        if (falta <= 0L) {
            return false;
        }
        this.awaitNanos(falta * 1000000L);
        return System.currentTimeMillis() < deadline.getTime();
    }

    // ---- lo que el lock necesita para sus consultas de inspeccion ---------------------------------

    boolean perteneceA(ReentrantReadWriteLock otro) {
        return this.lock == otro;
    }

    boolean hayEsperando() {
        boolean hay;
        synchronized (cvar) {
            hay = !esperando.isEmpty();
        }
        return hay;
    }

    int cuantosEsperan() {
        int n;
        synchronized (cvar) {
            n = esperando.size();
        }
        return n;
    }

    java.util.Collection<Thread> losQueEsperan() {
        java.util.ArrayList<Thread> copia;
        synchronized (cvar) {
            copia = new java.util.ArrayList<Thread>(esperando);
        }
        return copia;
    }

    public void signal() {
        if (!lock.writeHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        synchronized (cvar) {
            cvar.notify();
        }
    }

    public void signalAll() {
        if (!lock.writeHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        synchronized (cvar) {
            cvar.notifyAll();
        }
    }
}
