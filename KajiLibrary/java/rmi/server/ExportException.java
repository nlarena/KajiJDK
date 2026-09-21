package java.rmi.server;

import java.rmi.RemoteException;

/**
 * The remote object could not be exported.
 *
 * <p>Exporting is making it reachable from outside, and it fails for local reasons: the port is
 * already taken, there are no descriptors, the socket factory refused. It is a
 * {@link RemoteException} even though nothing remote has happened yet — the hierarchy groups by
 * where the error turns up, not by where it originates.
 */
public class ExportException extends RemoteException {

    private static final long serialVersionUID = -9155485338494060170L;

    /** With a message. */
    public ExportException(String s) {
        super(s);
    }

    /** With a message and the cause. */
    public ExportException(String s, Exception ex) {
        super(s, ex);
    }
}
