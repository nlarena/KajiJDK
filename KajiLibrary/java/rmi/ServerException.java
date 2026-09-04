package java.rmi;

/**
 * KajiLibrary's java.rmi.ServerException -- El metodo remoto lanzo una excepcion de RMI.
 *
 * <p>Envuelve una {@link RemoteException} que ocurrio <b>en el servidor</b> mientras ejecutaba el
 * metodo. La distincion con las demas es de <b>donde paso</b>: las otras son problemas del transporte,
 * esta dice que el transporte anduvo y el problema fue alla.
 *
 * <p>Que sea una {@code RemoteException} envuelta en otra no es redundante: la de adentro puede ser de
 * una llamada que el servidor hizo a un tercero.
 */
public class ServerException extends RemoteException {

    private static final long serialVersionUID = -4775845313121906682L;

    /** @param s el mensaje */
    public ServerException(String s) {
        super(s);
    }

    /**
     * @param s el mensaje
     * @param ex la causa
     */
    public ServerException(String s, Exception ex) {
        super(s, ex);
    }
}
