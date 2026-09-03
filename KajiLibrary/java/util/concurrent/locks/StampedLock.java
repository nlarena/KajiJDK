package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

// Un lock de lectura/escritura con **tres** modos, y el tercero es el que justifica la clase:
// ademas de la lectura y la escritura bloqueantes de siempre, ofrece la **lectura optimista**,
// que no toma nada. `tryOptimisticRead()` devuelve una foto del estado; el lector lee los campos
// que le interesan y despues pregunta `validate(sello)`. Si no hubo ninguna escritura en el
// medio, la respuesta es `true` y la lectura valio, sin haber escrito una sola palabra de
// memoria compartida. Si hubo, es `false` y hay que reintentar --o tomar el lock de verdad--.
//
// Cada operacion devuelve un **sello** (`long`), y ese sello es lo que se le pasa a `unlock`. No
// es reentrante: pedir dos veces el lock de escritura desde el mismo hilo se traba. Y no tiene
// condiciones: `newCondition()` de las vistas lanza `UnsupportedOperationException`, igual que en
// el JDK.
//
// ---------------------------------------------------------------------------------------------
// EL SELLO, QUE ES TODO EL DISENIO
// ---------------------------------------------------------------------------------------------
//
// Hay un solo `long state`, con el mismo reparto de bits que el JDK:
//
//     bits 0..6   cuenta de lectores (0..126); el 127 se reserva para la marca de desborde
//     bit  7      WBIT — hay un escritor
//     bits 8..63  numero de secuencia: **sube en uno cada vez que se suelta una escritura**
//
// De ahi salen las dos operaciones que hacen andar la lectura optimista:
//
//     tryOptimisticRead()  ->  state & SBITS      (la secuencia y el bit de escritura, sin lectores)
//     validate(sello)      ->  (sello & SBITS) == (state & SBITS)
//
// Y de ahi sale por que **el sello se puede validar de verdad**, que es lo unico que hace que
// este metodo sirva para algo: soltar una escritura hace `state += WBIT`, y esa suma apaga el
// bit 7 y **se lleva uno a la secuencia**. O sea que despues de cualquier escritura completa el
// `& SBITS` es distinto, y el sello viejo deja de validar. Un `tryOptimisticRead` que devolviera
// un numero que despues nadie compara con nada seria peor que no tenerlo: quien lo use va a creer
// que le esta preguntando algo al lock.
//
// El sello cero es el "no lo consegui" universal, y no puede colisionar con uno bueno porque el
// estado arranca en ORIGIN (256) y la secuencia nunca vuelve a cero salvo al dar la vuelta, caso
// en el que `soltarEscritura` la manda de nuevo a ORIGIN.
//
// ---------------------------------------------------------------------------------------------
// COMO SE BLOQUEA, Y LO QUE ESO CUESTA
// ---------------------------------------------------------------------------------------------
//
// El JDK trae su propia cola CLH adentro de esta clase. Aca el estado lo guarda un monitor
// interno (`sync`) y **la espera no**: quien no puede entrar se anota con un nodo propio
// (`SyncWaiter`), suelta `sync` y duerme en el monitor **de su nodo**; quien suelta le pone la
// bandera a todos los nodos anotados y los despierta, y cada uno vuelve a competir. Es correcto y
// es **no justo** -- que es exactamente lo que el javadoc del JDK promete para esta clase ("this
// class does not favor readers over writers, nor does it support fairness"). Lo que se pierde
// frente a la cola CLH es rendimiento bajo contencion, no semantica.
//
// Que la espera **no** sea un `sync.wait()`/`sync.notifyAll()` sobre el monitor del estado no es
// gusto: es que asi no anda. Nuestra VM deja al hilo adentro del conjunto de espera de un monitor
// cuando una espera **con plazo vence**, y entonces un `notifyAll()` posterior de ese mismo hilo
// sobre ese mismo monitor se despierta a si mismo y la VM se traba sin nadie ejecutable. Un
// `StampedLock` hace exactamente eso -- `tryReadLock(t, u)` que vence y despues `unlockWrite`--,
// asi que la primera version de esta clase se colgaba de forma reproducible. Repro minimo con
// ablacion en `scratchpad/zzlocks/WaitStale.java`. Con un nodo **nuevo por cada episodio de
// espera**, el monitor donde se duerme no lo vuelve a notificar nunca su propio dueno, y el
// defecto no se puede tocar.
//
// No se apoya en `AbstractQueuedSynchronizer` a proposito, igual que el JDK: los tres modos y la
// conversion entre ellos no entran en el contrato `tryAcquire`/`tryRelease`.
//
// Nota de estilo, la de siempre en este paquete: **ningun `return` adentro de un bloque
// `synchronized`** (finding #105 -- el javac congelado no emite el `monitorexit` de esa salida).
// Un `throw` adentro si es seguro.
public class StampedLock implements Serializable {

    // Un lector.
    private static final long RUNIT = 1L;
    // El bit del escritor.
    private static final long WBIT = 128L;
    // Los bits de la cuenta de lectores.
    private static final long RBITS = 127L;
    // El maximo de lectores que entran en esos bits; de ahi para arriba va al contador aparte.
    private static final long RFULL = 126L;
    // Todos los bits de "esta tomado": lectores y escritor.
    private static final long ABITS = 255L;
    // Los bits que forman el sello: el del escritor y la secuencia, sin la cuenta de lectores.
    // Es `~RBITS`, escrito como literal para que se vea que son los 57 de arriba mas el bit 7.
    private static final long SBITS = -128L;
    // El estado inicial. No es cero para que ningun sello legitimo pueda valer cero.
    private static final long ORIGIN = 256L;

    // El monitor interno: guarda `state`, `desborde`, la lista de esperantes y las vistas.
    // **No** es donde se duerme: cada esperante duerme en el monitor de su propio nodo.
    private final Object sync = new Object();

    private long state = ORIGIN;

    // Lectores que no entraron en los siete bits. Con esto la cuenta no tiene techo, que es lo
    // que hace el JDK con su `readerOverflow`.
    private int desborde;

    // Los que estan esperando para entrar. La guarda `sync`; cada elemento es el nodo de un
    // hilo dormido (o a punto de dormirse) en su propio monitor.
    private final java.util.ArrayList<SyncWaiter> esperando = new java.util.ArrayList<SyncWaiter>();

    // Las tres vistas, creadas la primera vez que se piden.
    private Lock vistaLectura;
    private Lock vistaEscritura;
    private ReadWriteLock vistaLE;

    /** Un lock nuevo, sin tomar. */
    public StampedLock() {
    }

    // ---- escritura ---------------------------------------------------------------------------

    /**
     * Toma el lock de escritura, esperando lo que haga falta.
     *
     * <p>**No** atiende interrupciones: si llega una, se anota y se le repone la bandera al hilo
     * al volver. Abortar aca dejaria al llamador sin el lock y creyendo que lo tiene.
     *
     * @return un sello de escritura (nunca cero)
     */
    public long writeLock() {
        long sello = 0L;
        boolean interrumpido = false;
        while (sello == 0L) {
            SyncWaiter nodo = null;
            synchronized (sync) {
                if ((state & ABITS) == 0L) {
                    sello = this.tomarEscritor();
                } else {
                    nodo = this.anotar();
                }
            }
            if (nodo != null) {
                if (this.dormir(nodo, false, 0L)) {
                    interrumpido = true;
                }
                this.sacar(nodo);
            }
        }
        if (interrumpido) {
            Thread.currentThread().interrupt();
        }
        return sello;
    }

    /**
     * Toma el lock de escritura, abortando si interrumpen al hilo.
     *
     * @return un sello de escritura (nunca cero)
     * @throws InterruptedException si interrumpen al hilo
     */
    public long writeLockInterruptibly() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        long sello = 0L;
        while (sello == 0L) {
            SyncWaiter nodo = null;
            synchronized (sync) {
                if ((state & ABITS) == 0L) {
                    sello = this.tomarEscritor();
                } else {
                    nodo = this.anotar();
                }
            }
            if (nodo != null) {
                boolean cortado = this.dormir(nodo, false, 0L);
                this.sacar(nodo);
                if (cortado) {
                    throw new InterruptedException();
                }
            }
        }
        return sello;
    }

    /**
     * Toma el lock de escritura solo si esta libre ahora mismo.
     *
     * @return el sello, o **cero** si no lo consiguio
     */
    public long tryWriteLock() {
        long sello;
        synchronized (sync) {
            sello = (state & ABITS) != 0L ? 0L : this.tomarEscritor();
        }
        return sello;
    }

    /**
     * Toma el lock de escritura esperando como mucho ese plazo.
     *
     * @return el sello, o **cero** si el plazo se agoto
     * @throws InterruptedException si interrumpen al hilo mientras espera
     */
    public long tryWriteLock(long time, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        long fin = System.nanoTime() + unit.toNanos(time);
        long sello = 0L;
        boolean seguir = true;
        while (seguir) {
            SyncWaiter nodo = null;
            synchronized (sync) {
                if ((state & ABITS) == 0L) {
                    sello = this.tomarEscritor();
                } else {
                    nodo = this.anotar();
                }
            }
            if (nodo == null) {
                seguir = false;
            } else if (fin - System.nanoTime() <= 0L) {
                this.sacar(nodo);
                seguir = false;
            } else {
                boolean cortado = this.dormir(nodo, true, fin);
                this.sacar(nodo);
                if (cortado) {
                    throw new InterruptedException();
                }
            }
        }
        return sello;
    }

    /**
     * Suelta el lock de escritura.
     *
     * @throws IllegalMonitorStateException si el sello no es el de la escritura vigente
     */
    public void unlockWrite(long stamp) {
        synchronized (sync) {
            if ((stamp & WBIT) == 0L || state != stamp) {
                throw new IllegalMonitorStateException();
            }
            this.soltarEscritura();
            this.despertar();
        }
    }

    /**
     * Suelta el lock de escritura **sin sello**, si lo tiene alguien.
     *
     * <p>Es la salida de emergencia que documenta el JDK: sirve para recuperarse de un error, no
     * para el uso normal, porque no comprueba que quien suelta sea quien tomo.
     *
     * @return `false` si no habia escritor
     */
    public boolean tryUnlockWrite() {
        boolean habia;
        synchronized (sync) {
            habia = (state & WBIT) != 0L;
            if (habia) {
                this.soltarEscritura();
                this.despertar();
            }
        }
        return habia;
    }

    // ---- lectura -----------------------------------------------------------------------------

    /**
     * Toma el lock de lectura, esperando lo que haga falta. No atiende interrupciones.
     *
     * @return un sello de lectura (nunca cero)
     */
    public long readLock() {
        long sello = 0L;
        boolean interrumpido = false;
        while (sello == 0L) {
            SyncWaiter nodo = null;
            synchronized (sync) {
                if ((state & WBIT) == 0L) {
                    sello = this.tomarLector();
                } else {
                    nodo = this.anotar();
                }
            }
            if (nodo != null) {
                if (this.dormir(nodo, false, 0L)) {
                    interrumpido = true;
                }
                this.sacar(nodo);
            }
        }
        if (interrumpido) {
            Thread.currentThread().interrupt();
        }
        return sello;
    }

    /**
     * Toma el lock de lectura, abortando si interrumpen al hilo.
     *
     * @return un sello de lectura (nunca cero)
     * @throws InterruptedException si interrumpen al hilo
     */
    public long readLockInterruptibly() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        long sello = 0L;
        while (sello == 0L) {
            SyncWaiter nodo = null;
            synchronized (sync) {
                if ((state & WBIT) == 0L) {
                    sello = this.tomarLector();
                } else {
                    nodo = this.anotar();
                }
            }
            if (nodo != null) {
                boolean cortado = this.dormir(nodo, false, 0L);
                this.sacar(nodo);
                if (cortado) {
                    throw new InterruptedException();
                }
            }
        }
        return sello;
    }

    /**
     * Toma el lock de lectura solo si no hay un escritor ahora mismo.
     *
     * @return el sello, o **cero** si no lo consiguio
     */
    public long tryReadLock() {
        long sello;
        synchronized (sync) {
            sello = (state & WBIT) != 0L ? 0L : this.tomarLector();
        }
        return sello;
    }

    /**
     * Toma el lock de lectura esperando como mucho ese plazo.
     *
     * @return el sello, o **cero** si el plazo se agoto
     * @throws InterruptedException si interrumpen al hilo mientras espera
     */
    public long tryReadLock(long time, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        long fin = System.nanoTime() + unit.toNanos(time);
        long sello = 0L;
        boolean seguir = true;
        while (seguir) {
            SyncWaiter nodo = null;
            synchronized (sync) {
                if ((state & WBIT) == 0L) {
                    sello = this.tomarLector();
                } else {
                    nodo = this.anotar();
                }
            }
            if (nodo == null) {
                seguir = false;
            } else if (fin - System.nanoTime() <= 0L) {
                this.sacar(nodo);
                seguir = false;
            } else {
                boolean cortado = this.dormir(nodo, true, fin);
                this.sacar(nodo);
                if (cortado) {
                    throw new InterruptedException();
                }
            }
        }
        return sello;
    }

    /**
     * Suelta el lock de lectura.
     *
     * @throws IllegalMonitorStateException si el sello no corresponde a una lectura vigente
     */
    public void unlockRead(long stamp) {
        synchronized (sync) {
            if ((stamp & RBITS) == 0L
                    || (state & SBITS) != (stamp & SBITS)
                    || (state & RBITS) == 0L) {
                throw new IllegalMonitorStateException();
            }
            this.soltarLectura();
            this.despertar();
        }
    }

    /**
     * Suelta **una** lectura sin sello, si hay alguna.
     *
     * @return `false` si no habia lectores
     */
    public boolean tryUnlockRead() {
        boolean habia;
        synchronized (sync) {
            habia = (state & RBITS) != 0L;
            if (habia) {
                this.soltarLectura();
                this.despertar();
            }
        }
        return habia;
    }

    // ---- lectura optimista -------------------------------------------------------------------

    /**
     * Una foto del estado, **sin tomar nada**.
     *
     * <p>El uso es siempre el mismo: se pide el sello, se leen los datos, y despues se llama a
     * {@link #validate}. Si devuelve `false` los datos leidos no valen nada y hay que reintentar
     * o tomar {@link #readLock}.
     *
     * @return el sello, o **cero** si hay un escritor (en cuyo caso no hay nada que optimizar)
     */
    public long tryOptimisticRead() {
        long sello;
        synchronized (sync) {
            sello = (state & WBIT) != 0L ? 0L : (state & SBITS);
        }
        return sello;
    }

    /**
     * Si desde que se saco `stamp` **no se completo ninguna escritura**.
     *
     * <p>Un sello cero nunca valida: el estado arranca en `ORIGIN`, asi que `state & SBITS` no
     * puede valer cero.
     */
    public boolean validate(long stamp) {
        boolean vale;
        synchronized (sync) {
            vale = (stamp & SBITS) == (state & SBITS);
        }
        return vale;
    }

    // ---- conversiones ------------------------------------------------------------------------
    //
    // Las tres siguen la misma forma: si el sello sigue siendo valido y el modo actual permite el
    // paso, se hace en **un solo paso** --sin soltar y volver a tomar, que es donde se colaria un
    // escritor-- y se devuelve el sello nuevo. Si no, cero, y el sello viejo sigue valiendo lo
    // que valia.

    /**
     * Pasa el sello a uno de escritura, si se puede sin soltar.
     *
     * @return el sello de escritura, o **cero** si no se pudo
     */
    public long tryConvertToWriteLock(long stamp) {
        long resultado = 0L;
        synchronized (sync) {
            if ((state & SBITS) == (stamp & SBITS)) {
                long a = stamp & ABITS;
                long m = state & ABITS;
                if (m == 0L) {
                    // Nadie lo tiene y el sello (optimista) sigue valiendo: se toma la escritura.
                    if (a == 0L) {
                        resultado = this.tomarEscritor();
                    }
                } else if (m == WBIT) {
                    // Ya es nuestro.
                    if (a == m) {
                        resultado = stamp;
                    }
                } else if (m == RUNIT && a != 0L) {
                    // Somos el unico lector: se pasa a escritor sin abrir la ventana.
                    state = state - RUNIT + WBIT;
                    resultado = state;
                }
            }
        }
        return resultado;
    }

    /**
     * Pasa el sello a uno de lectura, si se puede sin soltar.
     *
     * @return el sello de lectura, o **cero** si no se pudo
     */
    public long tryConvertToReadLock(long stamp) {
        long resultado = 0L;
        boolean avisar = false;
        synchronized (sync) {
            if ((state & SBITS) == (stamp & SBITS)) {
                long a = stamp & ABITS;
                long m = state & ABITS;
                if (m == 0L) {
                    if (a == 0L) {
                        resultado = this.tomarLector();
                    }
                } else if (m == WBIT) {
                    if (a == m) {
                        // Suelta la escritura y toma la lectura en la misma operacion: el `+WBIT`
                        // apaga el bit y adelanta la secuencia, el `+RUNIT` cuenta el lector.
                        state = state + WBIT + RUNIT;
                        resultado = state;
                        avisar = true;
                    }
                } else if (a != 0L && a < WBIT) {
                    // Ya es de lectura.
                    resultado = stamp;
                }
            }
            if (avisar) {
                this.despertar();
            }
        }
        return resultado;
    }

    /**
     * Suelta lo que el sello tenga tomado y devuelve un sello de lectura optimista.
     *
     * @return el sello de observacion, o **cero** si el sello ya no valia
     */
    public long tryConvertToOptimisticRead(long stamp) {
        long resultado = 0L;
        boolean avisar = false;
        synchronized (sync) {
            if ((state & SBITS) == (stamp & SBITS)) {
                long a = stamp & ABITS;
                long m = state & ABITS;
                if (m == 0L) {
                    if (a == 0L) {
                        resultado = state & SBITS;
                    }
                } else if (m == WBIT) {
                    if (a == m) {
                        this.soltarEscritura();
                        resultado = state & SBITS;
                        avisar = true;
                    }
                } else if (a != 0L && a < WBIT) {
                    this.soltarLectura();
                    resultado = state & SBITS;
                    avisar = true;
                }
            }
            if (avisar) {
                this.despertar();
            }
        }
        return resultado;
    }

    /**
     * Suelta lo que sea que el sello tenga tomado.
     *
     * @throws IllegalMonitorStateException si el sello no es de un lock tomado
     */
    public void unlock(long stamp) {
        long a = stamp & ABITS;
        if (a == WBIT) {
            this.unlockWrite(stamp);
        } else if (a != 0L && a < WBIT) {
            this.unlockRead(stamp);
        } else {
            throw new IllegalMonitorStateException();
        }
    }

    // ---- consultas de estado -----------------------------------------------------------------
    //
    // Son **fotos**, como todas las de este paquete: sirven para diagnosticar, no para decidir.

    /** Si hay un escritor. */
    public boolean isWriteLocked() {
        boolean r;
        synchronized (sync) {
            r = (state & WBIT) != 0L;
        }
        return r;
    }

    /** Si hay al menos un lector. */
    public boolean isReadLocked() {
        boolean r;
        synchronized (sync) {
            r = (state & RBITS) != 0L;
        }
        return r;
    }

    /** Cuantos lectores hay, contando los del desborde. */
    public int getReadLockCount() {
        long n;
        synchronized (sync) {
            n = (state & RBITS) + (long) desborde;
        }
        return (int) n;
    }

    // ---- los sellos, vistos desde afuera -----------------------------------------------------
    //
    // Las cuatro son estaticas y solo miran los bits: no preguntan nada a ningun lock, asi que un
    // sello de un `StampedLock` se puede clasificar sin tener el lock a mano.

    /** Si el sello es de un lock tomado (lectura o escritura). */
    public static boolean isLockStamp(long stamp) {
        return (stamp & ABITS) != 0L;
    }

    /** Si el sello es de escritura. */
    public static boolean isWriteLockStamp(long stamp) {
        return (stamp & ABITS) == WBIT;
    }

    /** Si el sello es de lectura. */
    public static boolean isReadLockStamp(long stamp) {
        return (stamp & RBITS) != 0L;
    }

    /** Si el sello es de lectura optimista (uno valido que no tiene nada tomado). */
    public static boolean isOptimisticReadStamp(long stamp) {
        return (stamp & ABITS) == 0L && stamp != 0L;
    }

    // ---- las vistas `Lock` -------------------------------------------------------------------

    /**
     * Una vista {@link Lock} que toma y suelta la lectura.
     *
     * <p>Su `newCondition()` lanza `UnsupportedOperationException`: este lock no tiene
     * condiciones, y devolver una que no funcione seria mentir.
     */
    public Lock asReadLock() {
        Lock v;
        synchronized (sync) {
            if (vistaLectura == null) {
                vistaLectura = new VistaLectura();
            }
            v = vistaLectura;
        }
        return v;
    }

    /** Una vista {@link Lock} que toma y suelta la escritura. */
    public Lock asWriteLock() {
        Lock v;
        synchronized (sync) {
            if (vistaEscritura == null) {
                vistaEscritura = new VistaEscritura();
            }
            v = vistaEscritura;
        }
        return v;
    }

    /** Las dos vistas juntas, como un {@link ReadWriteLock}. */
    public ReadWriteLock asReadWriteLock() {
        ReadWriteLock v;
        synchronized (sync) {
            if (vistaLE == null) {
                vistaLE = new VistaLE();
            }
            v = vistaLE;
        }
        return v;
    }

    // ---- la maquinaria del estado ------------------------------------------------------------
    //
    // Las cuatro suponen que quien llama **ya tiene** `sync`. No lo toman ellas para que la
    // operacion completa --comprobar y cambiar-- sea un solo paso indivisible.

    private long tomarEscritor() {
        state = state + WBIT;
        return state;
    }

    private void soltarEscritura() {
        long siguiente = state + WBIT;
        // La suma apaga el bit 7 y se lleva uno a la secuencia: por eso todo sello anterior deja
        // de validar. Si la secuencia dio la vuelta se vuelve a ORIGIN, para que ningun sello
        // legitimo pueda valer cero.
        state = siguiente == 0L ? ORIGIN : siguiente;
    }

    private long tomarLector() {
        if ((state & RBITS) < RFULL) {
            state = state + RUNIT;
        } else {
            // Ya no entran mas en los siete bits: van al contador aparte, y el sello queda con
            // los bits de lectores llenos (que sigue siendo distinto de cero, que es lo unico
            // que `unlockRead` necesita mirar).
            desborde++;
        }
        return state;
    }

    private void soltarLectura() {
        if (desborde > 0 && (state & RBITS) == RFULL) {
            desborde--;
        } else {
            state = state - RUNIT;
        }
    }

    // ---- la espera --------------------------------------------------------------------------
    //
    // Un nodo **nuevo por cada episodio**: se anota, se suelta `sync`, se duerme en el monitor del
    // nodo, y al despertar se lo saca. El nodo no se reusa, y esa es la propiedad que esquiva el
    // defecto de la VM descrito en el encabezado -- un monitor donde una espera con plazo pudo
    // vencer no lo vuelve a notificar nunca el hilo que espero en el.
    //
    // El orden de toma es siempre `sync` y despues el monitor de un nodo, nunca al reves: quien
    // espera suelta `sync` **antes** de tomar el suyo. Por eso no hay abrazo mortal.

    // Anota un nodo para el hilo actual. Quien llama ya tiene `sync`.
    private SyncWaiter anotar() {
        SyncWaiter nodo = new SyncWaiter();
        nodo.hilo = Thread.currentThread();
        esperando.add(nodo);
        return nodo;
    }

    // Lo saca de la lista. Toma `sync` el mismo.
    private void sacar(SyncWaiter nodo) {
        synchronized (sync) {
            esperando.remove(nodo);
        }
        nodo.hilo = null;
    }

    /**
     * Duerme en el monitor del nodo hasta que lo despierten (o venza el plazo, o lo interrumpan).
     *
     * @return `true` si lo corto una interrupcion
     */
    private boolean dormir(SyncWaiter nodo, boolean conPlazo, long finNanos) {
        boolean cortado = false;
        synchronized (nodo) {
            // Comprobar la bandera y dormirse pasan adentro del mismo monitor que usa `despertar`:
            // por eso una senial que llego antes no se pierde.
            if (conPlazo) {
                long restan = finNanos - System.nanoTime();
                while (!nodo.liberado && !cortado && restan > 0L) {
                    try {
                        nodo.wait(this.enMilis(restan));
                    } catch (InterruptedException e) {
                        cortado = true;
                    }
                    restan = finNanos - System.nanoTime();
                }
            } else if (!nodo.liberado) {
                try {
                    nodo.wait();
                } catch (InterruptedException e) {
                    cortado = true;
                }
            }
        }
        return cortado;
    }

    // Despierta a todos los anotados: cada uno vuelve a competir por el estado. Quien llama ya
    // tiene `sync`. Despertar a todos y no a uno es lo correcto aca: soltar una escritura puede
    // dejar pasar a **muchos** lectores, y esta clase no promete justicia.
    private void despertar() {
        for (int i = 0; i < esperando.size(); i++) {
            SyncWaiter nodo = esperando.get(i);
            synchronized (nodo) {
                nodo.liberado = true;
                nodo.notifyAll();
            }
        }
    }

    // Un plazo en nanos, pasado a los milisegundos que pide `Object.wait`. Nunca cero: un
    // `wait(0)` espera para siempre, y un plazo de menos de un milisegundo se redondea al
    // minimo que se puede pedir.
    private long enMilis(long nanos) {
        long ms = nanos / 1000000L;
        if (ms <= 0L) {
            ms = 1L;
        }
        return ms;
    }

    // =========================================================================================
    // Las vistas
    // =========================================================================================

    final class VistaLectura implements Lock {

        public void lock() {
            readLock();
        }

        public void lockInterruptibly() throws InterruptedException {
            readLockInterruptibly();
        }

        public boolean tryLock() {
            return tryReadLock() != 0L;
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return tryReadLock(time, unit) != 0L;
        }

        public void unlock() {
            if (!tryUnlockRead()) {
                throw new IllegalMonitorStateException();
            }
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class VistaEscritura implements Lock {

        public void lock() {
            writeLock();
        }

        public void lockInterruptibly() throws InterruptedException {
            writeLockInterruptibly();
        }

        public boolean tryLock() {
            return tryWriteLock() != 0L;
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return tryWriteLock(time, unit) != 0L;
        }

        public void unlock() {
            if (!tryUnlockWrite()) {
                throw new IllegalMonitorStateException();
            }
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class VistaLE implements ReadWriteLock {

        public Lock readLock() {
            return asReadLock();
        }

        public Lock writeLock() {
            return asWriteLock();
        }
    }
}
