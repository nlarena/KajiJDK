package java.rmi.server;

/**
 * La exportacion fallo porque no se permitio abrir el puerto.
 *
 * <p>Un caso particular de {@link ExportException}, separado para poder distinguir "no se pudo" de
 * "no se dejo": lo primero se arregla reintentando o cambiando de puerto, lo segundo no.
 */
public class SocketSecurityException extends ExportException {

    private static final long serialVersionUID = -7622072999407781979L;

    /** Con un mensaje. */
    public SocketSecurityException(String s) {
        super(s);
    }

    /** Con un mensaje y la causa. */
    public SocketSecurityException(String s, Exception ex) {
        super(s, ex);
    }
}
