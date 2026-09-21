package java.rmi.server;

import java.rmi.Remote;

/**
 * The server side of a generated stub: it receives the call and dispatches it to the object.
 *
 * @deprecated replaced by dynamic proxies.
 */
@Deprecated(since = "1.1")
public interface Skeleton {

    /** It dispatches a call to the object. */
    void dispatch(Remote obj, RemoteCall theCall, int opnum, long hash) throws Exception;

    /** The operations this skeleton knows. */
    Operation[] getOperations();
}
