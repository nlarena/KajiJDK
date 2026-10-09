package java.rmi.registry;

import java.rmi.RemoteException;
import java.rmi.UnknownHostException;

/**
 * The old hook through which {@link LocateRegistry} got its stubs.
 *
 * @deprecated Nobody has used it since JDK 1.2, and there is no replacement. It was the extension
 *     point for an RMI implementation to supply its own registry stub; {@link LocateRegistry}
 *     consults no `RegistryHandler`. This note used to say that {@link LocateRegistry} nowadays
 *     resolves it internally; in this library it resolves nothing: its seven methods always throw
 *     the `RemoteException` they declare, because this library has no RMI transport (checked in
 *     `LocateRegistry.java`, whose class note and method bodies say so). It stays declared because
 *     the type is still in the API and something compiled against it may still name it.
 */
@Deprecated
public interface RegistryHandler {

    /**
     * The stub with which to talk to the registry on that host and port.
     *
     * @deprecated Nobody has used it since JDK 1.2; its replacement is
     *     {@link LocateRegistry#getRegistry(String, int)}.
     * @throws RemoteException if building the stub fails
     * @throws UnknownHostException if the host does not resolve
     */
    @Deprecated
    Registry registryStub(String host, int port) throws RemoteException, UnknownHostException;

    /**
     * It builds and exports a registry on that port.
     *
     * @deprecated Nobody has used it since JDK 1.2; its replacement is
     *     {@link LocateRegistry#createRegistry(int)}.
     * @throws RemoteException if the export fails
     */
    @Deprecated
    Registry registryImpl(int port) throws RemoteException;
}
