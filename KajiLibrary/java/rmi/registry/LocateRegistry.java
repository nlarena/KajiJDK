package java.rmi.registry;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

/**
 * It gets the stub of a remote registry, or creates a local one.
 *
 * <p>The two halves of the class have nothing in common even though they share a name:
 *
 * <ul>
 *   <li>{@code getRegistry(...)} **talks to nobody**. It builds a stub out of the host and the
 *       port, and that is it: the registry may not exist, and nobody finds out until the first
 *       real call. That is why none of its forms fails with "it is not there".
 *   <li>{@code createRegistry(...)} does have an effect: it exports a registry **in this VM** and
 *       leaves it listening. The object stays referenced for as long as the VM lives.
 * </ul>
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library has no RMI transport --no JRMP, no stubs, no serialisation with location
 * annotation-- so there is no honest way to return a {@link Registry}: any object we returned would
 * be a stub that talks to nothing, and the program would only discover it at the first `lookup`,
 * far from here.
 *
 * <p>The seven methods therefore throw the exception they already declare, with the reason inside:
 * {@link ConnectException} for the `getRegistry` ones --which in the JDK is what comes out when the
 * other side cannot be reached-- and {@link ExportException} for the `createRegistry` ones, which
 * is what comes out when the export cannot be done. Both are {@link RemoteException}, which is
 * what is declared.
 *
 * @see java.rmi.Naming
 */
public final class LocateRegistry {

    /** It is not instantiated. */
    private LocateRegistry() {
    }

    /**
     * The stub of the local host's registry, on the usual port.
     *
     * @throws RemoteException always in this library; see the class note
     */
    public static Registry getRegistry() throws RemoteException {
        return getRegistry(null, Registry.REGISTRY_PORT, null);
    }

    /**
     * The stub of the local host's registry, on that port.
     *
     * @throws RemoteException always in this library; see the class note
     */
    public static Registry getRegistry(int port) throws RemoteException {
        return getRegistry(null, port, null);
    }

    /**
     * The stub of that host's registry, on the usual port.
     *
     * @throws RemoteException always in this library; see the class note
     */
    public static Registry getRegistry(String host) throws RemoteException {
        return getRegistry(host, Registry.REGISTRY_PORT, null);
    }

    /**
     * The stub of the registry at that host and port.
     *
     * @throws RemoteException always in this library; see the class note
     */
    public static Registry getRegistry(String host, int port) throws RemoteException {
        return getRegistry(host, port, null);
    }

    /**
     * The stub of the registry at that host and port, talking through the sockets the factory
     * gives.
     *
     * @param host the host, or `null` for the local one
     * @param port the port, or 0 for {@link Registry#REGISTRY_PORT}
     * @param csf the client socket factory, or `null` for the usual one
     * @throws RemoteException always in this library; see the class note
     */
    public static Registry getRegistry(String host, int port, RMIClientSocketFactory csf)
            throws RemoteException {
        String where = (host == null ? "localhost" : host)
                + ":" + (port <= 0 ? Registry.REGISTRY_PORT : port);
        throw new ConnectException("Connection refused to host: " + where
                + "; no RMI transport in this library");
    }

    /**
     * It exports a registry in this VM, listening on that port.
     *
     * @throws RemoteException always in this library; see the class note
     */
    public static Registry createRegistry(int port) throws RemoteException {
        return createRegistry(port, null, null);
    }

    /**
     * It exports a registry in this VM that talks through the sockets those factories give.
     *
     * @param port the port it listens on
     * @param csf the client socket factory, or `null` for the usual one
     * @param ssf the server socket factory, or `null` for the usual one
     * @throws RemoteException always in this library; see the class note
     */
    public static Registry createRegistry(int port, RMIClientSocketFactory csf,
            RMIServerSocketFactory ssf) throws RemoteException {
        throw new ExportException("cannot export a registry on port "
                + (port <= 0 ? Registry.REGISTRY_PORT : port)
                + "; no RMI transport in this library");
    }
}
