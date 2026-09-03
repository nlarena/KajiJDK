package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

// El armazon sobre el que el JDK construye casi todos sus sincronizadores. La subclase guarda su
// significado en un `int state` --que solo toca por los tres accesores de aca-- e implementa los
// metodos `try*`; este da todo lo demas: la cola de espera, el bloqueo, el despertar, la
// interrupcion y el plazo. Dos modos:
//
//   exclusivo  (`acquire`/`release`)             — un dueno a la vez, como un `ReentrantLock`.
//   compartido (`acquireShared`/`releaseShared`) — varios a la vez, como un `Semaphore` o un
//                                                  `CountDownLatch`.
//
// ---------------------------------------------------------------------------------------------
// LA DECISION: `ReentrantLock` y `ReentrantReadWriteLock` NO se reconstruyeron sobre esta clase
// ---------------------------------------------------------------------------------------------
//
// En el JDK los dos se apoyan en AQS. Aca no, y sigue siendo asi a proposito. Las razones, en
// orden de peso:
//
//  1. **La relacion es invisible desde el contrato.** En el JDK, AQS entra a `ReentrantLock` por
//     una clase anidada `private static class Sync extends AbstractQueuedSynchronizer`. Ningun
//     miembro `public` ni `protected` de `ReentrantLock` la menciona, ni la devuelve, ni la toma.
//     O sea que "estar construido sobre AQS" no es parte de lo que el paquete promete: es una
//     eleccion de implementacion, y la regla de la casa dice que los internos son libres.
//
//  2. **Lo que hay hoy anda, y anda medido.** `ReentrantLock` esta 20/20 y
//     `ReentrantReadWriteLock` 22/22, y de ellos cuelga media `java.util.concurrent`:
//     `CountDownLatch`, `CyclicBarrier`, `ArrayBlockingQueue`, `DelayQueue`, `Semaphore`. Son
//     cinco pruebas de comportamiento que hoy pasan con las tres substancias de hilos, la
//     paralela real incluida. Reescribirlas sobre una clase recien nacida arriesga las cinco
//     **sin mover el numero ni un miembro**: el contrato de los dos locks ya esta completo.
//
//  3. **Esta clase no queda como andamio sin probar por eso.** `java/AqsLockTest.java` construye
//     el mutex del propio javadoc de AQS (`tryAcquire` con `compareAndSetState`, `tryRelease` con
//     `setState`) y hace 300 incrementos no atomicos guardados; `java/AqsSharedTest.java` cubre el
//     modo compartido, el plazo, la interrupcion y las consultas de la cola. Las dos corren con
//     las dos VMs. La `ConditionObject` se prueba en `scratchpad/zzlocks/AqsCondProbe.java`, que
//     esta ahi y no en `java/` porque el javac congelado no sabe instanciar una clase interna
//     heredada (repro con ablacion en `scratchpad/zzlocks/InnerHer.java`); se compila con el
//     javac del JDK y el `.class` se corre con las dos VMs igual que las otras.
//
//     Y hay una comprobacion mas fuerte que las tres: los `.class` de este paquete se pueden
//     meter en la JVM real con `--patch-module java.base=<dir>`, y las tres pruebas dan lo mismo
//     ahi -- o sea que este AQS corre tambien con hilos del sistema operativo y paralelismo de
//     verdad, no solo con nuestro planificador.
//
// El argumento del otro lado --que reconstruirlos seria mas fiel-- es cierto y no alcanza: la
// fidelidad que se gana no se puede observar desde afuera, y la que se arriesga si.
//
// ---------------------------------------------------------------------------------------------
// COMO BLOQUEA, Y POR QUE NO CON `LockSupport.park`
// ---------------------------------------------------------------------------------------------
//
// El JDK estaciona a sus esperantes con `LockSupport.park`. Aca cada nodo de la cola es **su
// propio monitor**: el hilo duerme en `nodo.wait()` y quien lo libera hace
// `synchronized (nodo) { nodo.liberado = true; nodo.notifyAll(); }`. El permiso del nodo
// (`liberado`) da la misma garantia que el de `park` --una senial que llega antes de que el hilo
// se duerma no se pierde, porque las dos cosas pasan adentro del mismo monitor-- y ademas da las
// dos que `park` no puede dar en esta VM:
//
//   - **plazo**: `Object.wait(ms)` existe; un `park` con deadline no. Sin esto no habria
//     `tryAcquireNanos` ni `tryAcquireSharedNanos`.
//   - **interrupcion con la forma correcta**: `wait` lanza `InterruptedException`, que es lo que
//     `acquireInterruptibly` necesita. Nuestro `park`, interrumpido, lanza tambien -- pero lanza
//     *siempre*, incluso donde el contrato dice que tiene que retornar, y sin declararlo
//     (ver el encabezado de `LockSupport`). Un `acquire(int)` no interrumpible construido sobre
//     el moriria con una `InterruptedException` indeclarada.
//
// `LockSupport` queda igual como lo que es --la primitiva de la VM, completa y probada-- y esta
// clase no se apoya en ella. Es un detalle interno; el contrato no dice con que se duerme.
//
// ---------------------------------------------------------------------------------------------
// LA COLA
// ---------------------------------------------------------------------------------------------
//
// FIFO doblemente enlazada, guardada por un monitor interno (`sync`), y **solo el primero de la
// cola intenta adquirir**. Eso hace que `getFirstQueuedThread`, `hasQueuedPredecessors` y
// `getQueueLength` digan la verdad: sobre una pila de Treiber --que seria mas corta de escribir--
// "el primero" no significa nada. Que un hilo **recien llegado** pueda colarse antes que la cola
// no es un defecto sino el comportamiento del JDK: `acquire` prueba `tryAcquire` antes de
// encolarse, y la subclase que quiera ser justa lo evita consultando `hasQueuedPredecessors`.
//
// Nota de estilo, la misma que en `ReentrantLock`: **ningun `return` adentro de un bloque
// `synchronized`** (finding #105 -- el javac congelado no emite el `monitorexit` de esa salida y
// el monitor queda filtrado). Todo se calcula en un local adentro y se devuelve afuera. Un
// `throw` adentro si es seguro: el manejador que genera el compilador lo suelta.
public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer
        implements Serializable {

    // Los resultados posibles de una espera en la cola. Un `int` y no tres `boolean` porque la
    // espera tiene exactamente cuatro finales y nombrarlos evita la combinacion imposible.
    private static final int LOGRADO = 0;
    private static final int LOGRADO_CON_INTERRUPCION = 1;
    private static final int INTERRUMPIDO = 2;
    private static final int VENCIO = 3;

    // El monitor interno. Guarda `state` y los enlaces de la cola, y nada mas.
    //
    // Dos reglas que lo hacen seguro, y que valen para cada linea de esta clase:
    //   - **nunca** se lo tiene tomado mientras corre un `try*` de la subclase (que va a llamar a
    //     `compareAndSetState`, o sea a tomarlo de nuevo, y que puede hacer cualquier cosa);
    //   - **nunca** se lo tiene tomado mientras un hilo duerme.
    // Con esas dos, el unico orden de toma que existe es `sync` y despues el monitor de un nodo,
    // nunca al reves, asi que no hay abrazo mortal posible entre los dos.
    private final Object sync = new Object();

    // El estado de sincronizacion. Su significado lo pone la subclase.
    private int state;

    // La cola FIFO de esperantes, y su largo (para no recorrerla al preguntarlo).
    private SyncWaiter primero;
    private SyncWaiter ultimo;
    private int encolados;

    // Si alguna vez alguien tuvo que encolarse. Es exactamente `hasContended()`.
    private boolean hubo;

    /** Un `state` inicial de cero. Solo para uso de las subclases. */
    protected AbstractQueuedSynchronizer() {
    }

    // ---- el estado -------------------------------------------------------------------------

    /** El valor actual del estado de sincronizacion. */
    protected final int getState() {
        int s;
        synchronized (sync) {
            s = state;
        }
        return s;
    }

    /** Fija el estado de sincronizacion. */
    protected final void setState(int newState) {
        synchronized (sync) {
            state = newState;
        }
    }

    /**
     * Fija el estado a `update` **si y solo si** vale `expect`, en un solo paso indivisible.
     *
     * @return `true` si lo cambio
     */
    protected final boolean compareAndSetState(int expect, int update) {
        boolean ok;
        synchronized (sync) {
            ok = state == expect;
            if (ok) {
                state = update;
            }
        }
        return ok;
    }

    // ---- lo que la subclase implementa -------------------------------------------------------
    //
    // Concretos y no abstractos, como en el JDK: una subclase implementa solo el modo que usa, y
    // el otro par tiene que seguir siendo instanciable. Lanzan `UnsupportedOperationException`
    // porque llegar aca significa que alguien pidio un modo que la subclase no soporta -- y esa
    // es la respuesta correcta, no `false`, que se veria como "no pude todavia" y encolaria al
    // hilo para siempre.

    /** Intenta adquirir en modo exclusivo. */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /** Intenta soltar en modo exclusivo; `true` si el sincronizador quedo libre. */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /** Intenta adquirir en modo compartido; negativo si hay que esperar. */
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /** Intenta soltar en modo compartido; `true` si puede dejar pasar a algun esperante. */
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /** Si el hilo actual lo tiene tomado en exclusiva. Solo lo necesita `ConditionObject`. */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    // ---- modo exclusivo ----------------------------------------------------------------------

    /**
     * Adquiere en modo exclusivo, **sin** atender interrupciones.
     *
     * <p>Si llega una interrupcion mientras espera no se pierde ni corta la adquisicion: se anota
     * y se le vuelve a poner la bandera al hilo antes de volver. Abortar aca dejaria al llamador
     * sin el lock y creyendo que lo tiene.
     */
    public final void acquire(int arg) {
        if (!tryAcquire(arg)) {
            if (esperar(arg, false, false, false, 0L) == LOGRADO_CON_INTERRUPCION) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Adquiere en modo exclusivo, abortando si interrumpen al hilo.
     *
     * @throws InterruptedException si interrumpen al hilo
     */
    public final void acquireInterruptibly(int arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!tryAcquire(arg)) {
            if (esperar(arg, false, true, false, 0L) == INTERRUMPIDO) {
                throw new InterruptedException();
            }
        }
    }

    /**
     * Adquiere en modo exclusivo esperando como mucho `nanosTimeout`.
     *
     * @return `false` si el plazo se agoto sin adquirir
     * @throws InterruptedException si interrumpen al hilo mientras espera
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        boolean ok = tryAcquire(arg);
        if (!ok) {
            int r = esperar(arg, false, true, true, System.nanoTime() + nanosTimeout);
            if (r == INTERRUMPIDO) {
                throw new InterruptedException();
            }
            ok = r != VENCIO;
        }
        return ok;
    }

    /**
     * Suelta en modo exclusivo y, si el sincronizador quedo libre, le cede el turno al primero de
     * la cola.
     *
     * @return lo que devolvio `tryRelease`
     */
    public final boolean release(int arg) {
        boolean libre = tryRelease(arg);
        if (libre) {
            senialarPrimero();
        }
        return libre;
    }

    // ---- modo compartido ---------------------------------------------------------------------

    /** Adquiere en modo compartido, sin atender interrupciones. */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0) {
            if (esperar(arg, true, false, false, 0L) == LOGRADO_CON_INTERRUPCION) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Adquiere en modo compartido, abortando si interrumpen al hilo.
     *
     * @throws InterruptedException si interrumpen al hilo
     */
    public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(arg) < 0) {
            if (esperar(arg, true, true, false, 0L) == INTERRUMPIDO) {
                throw new InterruptedException();
            }
        }
    }

    /**
     * Adquiere en modo compartido esperando como mucho `nanosTimeout`.
     *
     * @return `false` si el plazo se agoto sin adquirir
     * @throws InterruptedException si interrumpen al hilo mientras espera
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        boolean ok = tryAcquireShared(arg) >= 0;
        if (!ok) {
            int r = esperar(arg, true, true, true, System.nanoTime() + nanosTimeout);
            if (r == INTERRUMPIDO) {
                throw new InterruptedException();
            }
            ok = r != VENCIO;
        }
        return ok;
    }

    /** Suelta en modo compartido y despierta al primero de la cola. */
    public final boolean releaseShared(int arg) {
        boolean paso = tryReleaseShared(arg);
        if (paso) {
            senialarPrimero();
        }
        return paso;
    }

    // ---- el motor: encolarse, dormir, reintentar ---------------------------------------------

    /**
     * El unico lugar donde esta clase espera. Encola al hilo actual y no vuelve hasta que
     * adquirio, lo interrumpieron (y se pidio que eso corte) o vencio el plazo.
     *
     * <p>El `try*` de la subclase se llama **sin** el monitor interno tomado -- si no, un
     * `compareAndSetState` de adentro lo volveria a tomar y, peor, se estaria corriendo codigo
     * ajeno con un lock del sincronizador en la mano.
     */
    private int esperar(int arg, boolean compartido, boolean interrumpible, boolean conPlazo,
                        long finNanos) {
        SyncWaiter nodo = encolar(compartido);
        int resultado = -1;
        boolean interrumpido = false;
        while (resultado < 0) {
            boolean logrado = false;
            if (esPrimero(nodo)) {
                logrado = compartido ? tryAcquireShared(arg) >= 0 : tryAcquire(arg);
            }
            if (logrado) {
                desencolar(nodo);
                resultado = interrumpido ? LOGRADO_CON_INTERRUPCION : LOGRADO;
            } else if (conPlazo && finNanos - System.nanoTime() <= 0L) {
                desencolar(nodo);
                resultado = VENCIO;
            } else if (dormir(nodo, conPlazo, finNanos)) {
                if (interrumpible) {
                    desencolar(nodo);
                    resultado = INTERRUMPIDO;
                } else {
                    // No interrumpible: se anota y se sigue esperando. La bandera se repone
                    // arriba, cuando la adquisicion termine.
                    interrumpido = true;
                }
            }
        }
        return resultado;
    }

    /**
     * Duerme en el monitor del nodo hasta que lo liberen (o venza el plazo, o lo interrumpan).
     *
     * @return `true` si lo corto una interrupcion
     */
    private boolean dormir(SyncWaiter nodo, boolean conPlazo, long finNanos) {
        boolean cortado = false;
        synchronized (nodo) {
            // La comprobacion del permiso y el dormirse pasan adentro del mismo monitor que usa
            // el que libera: por eso no hay despertar perdido.
            if (!nodo.liberado) {
                try {
                    if (conPlazo) {
                        long restan = finNanos - System.nanoTime();
                        if (restan > 0L) {
                            long ms = restan / 1000000L;
                            // Un `wait(0)` espera para siempre; un plazo de menos de un
                            // milisegundo se redondea al minimo que se puede pedir.
                            if (ms <= 0L) {
                                ms = 1L;
                            }
                            nodo.wait(ms);
                        }
                    } else {
                        nodo.wait();
                    }
                } catch (InterruptedException e) {
                    cortado = true;
                }
            }
            nodo.liberado = false;
        }
        return cortado;
    }

    // ---- la cola ------------------------------------------------------------------------------

    private SyncWaiter encolar(boolean compartido) {
        SyncWaiter nodo = new SyncWaiter();
        nodo.hilo = Thread.currentThread();
        nodo.compartido = compartido;
        synchronized (sync) {
            nodo.encolado = true;
            nodo.anterior = ultimo;
            if (ultimo == null) {
                primero = nodo;
            } else {
                ultimo.siguiente = nodo;
            }
            ultimo = nodo;
            encolados++;
            hubo = true;
        }
        return nodo;
    }

    private boolean esPrimero(SyncWaiter nodo) {
        boolean r;
        synchronized (sync) {
            r = primero == nodo;
        }
        return r;
    }

    /**
     * Saca el nodo de la cola y le cede el turno al que quede primero.
     *
     * <p>Se llama tanto cuando el nodo **logro** adquirir como cuando **abandona** (plazo o
     * interrupcion), y en los dos casos hay que senialar al nuevo primero: en el primero porque
     * en modo compartido puede quedar cupo para el siguiente, y en el segundo porque el que se va
     * podia ser el unico que tenia derecho a intentar. No senialar en el caso de abandono deja la
     * cola clavada -- es el error clasico de este armazon.
     */
    private void desencolar(SyncWaiter nodo) {
        SyncWaiter nuevoPrimero = null;
        synchronized (sync) {
            if (nodo.encolado) {
                nodo.encolado = false;
                if (nodo.anterior == null) {
                    primero = nodo.siguiente;
                } else {
                    nodo.anterior.siguiente = nodo.siguiente;
                }
                if (nodo.siguiente == null) {
                    ultimo = nodo.anterior;
                } else {
                    nodo.siguiente.anterior = nodo.anterior;
                }
                nodo.siguiente = null;
                nodo.anterior = null;
                nodo.hilo = null;
                encolados--;
                nuevoPrimero = primero;
            }
        }
        if (nuevoPrimero != null) {
            senialar(nuevoPrimero);
        }
    }

    private void senialarPrimero() {
        SyncWaiter n;
        synchronized (sync) {
            n = primero;
        }
        if (n != null) {
            senialar(n);
        }
    }

    // Le deja el permiso al nodo y lo despierta. `notifyAll` y no `notify` porque el mismo
    // monitor lo usa la `ConditionObject` para su propia espera.
    private void senialar(SyncWaiter nodo) {
        synchronized (nodo) {
            nodo.liberado = true;
            nodo.notifyAll();
        }
    }

    // ---- inspeccion de la cola ---------------------------------------------------------------
    //
    // Todas son **fotos**, y el javadoc del JDK insiste en eso: sirven para diagnosticar y para
    // heuristicas, nunca para decidir. Para cuando la respuesta llegue, la cola puede ser otra.

    /** Si hay algun hilo esperando para adquirir. */
    public final boolean hasQueuedThreads() {
        boolean hay;
        synchronized (sync) {
            hay = primero != null;
        }
        return hay;
    }

    /** Si alguna vez **alguien** tuvo que encolarse. Nunca vuelve a `false`. */
    public final boolean hasContended() {
        boolean h;
        synchronized (sync) {
            h = hubo;
        }
        return h;
    }

    /** El primero de la cola, o `null` si esta vacia. */
    public final Thread getFirstQueuedThread() {
        Thread t;
        synchronized (sync) {
            t = primero == null ? null : primero.hilo;
        }
        return t;
    }

    /**
     * Si ese hilo esta en la cola.
     *
     * @throws NullPointerException si `thread` es `null`
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null) {
            throw new NullPointerException("thread");
        }
        boolean esta = false;
        synchronized (sync) {
            SyncWaiter n = primero;
            while (n != null && !esta) {
                esta = n.hilo == thread;
                n = n.siguiente;
            }
        }
        return esta;
    }

    /**
     * Si hay algun hilo esperando **por delante** del actual.
     *
     * <p>Es la consulta que una subclase justa hace adentro de su `tryAcquire`: adquirir solo si
     * devuelve `false` convierte la politica de "el que llega se cuela" en FIFO estricta.
     */
    public final boolean hasQueuedPredecessors() {
        Thread yo = Thread.currentThread();
        boolean hay;
        synchronized (sync) {
            hay = primero != null && primero.hilo != yo;
        }
        return hay;
    }

    /** Cuantos esperan para adquirir. */
    public final int getQueueLength() {
        int n;
        synchronized (sync) {
            n = encolados;
        }
        return n;
    }

    /** Los hilos que esperan para adquirir. Una copia; la cola interna no sale de aca. */
    public final Collection<Thread> getQueuedThreads() {
        return this.recolectar(false, false);
    }

    /** Los que esperan en modo exclusivo. */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        return this.recolectar(true, false);
    }

    /** Los que esperan en modo compartido. */
    public final Collection<Thread> getSharedQueuedThreads() {
        return this.recolectar(true, true);
    }

    private Collection<Thread> recolectar(boolean filtrar, boolean compartido) {
        ArrayList<Thread> out = new ArrayList<Thread>();
        synchronized (sync) {
            SyncWaiter n = primero;
            while (n != null) {
                if ((!filtrar || n.compartido == compartido) && n.hilo != null) {
                    out.add(n.hilo);
                }
                n = n.siguiente;
            }
        }
        return out;
    }

    // ---- inspeccion de las condiciones -------------------------------------------------------
    //
    // Las cuatro piden que la condicion sea **de este** sincronizador. Preguntarle a uno por una
    // condicion ajena no tiene respuesta correcta, y devolver "ninguno" seria peor que fallar.

    /**
     * Si esa condicion se creo sobre este sincronizador.
     *
     * @throws NullPointerException si `condition` es `null`
     */
    public final boolean owns(ConditionObject condition) {
        if (condition == null) {
            throw new NullPointerException("condition");
        }
        return condition.perteneceA(this);
    }

    /**
     * Si alguien espera en esa condicion.
     *
     * @throws IllegalMonitorStateException si el hilo actual no tiene la exclusiva
     * @throws IllegalArgumentException si la condicion no es de este sincronizador
     */
    public final boolean hasWaiters(ConditionObject condition) {
        return this.mia(condition).hayEsperando();
    }

    /**
     * Cuantos esperan en esa condicion.
     *
     * @throws IllegalMonitorStateException si el hilo actual no tiene la exclusiva
     * @throws IllegalArgumentException si la condicion no es de este sincronizador
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        return this.mia(condition).cuantosEsperan();
    }

    /**
     * Los hilos que esperan en esa condicion.
     *
     * @throws IllegalMonitorStateException si el hilo actual no tiene la exclusiva
     * @throws IllegalArgumentException si la condicion no es de este sincronizador
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        return this.mia(condition).losQueEsperan();
    }

    private ConditionObject mia(ConditionObject condition) {
        if (condition == null) {
            throw new NullPointerException("condition");
        }
        if (!condition.perteneceA(this)) {
            throw new IllegalArgumentException("not owner");
        }
        if (!isHeldExclusively()) {
            throw new IllegalMonitorStateException();
        }
        return condition;
    }

    // El `this` del sincronizador, visto desde la clase interna. Existe porque la
    // `ConditionObject` necesita comparar su dueno y llamar a `acquire`/`release`, y una llamada
    // sin calificar desde adentro de la interna ya resuelve al externo.
    AbstractQueuedSynchronizer esteSincronizador() {
        return this;
    }

    // =========================================================================================
    // La condicion
    // =========================================================================================

    /**
     * Una {@link Condition} sobre un sincronizador en modo exclusivo.
     *
     * <p>El protocolo es el de siempre: `await` **suelta el sincronizador entero** --sea cual sea
     * la profundidad reentrante, guardando el `state` para reponerlo-- y duerme; `signal`
     * despierta a uno, que vuelve a adquirir antes de retornar. Cada esperante duerme en su
     * propio nodo, igual que en la cola de adquisicion, con la misma garantia contra el despertar
     * perdido.
     */
    public class ConditionObject implements Condition, Serializable {

        // Los que esperan en esta condicion, en orden de llegada. Lo guarda el monitor de la
        // lista (`cola`), que es distinto del monitor interno del sincronizador y del de cada
        // nodo -- y nunca se toma teniendo alguno de esos dos.
        private final ArrayList<SyncWaiter> cola = new ArrayList<SyncWaiter>();

        public ConditionObject() {
        }

        /**
         * Suelta el sincronizador y espera hasta que la senialen.
         *
         * @throws InterruptedException si interrumpen al hilo mientras espera
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            SyncWaiter nodo = this.agregar();
            int guardado = this.soltarTodo();
            boolean cortado = false;
            boolean senialado = false;
            while (!senialado && !cortado) {
                synchronized (nodo) {
                    if (!nodo.liberado) {
                        try {
                            nodo.wait();
                        } catch (InterruptedException e) {
                            cortado = true;
                        }
                    }
                    senialado = nodo.liberado;
                }
            }
            this.quitar(nodo);
            esteSincronizador().acquire(guardado);
            if (cortado) {
                // Si tambien llego la senial, la interrupcion no se puede lanzar sin perderla:
                // se repone la bandera y el que llama la vera en su proxima espera. Es el
                // reparto que hace el JDK entre THROW_IE y REINTERRUPT.
                if (senialado) {
                    Thread.currentThread().interrupt();
                } else {
                    throw new InterruptedException();
                }
            }
        }

        /**
         * Espera **sin** poder ser interrumpida.
         *
         * <p>La interrupcion no se pierde: se atrapa, se sigue esperando, y al final se le repone
         * la bandera al hilo.
         */
        public final void awaitUninterruptibly() {
            SyncWaiter nodo = this.agregar();
            int guardado = this.soltarTodo();
            boolean interrumpido = false;
            boolean senialado = false;
            while (!senialado) {
                synchronized (nodo) {
                    if (!nodo.liberado) {
                        try {
                            nodo.wait();
                        } catch (InterruptedException e) {
                            interrumpido = true;
                        }
                    }
                    senialado = nodo.liberado;
                }
            }
            this.quitar(nodo);
            esteSincronizador().acquire(guardado);
            if (interrumpido) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Espera con plazo en nanosegundos y devuelve **lo que sobro**.
         *
         * <p>Devolver el sobrante y no un `boolean` es lo que la hace util en un bucle: una espera
         * puede despertar sin senial y hay que volver a esperar, pero solo por el resto.
         *
         * @return los nanosegundos que sobraron; cero o menos si el plazo se agoto
         * @throws InterruptedException si interrumpen al hilo mientras espera
         */
        public final long awaitNanos(long nanosTimeout) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            long fin = System.nanoTime() + nanosTimeout;
            SyncWaiter nodo = this.agregar();
            int guardado = this.soltarTodo();
            boolean cortado = false;
            boolean senialado = false;
            boolean vencio = false;
            while (!senialado && !cortado && !vencio) {
                synchronized (nodo) {
                    if (!nodo.liberado) {
                        long restan = fin - System.nanoTime();
                        if (restan <= 0L) {
                            vencio = true;
                        } else {
                            long ms = restan / 1000000L;
                            if (ms <= 0L) {
                                ms = 1L;
                            }
                            try {
                                nodo.wait(ms);
                            } catch (InterruptedException e) {
                                cortado = true;
                            }
                        }
                    }
                    senialado = nodo.liberado;
                }
                if (!senialado && !cortado && fin - System.nanoTime() <= 0L) {
                    vencio = true;
                }
            }
            this.quitar(nodo);
            esteSincronizador().acquire(guardado);
            if (cortado) {
                if (senialado) {
                    Thread.currentThread().interrupt();
                } else {
                    throw new InterruptedException();
                }
            }
            return fin - System.nanoTime();
        }

        /**
         * Espera con plazo.
         *
         * @return `false` si el plazo se agoto antes de la senial
         * @throws InterruptedException si interrumpen al hilo mientras espera
         */
        public final boolean await(long time, TimeUnit unit) throws InterruptedException {
            if (unit == null) {
                throw new NullPointerException("unit");
            }
            return this.awaitNanos(unit.toNanos(time)) > 0L;
        }

        /**
         * Espera hasta una fecha.
         *
         * <p>Aca **si** se usa el reloj de pared, y tiene que ser asi: el plazo esta expresado
         * como un momento del calendario, no como una duracion. La consecuencia es la del
         * contrato: si alguien corre el reloj del sistema, esta espera se mueve con el.
         *
         * @return `false` si la fecha llego antes de la senial
         * @throws InterruptedException si interrumpen al hilo mientras espera
         */
        public final boolean awaitUntil(java.util.Date deadline) throws InterruptedException {
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

        /**
         * Despierta al que lleva mas tiempo esperando.
         *
         * @throws IllegalMonitorStateException si el hilo actual no tiene la exclusiva
         */
        public final void signal() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            SyncWaiter n = null;
            synchronized (cola) {
                if (!cola.isEmpty()) {
                    n = cola.get(0);
                }
            }
            if (n != null) {
                synchronized (n) {
                    n.liberado = true;
                    n.notifyAll();
                }
            }
        }

        /**
         * Despierta a todos los que esperan.
         *
         * @throws IllegalMonitorStateException si el hilo actual no tiene la exclusiva
         */
        public final void signalAll() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            ArrayList<SyncWaiter> copia;
            synchronized (cola) {
                copia = new ArrayList<SyncWaiter>(cola);
            }
            for (int i = 0; i < copia.size(); i++) {
                SyncWaiter n = copia.get(i);
                synchronized (n) {
                    n.liberado = true;
                    n.notifyAll();
                }
            }
        }

        // ---- la maquinaria ------------------------------------------------------------------

        private SyncWaiter agregar() {
            SyncWaiter nodo = new SyncWaiter();
            nodo.hilo = Thread.currentThread();
            synchronized (cola) {
                cola.add(nodo);
            }
            return nodo;
        }

        private void quitar(SyncWaiter nodo) {
            synchronized (cola) {
                cola.remove(nodo);
            }
            nodo.hilo = null;
        }

        /**
         * Suelta el sincronizador **entero**, guardando el `state` para reponerlo despues.
         *
         * @throws IllegalMonitorStateException si el hilo actual no lo tiene tomado
         */
        private int soltarTodo() {
            int guardado = getState();
            if (!esteSincronizador().release(guardado)) {
                throw new IllegalMonitorStateException();
            }
            return guardado;
        }

        // ---- lo que el sincronizador necesita para contestar sus consultas -------------------

        boolean perteneceA(AbstractQueuedSynchronizer otro) {
            return esteSincronizador() == otro;
        }

        boolean hayEsperando() {
            boolean hay;
            synchronized (cola) {
                hay = !cola.isEmpty();
            }
            return hay;
        }

        int cuantosEsperan() {
            int n;
            synchronized (cola) {
                n = cola.size();
            }
            return n;
        }

        Collection<Thread> losQueEsperan() {
            ArrayList<Thread> out = new ArrayList<Thread>();
            synchronized (cola) {
                for (int i = 0; i < cola.size(); i++) {
                    Thread t = cola.get(i).hilo;
                    if (t != null) {
                        out.add(t);
                    }
                }
            }
            return out;
        }
    }
}
