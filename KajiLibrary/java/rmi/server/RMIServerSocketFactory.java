package java.rmi.server;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Como el servidor escucha por un objeto remoto.
 *
 * <p>La contraparte de {@link RMIClientSocketFactory}, con una diferencia importante: esta
 * <strong>no</strong> viaja. Se queda del lado del servidor, que es donde tiene sentido — y donde
 * viven las claves privadas si la conexion es cifrada.
 */
public interface RMIServerSocketFactory {

    /** Abre un socket de escucha; el puerto {@code 0} deja elegir al sistema. */
    ServerSocket createServerSocket(int port) throws IOException;
}
