package java.rmi.server;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * How the server listens on behalf of a remote object.
 *
 * <p>The counterpart of {@link RMIClientSocketFactory}, with one important difference: this one
 * does <strong>not</strong> travel. It stays on the server side, which is where it makes sense —
 * and where the private keys live if the connection is encrypted.
 */
public interface RMIServerSocketFactory {

    /** It opens a listening socket; port {@code 0} lets the system choose. */
    ServerSocket createServerSocket(int port) throws IOException;
}
