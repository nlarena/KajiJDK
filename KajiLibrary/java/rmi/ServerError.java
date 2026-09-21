package java.rmi;

/**
 * KajiLibrary's java.rmi.ServerError -- the server threw an {@link Error}.
 *
 * <p>A server-side {@code Error} cannot be propagated as it is: over there it would mean the
 * <b>client's</b> virtual machine is broken, and it is not. So it is wrapped in a
 * {@link RemoteException}, which is what the client already has to catch.
 *
 * <p>It is the same reasoning as {@code javax.management.remote.JMXServerErrorException}, and not
 * by chance: both settle the problem of carrying a machine error across a network.
 *
 * <p>The name misleads: it is <b>not</b> an {@code Error}, it is a {@code RemoteException} carrying
 * one inside.
 */
public class ServerError extends RemoteException {

    private static final long serialVersionUID = 8455284893909696482L;

    /**
     * @param s the message
     * @param err the server's error
     */
    public ServerError(String s, Error err) {
        super(s, err);
    }
}
