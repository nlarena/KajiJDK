package java.rmi.server;

import java.rmi.Remote;

/**
 * El lado servidor de un stub generado: recibe la llamada y la despacha al objeto.
 *
 * @deprecated reemplazado por los proxies dinamicos.
 */
@Deprecated(since = "1.1")
public interface Skeleton {

    /** Despacha una llamada al objeto. */
    void dispatch(Remote obj, RemoteCall theCall, int opnum, long hash) throws Exception;

    /** Las operaciones que este skeleton conoce. */
    Operation[] getOperations();
}
