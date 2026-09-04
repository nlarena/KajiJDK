package java.rmi.server;

import java.rmi.RemoteException;
/**
 * El stub y el skeleton no son de la misma version.

 * <p>Los stubs y skeletons generados por <code>rmic</code> quedaron obsoletos con los proxies
 * dinamicos, y esta excepcion con ellos.
 *
 * @deprecated no la tira nada desde que los skeletons dejaron de usarse.
 */
@Deprecated(since = "1.1")
public class SkeletonMismatchException extends RemoteException {

    private static final long serialVersionUID = -7780460454818859281L;

    /** Con un mensaje. */
    public SkeletonMismatchException(String s) {
        super(s);
    }
}
