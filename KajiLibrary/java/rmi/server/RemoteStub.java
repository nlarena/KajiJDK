package java.rmi.server;

/**
 * La base de los stubs generados por {@code rmic}.
 *
 * <p>No agrega comportamiento sobre {@link RemoteObject}: lo que aporta es el <strong>tipo</strong>,
 * que era como el runtime reconocia un stub generado.
 *
 * @deprecated los proxies dinamicos generan el stub en tiempo de ejecucion y no heredan de esto.
 */
@Deprecated(since = "1.5")
public abstract class RemoteStub extends RemoteObject {

    private static final long serialVersionUID = -1585587260594494182L;

    /** Sin referencia. */
    protected RemoteStub() {
        super();
    }

    /** Con esa referencia. */
    protected RemoteStub(RemoteRef ref) {
        super(ref);
    }

    /**
     * Le pone la referencia a un stub.
     *
     * @deprecated hay que pasarla por el constructor
     */
    @Deprecated(since = "1.2")
    protected static void setRef(RemoteStub stub, RemoteRef ref) {
        throw new UnsupportedOperationException("la referencia va por el constructor");
    }
}
