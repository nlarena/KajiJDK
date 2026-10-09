package java.rmi.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The default factory: sockets with nothing on top.
 *
 * <p>Package-private. The JDK has it as an internal class of {@code sun.rmi.transport}; here it
 * lives alongside because there is no {@code sun.rmi} at all and hiding it further would add
 * nothing.
 */
final class CommonFactory extends RMISocketFactory {

    public Socket createSocket(String host, int port) throws IOException {
        return new Socket(host, port);
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port);
    }
}
