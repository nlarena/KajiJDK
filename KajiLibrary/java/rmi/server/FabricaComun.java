package java.rmi.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * La fabrica por omision: sockets sin nada encima.
 *
 * <p>De paquete. El JDK la tiene como una clase interna de {@code sun.rmi.transport}; aca vive al
 * lado porque no hay ningun {@code sun.rmi} y esconderla mas no aportaria nada.
 */
final class FabricaComun extends RMISocketFactory {

    public Socket createSocket(String host, int port) throws IOException {
        return new Socket(host, port);
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port);
    }
}
