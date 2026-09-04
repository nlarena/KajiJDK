package java.rmi;

/**
 * KajiLibrary's java.rmi.ConnectIOException -- Fallo la entrada/salida de la conexion.
 *
 * <p>La conexion se establecio y despues algo se rompio en el camino. A diferencia de
 * {@link ConnectException}, aca el servidor existe y responde, asi que reintentar puede tener
 * sentido.
 */
public class ConnectIOException extends RemoteException {

    private static final long serialVersionUID = -8087809532704668744L;

    /** @param s el mensaje */
    public ConnectIOException(String s) {
        super(s);
    }

    /**
     * @param s el mensaje
     * @param ex la causa
     */
    public ConnectIOException(String s, Exception ex) {
        super(s, ex);
    }
}
