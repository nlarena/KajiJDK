package java.rmi.server;

import java.rmi.Remote;
import java.rmi.server.RemoteStub;

/**
 * La referencia del lado servidor de un objeto exportado.
 *
 * @deprecated de la epoca de los stubs generados por {@code rmic}.
 */
@Deprecated(since = "1.2")
public interface ServerRef extends RemoteRef {

    static final long serialVersionUID = -4557750989390278438L;

    /** Exporta el objeto y devuelve su stub. */
    RemoteStub exportObject(Remote obj, Object data) throws RemoteException;

    /** El host del cliente que se esta atendiendo. */
    String getClientHost() throws ServerNotActiveException;
}
