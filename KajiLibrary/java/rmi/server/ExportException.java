package java.rmi.server;

import java.rmi.RemoteException;

/**
 * No se pudo exportar el objeto remoto.
 *
 * <p>Exportar es hacerlo alcanzable desde afuera, y falla por razones locales: el puerto ya esta
 * tomado, no hay descriptores, la fabrica de sockets rechazo. Es una {@link RemoteException} aunque
 * nada remoto haya ocurrido todavia — la jerarquia agrupa por donde aparece el error, no por donde
 * se origina.
 */
public class ExportException extends RemoteException {

    private static final long serialVersionUID = -9155485338494060170L;

    /** Con un mensaje. */
    public ExportException(String s) {
        super(s);
    }

    /** Con un mensaje y la causa. */
    public ExportException(String s, Exception ex) {
        super(s, ex);
    }
}
