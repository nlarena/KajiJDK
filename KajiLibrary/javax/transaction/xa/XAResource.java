package javax.transaction.xa;

/**
 * KajiLibrary's javax.transaction.xa.XAResource -- un recurso que sabe participar de una transaccion
 * que abarca a otros.
 *
 * <p>Es el **compromiso en dos fases** puesto en una interfaz: primero se le pregunta a cada
 * participante si puede confirmar ({@link #prepare}), y solo si todos dicen que si se les ordena
 * hacerlo ({@link #commit}). Esa separacion es lo que permite que dos bases distintas se confirmen
 * como si fueran una: entre el "puedo" y el "hacelo" ya no queda nada que pueda fallar del lado del
 * recurso.
 *
 * <p>El precio esta en {@link XAException#XA_HEURMIX}: si un participante se cansa de esperar entre
 * las dos fases y decide solo, la atomicidad se rompe y ningun protocolo la recupera.
 */
public interface XAResource {

    /** La rama se puede confirmar. */
    int XA_OK = 0;

    /** La rama era de solo lectura: ya esta, no hace falta segunda fase. */
    int XA_RDONLY = 3;

    /** Sin banderas. */
    int TMNOFLAGS = 0;

    /** Unirse a una rama que ya existe. */
    int TMJOIN = 2097152;

    /** Terminar de recorrer las transacciones en duda. */
    int TMENDRSCAN = 8388608;

    /** El trabajo de la rama fallo. */
    int TMFAIL = 536870912;

    /** Confirmar en una sola fase: se puede solo si el recurso es el unico participante. */
    int TMONEPHASE = 1073741824;

    /** Retomar una rama suspendida. */
    int TMRESUME = 134217728;

    /** Empezar a recorrer las transacciones en duda. */
    int TMSTARTRSCAN = 16777216;

    /** El trabajo de la rama termino bien. */
    int TMSUCCESS = 67108864;

    /** Suspender la rama sin terminarla. */
    int TMSUSPEND = 33554432;

    /** Empieza el trabajo de esa rama. */
    void start(Xid xid, int flags) throws XAException;

    /** Termina el trabajo de esa rama. */
    void end(Xid xid, int flags) throws XAException;

    /**
     * Primera fase: si el recurso puede confirmar.
     *
     * @return {@link #XA_OK}, o {@link #XA_RDONLY} si no habia nada que escribir
     */
    int prepare(Xid xid) throws XAException;

    /** Segunda fase: confirmar. `onePhase` saltea la primera, valido solo si no hay otros. */
    void commit(Xid xid, boolean onePhase) throws XAException;

    /** Deshacer la rama. */
    void rollback(Xid xid) throws XAException;

    /** Olvidar una rama que se decidio por cuenta propia. */
    void forget(Xid xid) throws XAException;

    /**
     * Las transacciones que quedaron **en duda**.
     *
     * <p>Es como se sale de una caida entre las dos fases: al reiniciar, el coordinador pregunta que
     * quedo preparado sin resolver y lo termina.
     */
    Xid[] recover(int flag) throws XAException;

    /** Si este recurso y el otro son el mismo gestor -- decide si comparten rama. */
    boolean isSameRM(XAResource xares) throws XAException;

    int getTransactionTimeout() throws XAException;

    boolean setTransactionTimeout(int seconds) throws XAException;
}
