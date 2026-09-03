package java.util.concurrent.locks;

// The {@link Condition} returned by {@link ReentrantLock#newCondition}. A top-level
// package-private class (not an inner class) that holds its owning lock explicitly —
// avoiding enclosing-instance capture, which the compiler does not yet generate reliably.
//
// The await protocol is lost-wakeup-free: the waiter enters the condition monitor
// (`cvar`) BEFORE releasing the lock, so a signaller — which must take `cvar` to notify —
// cannot slip its signal in between the release and the park. `cvar.wait()` then atomically
// releases `cvar` and parks; the signal can only land while the waiter is parked.
class ReentrantCondition implements Condition {

    private final ReentrantLock lock;
    private final Object cvar = new Object();
    // Quienes estan esperando en esta condicion. Se guarda la lista y no un contador porque
    // `getWaitingThreads` pide los hilos; el contador saldria de esta igual.
    private final java.util.ArrayList<Thread> esperando = new java.util.ArrayList<Thread>();

    ReentrantCondition(ReentrantLock lock) {
        this.lock = lock;
    }

    // Declara `throws InterruptedException`, como el JDK. La nota que estaba aca decia que no lo
    // hacia para esquivar el #104 --el lector de clases del javac congelado ignoraba el atributo
    // `Exceptions` de un metodo del classpath, asi que un `throws` igual se leia como **mas ancho** y
    // se rechazaba--. Ese finding se cerro, y de paso `Object.wait()` paso a verse como lo que es:
    // una espera **interrumpible**. Tragarse esa interrupcion seria quitarle al que llama la unica
    // forma de sacar a un hilo de una espera.
    public void await() throws InterruptedException {
        int holds;
        synchronized (cvar) {
            esperando.add(Thread.currentThread());
            holds = lock.fullyRelease();
            cvar.wait();
            esperando.remove(Thread.currentThread());
        }
        lock.reacquire(holds);
    }

    /**
     * Espera **sin** poder ser interrumpida.
     *
     * <p>La interrupcion no se pierde: se atrapa, se sigue esperando, y al final se vuelve a marcar
     * el hilo como interrumpido. Es la diferencia entre "no me interrumpe" y "me trago la
     * interrupcion" -- lo segundo deja al hilo sin saber que alguien le pidio parar.
     */
    public void awaitUninterruptibly() {
        int holds;
        boolean interrumpido = false;
        synchronized (cvar) {
            esperando.add(Thread.currentThread());
            holds = lock.fullyRelease();
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
        lock.reacquire(holds);
        if (interrumpido) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Espera con plazo, en nanosegundos, y devuelve **lo que sobro**.
     *
     * <p>El plazo se mide con `System.nanoTime()` y no con el reloj de pared: es el unico que no
     * salta si alguien cambia la hora del sistema, y una espera que se acorta o se alarga porque
     * corrieron el reloj es un error muy dificil de encontrar.
     */
    public long awaitNanos(long nanosTimeout) throws InterruptedException {
        int holds;
        long sobrante;
        synchronized (cvar) {
            long arranque = System.nanoTime();
            esperando.add(Thread.currentThread());
            holds = lock.fullyRelease();
            long millis = nanosTimeout / 1000000L;
            int nanos = (int) (nanosTimeout % 1000000L);
            if (millis > 0L || nanos > 0) {
                cvar.wait(millis, nanos);
            }
            esperando.remove(Thread.currentThread());
            sobrante = nanosTimeout - (System.nanoTime() - arranque);
        }
        lock.reacquire(holds);
        return sobrante;
    }

    /** Espera con plazo; `false` si el plazo se agoto. */
    public boolean await(long time, java.util.concurrent.TimeUnit unit)
            throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return this.awaitNanos(unit.toNanos(time)) > 0L;
    }

    /**
     * Espera hasta una fecha.
     *
     * <p>Aca **si** se usa el reloj de pared, y tiene que ser asi: el plazo esta expresado como un
     * momento del calendario, no como una duracion. La consecuencia es la del contrato -- si alguien
     * corre el reloj del sistema, esta espera se mueve con el.
     */
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

    // ---- lo que el lock necesita para contestar sus consultas de inspeccion -----------------------

    boolean perteneceA(ReentrantLock otro) {
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
        if (!lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        synchronized (cvar) {
            cvar.notify();
        }
    }

    public void signalAll() {
        if (!lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        synchronized (cvar) {
            cvar.notifyAll();
        }
    }
}
