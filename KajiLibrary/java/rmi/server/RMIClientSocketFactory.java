package java.rmi.server;

import java.io.IOException;
import java.net.Socket;

/**
 * How the client opens the connection to a remote object.
 *
 * <h2>Why this travels with the object</h2>
 *
 * <p>This is the surprising part of RMI: when a remote object is exported with its own factory,
 * <strong>the factory is serialised along with the stub</strong> and reaches the client. It runs
 * there, and it is what decides how the socket is opened.
 *
 * <p>That is what lets an object demand TLS without the client configuring anything — see
 * {@code javax.rmi.ssl.SslRMIClientSocketFactory}. It is also why it has to implement
 * {@code equals} and {@code hashCode}: RMI uses them to reuse connections, and two equivalent
 * factories that do not declare themselves equal open a socket each.
 */
public interface RMIClientSocketFactory {

    /** It opens a connection to the server. */
    Socket createSocket(String host, int port) throws IOException;
}
