package java.rmi.server;

import java.rmi.RemoteException;

/**
 * No se encontro el skeleton de un objeto remoto.
 *
 * @deprecated los skeletons se usaban con los stubs generados por {@code rmic}; los proxies
 *     dinamicos los reemplazaron y nada tira esto.
 */
@Deprecated(since = "1.1")
public class SkeletonNotFoundException extends RemoteException {

    private static final long serialVersionUID = -7860299673822761231L;

    /** Con un mensaje. */
    public SkeletonNotFoundException(String s) {
        super(s);
    }

    /** Con un mensaje y la causa. */
    public SkeletonNotFoundException(String s, Exception ex) {
        super(s, ex);
    }
}
