package java.rmi;

/**
 * KajiLibrary's java.rmi.UnknownHostException -- No se pudo resolver el nombre del servidor.
 *
 * <p>El DNS no supo que hacer con el nombre. Es la version de RMI de
 * {@link java.net.UnknownHostException} --y no la misma clase-- porque tiene que ser una
 * {@link RemoteException} para poder salir por la firma de un metodo remoto.
 */
public class UnknownHostException extends RemoteException {

    private static final long serialVersionUID = -8152710247442114228L;

    /** @param s el mensaje */
    public UnknownHostException(String s) {
        super(s);
    }

    /**
     * @param s el mensaje
     * @param ex la causa
     */
    public UnknownHostException(String s, Exception ex) {
        super(s, ex);
    }
}
