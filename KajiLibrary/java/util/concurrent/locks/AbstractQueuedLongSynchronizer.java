package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

// El gemelo de `AbstractQueuedSynchronizer` con el estado en un `long` en vez de un `int`. Es la
// misma clase, miembro por miembro, con `long` donde el otro dice `int`: existe porque hay
// sincronizadores cuyo significado no entra en 32 bits --el caso de manual es `StampedLock`, que
// mete en la misma palabra un contador de lectores y un numero de secuencia de escrituras-- y
// partirlo en dos campos perderia la atomicidad de leerlos juntos.
//
// **Toda la explicacion de como funciona esto esta en el encabezado de
// `AbstractQueuedSynchronizer`**, y no se repite aca: la decision de no reconstruir
// `ReentrantLock`/`ReentrantReadWriteLock` sobre el armazon, por que la cola FIFO y no una pila,
// por que se duerme en el monitor de cada nodo y no en `LockSupport.park`, y la regla de estilo
// del finding #105 (ningun `return` adentro de un `synchronized`).
//
// En el JDK las dos clases tampoco comparten codigo: la larga es una copia generada de la corta.
// Aca es lo mismo, y por la misma razon -- Java no tiene forma de parametrizar una clase por un
// tipo primitivo, y un `AbstractQueuedSynchronizer<T>` con `Long` en caja cambiaria el contrato
// de los `try*` de la subclase, que es justamente lo que hay que respetar.
//
// La unica diferencia que **no** es el tipo: el constructor de esta es `public` y el de la corta
// `protected`. Es asi en el JDK y se copia tal cual; un modificador de acceso es parte del
// contrato, no un detalle.
public abstract class AbstractQueuedLongSynchronizer extends AbstractOwnableSynchronizer
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
    private long state;

    // La cola FIFO de esperantes, y su largo (para no recorrerla al preguntarlo).
    private SyncWaiter primero;
    private SyncWaiter ultimo;
    private int encolados;

    // Si alguna vez alguien tuvo que encolarse. Es exactamente `hasContended()`.
    private boolean hubo;

    /** Un `state` inicial de cero. Solo para uso de las subclases. */
    public AbstractQueuedLongSynchronizer() {
    }

    // ---- el estado -------------------------------------------------------------------------

    /** El valor actual del estado de sincronizacion. */
    protected final long getState() {
        long s;
        synchronized (sync) {
            s = state;
        }
        return s;
    }

    /** Fija el estado de sincronizacion. */
    protected final void setState(long newState) {
        synchronized (sync) {
            state = newState;
        }
    }

    /**
     * Fija el estado a `update` **si y solo si** vale `expect`, en un solo paso indivisible.
     *
     * @return `true` si lo cambio
     */
    protected final boolean compareAndSetState(long expect, long update) {
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
    protected boolean tryAcquire(long arg) {
        throw new UnsupportedOperationException();
    }

    /** Intenta soltar en modo exclusivo; `true` si el sincronizador quedo libre. */
    protected boolean tryRelease(long arg) {
        throw new UnsupportedOperationException();
    }

    /** Intenta adquirir en modo compartido; negativo si hay que esperar. */
    protected long tryAcquireShared(long arg) {
        throw new UnsupportedOperationException();
    }

    /** Intenta soltar en modo compartido; `true` si puede dejar pasar a algun esperante. */
    protected boolean tryReleaseShared(long arg) {
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
    public final void acquire(long arg) {
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
    public final void acquireInterruptibly(long arg) throws InterruptedException {
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
    public final boolean tryAcquireNanos(long arg, long nanosTimeout) throws InterruptedException {
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
    public final boolean release(long arg) {
        boolean libre = tryRelease(arg);
        if (libre) {
            senialarPrimero();
        }
        return libre;
    }

    // ---- modo compartido ---------------------------------------------------------------------

    /** Adquiere en modo compartido, sin atender interrupciones. */
    public final void acquireShared(long arg) {
        if (tryAcquireShared(arg) < 0L) {
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
    public final void acquireSharedInterruptibly(long arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(arg) < 0L) {
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
    public final boolean tryAcquireSharedNanos(long arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        boolean ok = tryAcquireShared(arg) >= 0L;
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
    public final boolean releaseShared(long arg) {
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
    private int esperar(long arg, boolean compartido, boolean interrumpible, boolean conPlazo,
                        long finNanos) {
        SyncWaiter nodo = encolar(compartido);
        int resultado = -1;
        boolean interrumpido = false;
        while (resultado < 0) {
            boolean logrado = false;
            if (esPrimero(nodo)) {
                logrado = compartido ? tryAcquireShared(arg) >= 0L : tryAcquire(arg);
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
    AbstractQueuedLongSynchronizer esteSincronizador() {
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
            long guardado = this.soltarTodo();
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
            long guardado = this.soltarTodo();
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
            long guardado = this.soltarTodo();
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
        private long soltarTodo() {
            long guardado = getState();
            if (!esteSincronizador().release(guardado)) {
                throw new IllegalMonitorStateException();
            }
            return guardado;
        }

        // ---- lo que el sincronizador necesita para contestar sus consultas -------------------

        boolean perteneceA(AbstractQueuedLongSynchronizer otro) {
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
