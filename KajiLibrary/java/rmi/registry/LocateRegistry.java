package java.rmi.registry;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

/**
 * Consigue el talon de un registro remoto, o crea uno local.
 *
 * <p>Las dos mitades de la clase no se parecen en nada aunque compartan nombre:
 *
 * <ul>
 *   <li>{@code getRegistry(...)} **no habla con nadie**. Fabrica un talon a partir de la maquina y
 *       el puerto, y punto: el registro puede no existir, y no se va a enterar hasta la primera
 *       llamada de verdad. Por eso ninguna de sus formas falla por "no esta ahi".
 *   <li>{@code createRegistry(...)} si tiene efecto: exporta un registro **en esta VM** y lo deja
 *       escuchando. El objeto queda referenciado mientras la VM viva.
 * </ul>
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no tiene transporte RMI --ni JRMP, ni talones, ni serializacion con anotacion
 * de ubicacion-- asi que no hay forma honesta de devolver un {@link Registry}: cualquier objeto que
 * devolvieramos seria un talon que no habla con nada, y el programa lo descubriria recien en el
 * primer `lookup`, lejos de aca.
 *
 * <p>Los siete metodos lanzan entonces la excepcion que ya declaran, con el motivo adentro:
 * {@link ConnectException} los `getRegistry` --que en el JDK es lo que sale cuando no se llega al
 * otro lado-- y {@link ExportException} los `createRegistry`, que es lo que sale cuando la
 * exportacion no se puede hacer. Las dos son {@link RemoteException}, que es lo declarado.
 *
 * @see java.rmi.Naming
 */
public final class LocateRegistry {

    /** No se instancia. */
    private LocateRegistry() {
    }

    /**
     * El talon del registro de la maquina local, en el puerto de siempre.
     *
     * @throws RemoteException siempre en esta biblioteca; ver la nota de la clase
     */
    public static Registry getRegistry() throws RemoteException {
        return getRegistry(null, Registry.REGISTRY_PORT, null);
    }

    /**
     * El talon del registro de la maquina local, en ese puerto.
     *
     * @throws RemoteException siempre en esta biblioteca; ver la nota de la clase
     */
    public static Registry getRegistry(int port) throws RemoteException {
        return getRegistry(null, port, null);
    }

    /**
     * El talon del registro de esa maquina, en el puerto de siempre.
     *
     * @throws RemoteException siempre en esta biblioteca; ver la nota de la clase
     */
    public static Registry getRegistry(String host) throws RemoteException {
        return getRegistry(host, Registry.REGISTRY_PORT, null);
    }

    /**
     * El talon del registro de esa maquina y ese puerto.
     *
     * @throws RemoteException siempre en esta biblioteca; ver la nota de la clase
     */
    public static Registry getRegistry(String host, int port) throws RemoteException {
        return getRegistry(host, port, null);
    }

    /**
     * El talon del registro de esa maquina y ese puerto, hablando por los sockets que da la
     * fabrica.
     *
     * @param host la maquina, o `null` para la local
     * @param port el puerto, o 0 para {@link Registry#REGISTRY_PORT}
     * @param csf la fabrica de sockets del cliente, o `null` para la de siempre
     * @throws RemoteException siempre en esta biblioteca; ver la nota de la clase
     */
    public static Registry getRegistry(String host, int port, RMIClientSocketFactory csf)
            throws RemoteException {
        String donde = (host == null ? "localhost" : host)
                + ":" + (port <= 0 ? Registry.REGISTRY_PORT : port);
        throw new ConnectException("Connection refused to host: " + donde
                + "; no RMI transport in this library");
    }

    /**
     * Exporta un registro en esta VM, escuchando en ese puerto.
     *
     * @throws RemoteException siempre en esta biblioteca; ver la nota de la clase
     */
    public static Registry createRegistry(int port) throws RemoteException {
        return createRegistry(port, null, null);
    }

    /**
     * Exporta un registro en esta VM que habla por los sockets que dan esas fabricas.
     *
     * @param port el puerto en el que escucha
     * @param csf la fabrica de sockets del cliente, o `null` para la de siempre
     * @param ssf la fabrica de sockets del servidor, o `null` para la de siempre
     * @throws RemoteException siempre en esta biblioteca; ver la nota de la clase
     */
    public static Registry createRegistry(int port, RMIClientSocketFactory csf,
            RMIServerSocketFactory ssf) throws RemoteException {
        throw new ExportException("cannot export a registry on port "
                + (port <= 0 ? Registry.REGISTRY_PORT : port)
                + "; no RMI transport in this library");
    }
}
