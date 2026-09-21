package javax.management.remote.rmi;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The remote object a client connects to first.
 *
 * <h2>Two methods, and both are needed</h2>
 *
 * <p>{@link #getVersion} is called <strong>before</strong> authenticating, and it is what allows
 * a client and a server of different versions to understand each other or to reject each other
 * with a clear message instead of failing in the middle of the first operation.
 *
 * <p>{@link #newClient} is the one that authenticates and returns the connection. That the
 * credential is an {@code Object} and not something typed is on purpose: it may be an array of
 * two {@code String}s, an object of a mechanism of one's own, or {@code null} if the server asks
 * for nothing.
 *
 * <h2>Why there are two objects and not one</h2>
 *
 * <p>This one is single and shared; the {@link RMIConnection} it returns is <strong>one per
 * client</strong>. That separation is what allows each client to have its own identity, its own
 * class loader and its own notification queue.
 *
 * @since 1.5
 */
public interface RMIServer extends Remote {

    /**
     * The protocol's and the implementation's version.
     *
     * <p>The format is {@code "<specification version> <provider name>"}.
     *
     * @return the version
     * @throws RemoteException if the server could not be talked to
     */
    String getVersion() throws RemoteException;

    /**
     * Authenticates the client and opens a connection of its own for it.
     *
     * @param credentials the credential, or {@code null} if the server asks for none
     * @return this client's connection
     * @throws java.rmi.RemoteException if the server could not be talked to
     * @throws java.lang.SecurityException if the credential does not serve
     * @throws IOException if the connection could not be opened
     */
    RMIConnection newClient(Object credentials) throws IOException;
}
