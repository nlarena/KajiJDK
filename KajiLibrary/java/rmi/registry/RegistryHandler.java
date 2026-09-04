package java.rmi.registry;

import java.rmi.RemoteException;
import java.rmi.UnknownHostException;

/**
 * El enganche viejo con el que {@link LocateRegistry} conseguia sus talones.
 *
 * @deprecated No lo usa nadie desde JDK 1.2, y no hay sustituto. Era el punto de extension para
 *     que una implementacion de RMI aportara su propio talon de registro; hoy {@link LocateRegistry}
 *     lo resuelve por dentro y no consulta ningun `RegistryHandler`. Queda declarado porque el tipo
 *     sigue en la API y algo compilado contra el todavia lo nombra.
 */
@Deprecated
public interface RegistryHandler {

    /**
     * El talon con el que hablarle al registro de esa maquina y puerto.
     *
     * @deprecated No lo usa nadie desde JDK 1.2; su sustituto es
     *     {@link LocateRegistry#getRegistry(String, int)}.
     * @throws RemoteException si falla la construccion del talon
     * @throws UnknownHostException si no se resuelve la maquina
     */
    @Deprecated
    Registry registryStub(String host, int port) throws RemoteException, UnknownHostException;

    /**
     * Construye y exporta un registro en ese puerto.
     *
     * @deprecated No lo usa nadie desde JDK 1.2; su sustituto es
     *     {@link LocateRegistry#createRegistry(int)}.
     * @throws RemoteException si falla la exportacion
     */
    @Deprecated
    Registry registryImpl(int port) throws RemoteException;
}
