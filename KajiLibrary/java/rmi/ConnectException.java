package java.rmi;

/**
 * KajiLibrary's java.rmi.ConnectException -- No se pudo llegar al servidor.
 *
 * <p>La conexion no se pudo <b>establecer</b>: no hay nadie escuchando, o el cortafuegos la corto.
 * Distinta de {@link ConnectIOException}, que es cuando la conexion se establecio y despues fallo.
 *
 * <p>La diferencia importa para reintentar: esta suele significar que el servidor no esta levantado, y
 * reintentar enseguida no va a cambiar nada.
 */
public class ConnectException extends RemoteException {

    private static final long serialVersionUID = 4863550261346652506L;

    /** @param s el mensaje */
    public ConnectException(String s) {
        super(s);
    }

    /**
     * @param s el mensaje
     * @param ex la causa
     */
    public ConnectException(String s, Exception ex) {
        super(s, ex);
    }
}
