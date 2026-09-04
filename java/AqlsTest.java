import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;

// Prueba de comportamiento de `AbstractQueuedLongSynchronizer`, el gemelo de AQS con el estado en
// un `long`. Compilada contra el del JDK y corrida sobre el nuestro: las dos VMs tienen que dar lo
// mismo. Devuelve **-1 si esta todo bien**; si no, el numero del paso que fallo.
//
// El estado que usa el mutex de aca no es 0/1 sino 0 y `0x1_0000_0000L` -- un valor que **no entra
// en 32 bits**. Es a proposito: es la unica diferencia real entre esta clase y AQS, y una copia mal
// hecha (un `int` que se colo en el medio) se veria justo ahi y en ningun otro lado.

class AqlsMutex extends AbstractQueuedLongSynchronizer {

    // El bit 32: si algo por el camino trunca a `int`, este valor se vuelve cero.
    static final long TOMADO = 4294967296L;

    protected boolean tryAcquire(long arg) {
        boolean ok = compareAndSetState(0L, TOMADO);
        if (ok) {
            setExclusiveOwnerThread(Thread.currentThread());
        }
        return ok;
    }

    protected boolean tryRelease(long arg) {
        setExclusiveOwnerThread(null);
        setState(0L);
        return true;
    }

    protected boolean isHeldExclusively() {
        return getExclusiveOwnerThread() == Thread.currentThread();
    }

    long estado() {
        return getState();
    }
}

// Una puerta compartida con el mismo truco: cerrada = el bit 32 puesto.
class AqlsPuerta extends AbstractQueuedLongSynchronizer {

    protected long tryAcquireShared(long arg) {
        return getState() == 0L ? 1L : -1L;
    }

    protected boolean tryReleaseShared(long arg) {
        setState(0L);
        return true;
    }

    void cerrar() {
        setState(AqlsMutex.TOMADO);
    }
}

class AqlsWorker extends Thread {
    AqlsMutex mutex;
    int[] compartido;
    int vueltas;

    public void run() {
        for (int i = 0; i < this.vueltas; i++) {
            this.mutex.acquire(1L);
            this.compartido[0] = this.compartido[0] + 1;
            this.mutex.release(1L);
        }
    }
}

class AqlsPuertaWorker extends Thread {
    AqlsPuerta puerta;
    int[] compartido;

    public void run() {
        this.puerta.acquireShared(1L);
        synchronized (this.compartido) {
            this.compartido[0] = this.compartido[0] + 1;
        }
    }
}

class AqlsIntrWorker extends Thread {
    AqlsMutex mutex;
    // 1 = lanzo InterruptedException (lo correcto), 0 = adquirio.
    volatile int resultado = -9;

    public void run() {
        try {
            this.mutex.acquireInterruptibly(1L);
            this.resultado = 0;
            this.mutex.release(1L);
        } catch (InterruptedException e) {
            this.resultado = 1;
        }
    }
}

public class AqlsTest {

    public static int run() throws Exception {
        // --- 1. El estado es de verdad un `long` ----------------------------------------------
        AqlsMutex mutex = new AqlsMutex();
        mutex.acquire(1L);
        if (mutex.estado() != AqlsMutex.TOMADO) {
            return 1; // truncado a int: el bit 32 se perdio
        }
        mutex.release(1L);
        if (mutex.estado() != 0L) {
            return 2;
        }

        // --- 2. Exclusion: tres hilos, 100 incrementos no atomicos cada uno --------------------
        int[] compartido = new int[1];
        AqlsWorker a = new AqlsWorker();
        AqlsWorker b = new AqlsWorker();
        AqlsWorker c = new AqlsWorker();
        a.mutex = mutex; a.compartido = compartido; a.vueltas = 100;
        b.mutex = mutex; b.compartido = compartido; b.vueltas = 100;
        c.mutex = mutex; c.compartido = compartido; c.vueltas = 100;
        a.start(); b.start(); c.start();
        a.join(); b.join(); c.join();
        if (compartido[0] != 300) {
            return 3;
        }

        // --- 3. Modo compartido ----------------------------------------------------------------
        AqlsPuerta puerta = new AqlsPuerta();
        puerta.cerrar();
        int[] pasaron = new int[1];
        AqlsPuertaWorker p1 = new AqlsPuertaWorker();
        AqlsPuertaWorker p2 = new AqlsPuertaWorker();
        AqlsPuertaWorker p3 = new AqlsPuertaWorker();
        p1.puerta = puerta; p1.compartido = pasaron;
        p2.puerta = puerta; p2.compartido = pasaron;
        p3.puerta = puerta; p3.compartido = pasaron;
        p1.start(); p2.start(); p3.start();
        boolean encolados = false;
        for (int i = 0; i < 2000000 && !encolados; i++) {
            encolados = puerta.getQueueLength() == 3;
            if (!encolados) {
                Thread.yield();
            }
        }
        if (!encolados) {
            return 4;
        }
        puerta.releaseShared(1L);
        p1.join(); p2.join(); p3.join();
        if (pasaron[0] != 3) {
            return 5;
        }

        // --- 4. Consultas de la cola, interrupcion y plazo -------------------------------------
        mutex.acquire(1L);
        AqlsIntrWorker mirado = new AqlsIntrWorker();
        mirado.mutex = mutex;
        mirado.start();
        boolean uno = false;
        for (int i = 0; i < 2000000 && !uno; i++) {
            uno = mutex.getQueueLength() == 1;
            if (!uno) {
                Thread.yield();
            }
        }
        if (!uno) {
            return 6;
        }
        if (!mutex.hasQueuedThreads() || !mutex.hasContended()) {
            return 7;
        }
        if (mutex.getFirstQueuedThread() != mirado || !mutex.isQueued(mirado)) {
            return 8;
        }
        if (mutex.getExclusiveQueuedThreads().size() != 1
                || mutex.getSharedQueuedThreads().size() != 0) {
            return 9;
        }
        mirado.interrupt();
        mirado.join();
        if (mirado.resultado != 1) {
            return 10;
        }
        if (mutex.getQueueLength() != 0) {
            return 11;
        }
        // Con el mutex tomado por nosotros, un plazo tiene que vencer.
        if (mutex.tryAcquireNanos(1L, 5000000L)) {
            return 12;
        }
        mutex.release(1L);
        if (!mutex.tryAcquireNanos(1L, 5000000L)) {
            return 13;
        }
        mutex.release(1L);

        return -1;
    }
}
