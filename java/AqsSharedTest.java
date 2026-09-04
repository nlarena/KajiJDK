import java.util.concurrent.locks.AbstractQueuedSynchronizer;

// Prueba de comportamiento de `AbstractQueuedSynchronizer` mas alla del modo exclusivo simple que
// ya cubre `AqsLockTest`: modo **compartido**, **plazo**, **interrupcion** y las consultas de la
// cola. Compilada contra el AQS real y corrida sobre el nuestro, asi que las dos VMs tienen que
// dar lo mismo.
//
// La `ConditionObject` **no** se prueba aca y no es porque no ande: el javac congelado no sabe
// instanciar una clase interna heredada (`new ConditionObject()` adentro de una subclase de AQS,
// que es el idioma del propio JDK), ni en su forma calificada `x.new ConditionObject()`. Repro y
// ablacion en `scratchpad/zzlocks/InnerHer.java`; la prueba de la condicion vive en
// `scratchpad/zzlocks/AqsCondProbe.java`, compilada con el javac del JDK y corrida sobre nuestra
// VM, hasta que ese defecto se arregle.
//
// Devuelve **-1 si esta todo bien**; si no, el numero del paso que fallo. Todo lo que espera lo
// hace esperando una **condicion** (un latch, un contador, el largo de la cola), nunca durmiendo
// y confiando: los totales son exactos.

// Contador con metodos `synchronized` -- el metodo entero, no un bloque: el finding #105 solo
// afecta a un `return` adentro de un bloque `synchronized`.
class AqsContador {
    private int n;
    private int pico;

    synchronized void subir() {
        n++;
        if (n > pico) {
            pico = n;
        }
    }

    synchronized void bajar() {
        n--;
    }

    synchronized int valor() {
        return n;
    }

    synchronized int pico() {
        return pico;
    }
}

// El mutex no reentrante del propio javadoc de AQS, con el dueno anotado en
// `AbstractOwnableSynchronizer` -- que es lo que hace que `isHeldExclusively` (y con el la
// condicion) diga la verdad.
class AqsMutex extends AbstractQueuedSynchronizer {

    protected boolean tryAcquire(int arg) {
        boolean ok = compareAndSetState(0, 1);
        if (ok) {
            setExclusiveOwnerThread(Thread.currentThread());
        }
        return ok;
    }

    protected boolean tryRelease(int arg) {
        setExclusiveOwnerThread(null);
        setState(0);
        return true;
    }

    protected boolean isHeldExclusively() {
        return getExclusiveOwnerThread() == Thread.currentThread();
    }
}

// Una puerta: cerrada (`state` 1) hasta que alguien la abre, y entonces pasan todos.
class AqsPuerta extends AbstractQueuedSynchronizer {

    protected int tryAcquireShared(int arg) {
        return getState() == 0 ? 1 : -1;
    }

    protected boolean tryReleaseShared(int arg) {
        setState(0);
        return true;
    }

    void cerrar() {
        setState(1);
    }
}

// Un semaforo de N permisos, en modo compartido.
class AqsSemaforo extends AbstractQueuedSynchronizer {

    protected int tryAcquireShared(int n) {
        int r = 0;
        boolean listo = false;
        while (!listo) {
            int hay = getState();
            int quedan = hay - n;
            if (quedan < 0) {
                r = quedan;
                listo = true;
            } else if (compareAndSetState(hay, quedan)) {
                r = quedan;
                listo = true;
            }
        }
        return r;
    }

    protected boolean tryReleaseShared(int n) {
        boolean listo = false;
        while (!listo) {
            int hay = getState();
            if (compareAndSetState(hay, hay + n)) {
                listo = true;
            }
        }
        return true;
    }

    void cargar(int permisos) {
        setState(permisos);
    }

    int permisos() {
        return getState();
    }
}

class AqsPuertaWorker extends Thread {
    AqsPuerta puerta;
    AqsContador contador;

    public void run() {
        this.puerta.acquireShared(1);
        this.contador.subir();
    }
}

class AqsSemWorker extends Thread {
    AqsSemaforo sem;
    AqsContador dentro;
    AqsContador total;
    int vueltas;

    public void run() {
        for (int i = 0; i < this.vueltas; i++) {
            this.sem.acquireShared(1);
            this.dentro.subir();
            Thread.yield();
            this.dentro.bajar();
            this.total.subir();
            this.sem.releaseShared(1);
        }
    }
}

class AqsPlazoWorker extends Thread {
    AqsMutex mutex;
    // 0 = devolvio false al vencer el plazo (lo correcto), 1 = lo adquirio, 2 = lanzo.
    volatile int resultado = -9;

    public void run() {
        try {
            this.resultado = this.mutex.tryAcquireNanos(1, 20000000L) ? 1 : 0;
        } catch (InterruptedException e) {
            this.resultado = 2;
        }
    }
}

class AqsIntrWorker extends Thread {
    AqsMutex mutex;
    // 1 = lanzo InterruptedException (lo correcto), 0 = adquirio.
    volatile int resultado = -9;

    public void run() {
        try {
            this.mutex.acquireInterruptibly(1);
            this.resultado = 0;
            this.mutex.release(1);
        } catch (InterruptedException e) {
            this.resultado = 1;
        }
    }
}

public class AqsSharedTest {

    // Espera a que la cola del sincronizador tenga al menos `n`. Con tope, para que una prueba
    // rota falle en vez de colgarse.
    static boolean esperarCola(AbstractQueuedSynchronizer s, int n) {
        for (int i = 0; i < 2000000; i++) {
            if (s.getQueueLength() >= n) {
                return true;
            }
            Thread.yield();
        }
        return false;
    }

    public static int run() throws Exception {
        // --- 1. Modo compartido: la puerta suelta a los tres de una ---------------------------
        AqsPuerta puerta = new AqsPuerta();
        puerta.cerrar();
        AqsContador pasaron = new AqsContador();
        AqsPuertaWorker p1 = new AqsPuertaWorker();
        AqsPuertaWorker p2 = new AqsPuertaWorker();
        AqsPuertaWorker p3 = new AqsPuertaWorker();
        p1.puerta = puerta; p1.contador = pasaron;
        p2.puerta = puerta; p2.contador = pasaron;
        p3.puerta = puerta; p3.contador = pasaron;
        p1.start(); p2.start(); p3.start();
        if (!esperarCola(puerta, 3)) {
            return 1;
        }
        puerta.releaseShared(1);
        p1.join(); p2.join(); p3.join();
        if (pasaron.valor() != 3) {
            return 2;
        }

        // --- 2. Semaforo de 2 permisos: 4 x 50 vueltas ----------------------------------------
        // El total tiene que ser exacto (200) y nunca puede haber habido mas de 2 adentro.
        AqsSemaforo sem = new AqsSemaforo();
        sem.cargar(2);
        AqsContador dentro = new AqsContador();
        AqsContador total = new AqsContador();
        AqsSemWorker[] ws = new AqsSemWorker[4];
        for (int i = 0; i < 4; i++) {
            ws[i] = new AqsSemWorker();
            ws[i].sem = sem;
            ws[i].dentro = dentro;
            ws[i].total = total;
            ws[i].vueltas = 50;
        }
        for (int i = 0; i < 4; i++) {
            ws[i].start();
        }
        for (int i = 0; i < 4; i++) {
            ws[i].join();
        }
        if (total.valor() != 200) {
            return 3;
        }
        if (dentro.pico() > 2) {
            return 4;
        }
        if (sem.permisos() != 2) {
            return 5; // todos los permisos tienen que haber vuelto
        }

        // --- 3. Consultas de la cola con un hilo realmente bloqueado --------------------------
        AqsMutex mutex = new AqsMutex();
        mutex.acquire(1);
        AqsIntrWorker mirado = new AqsIntrWorker();
        mirado.mutex = mutex;
        mirado.start();
        if (!esperarCola(mutex, 1)) {
            return 6;
        }
        if (!mutex.hasQueuedThreads()) {
            return 7;
        }
        if (!mutex.hasContended()) {
            return 8;
        }
        if (mutex.getFirstQueuedThread() != mirado) {
            return 9;
        }
        if (!mutex.isQueued(mirado)) {
            return 10;
        }
        if (mutex.getQueuedThreads().size() != 1) {
            return 11;
        }
        if (mutex.getExclusiveQueuedThreads().size() != 1) {
            return 12;
        }
        if (mutex.getSharedQueuedThreads().size() != 0) {
            return 13;
        }
        if (!mutex.hasQueuedPredecessors()) {
            return 14;
        }

        // --- 4. Interrupcion: el mismo hilo, ahora interrumpido -------------------------------
        mirado.interrupt();
        mirado.join();
        if (mirado.resultado != 1) {
            return 15;
        }
        if (mutex.getQueueLength() != 0) {
            return 16; // el nodo del interrumpido tiene que haber salido de la cola
        }

        // --- 5. Plazo: `tryAcquireNanos` sobre un mutex tomado tiene que vencer ---------------
        AqsPlazoWorker plazo = new AqsPlazoWorker();
        plazo.mutex = mutex;
        plazo.start();
        plazo.join();
        if (plazo.resultado != 0) {
            return 17;
        }
        mutex.release(1);
        // Y con el mutex libre, el mismo plazo tiene que conseguirlo.
        if (!mutex.tryAcquireNanos(1, 20000000L)) {
            return 18;
        }
        mutex.release(1);

        return -1;
    }
}
