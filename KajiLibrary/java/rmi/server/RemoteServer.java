package java.rmi.server;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * The base of remote object implementations, on the server side.
 *
 * <p>Separate from {@link RemoteObject} because the two sides inherit different things: a stub
 * needs remote identity, an implementation also needs to know whom it is serving. Hence {@link
 * #getClientHost}, which only has an answer during a call — see {@link ServerNotActiveException}.
 * This note used to leave it at that; in this library there is never a call in progress, because
 * there is no RMI transport (`UnicastRemoteObject.exportObject` throws
 * `UnsupportedOperationException`), so {@link #getClientHost} always throws. Checked in its body
 * below and in `UnicastRemoteObject.java`.
 */
public abstract class RemoteServer extends RemoteObject {

    private static final long serialVersionUID = -4100238210092549637L;

    private static PrintStream log;

    /** Without a reference. */
    protected RemoteServer() {
        super();
    }

    /** With that reference. */
    protected RemoteServer(RemoteRef ref) {
        super(ref);
    }

    /**
     * The host of the client being served.
     *
     * @throws ServerNotActiveException if no call is being served on this thread — always, in
     *     this library, which serves no remote calls
     */
    public static String getClientHost() throws ServerNotActiveException {
        throw new ServerNotActiveException("no remote call in progress");
    }

    /**
     * It turns on the call log; {@code null} turns it off.
     *
     * <p>It is static and global: there is no log per object.
     *
     * <p>This note used to leave it at turning on a log of calls; in this library it only stores
     * the stream, and nothing writes to it, since no remote calls are served (a search of
     * KajiLibrary finds no caller of {@link #getLog} and no other use of the field).
     */
    public static void setLog(OutputStream out) {
        log = out == null ? null : new PrintStream(out, true);
    }

    /** Where the log goes, or {@code null} if it is off. */
    public static PrintStream getLog() {
        return log;
    }
}
