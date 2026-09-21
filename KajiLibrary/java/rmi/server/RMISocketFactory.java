package java.rmi.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The socket factory RMI uses when an object does not bring its own.
 *
 * <p>It implements both interfaces, and that union makes sense right here: this is the
 * <strong>global</strong> configuration of the process, where the two sides are set together. The
 * per-object factories go separately, because the client's travels and the server's does not — see
 * {@link RMIClientSocketFactory}.
 *
 * <p>{@link #setSocketFactory} can be called <strong>only once</strong>. That is not a whim:
 * changing it with open connections would leave sockets created by one factory and closed by
 * another.
 */
public abstract class RMISocketFactory implements RMIClientSocketFactory, RMIServerSocketFactory {

    private static RMISocketFactory chosen;
    private static RMISocketFactory defaultFactory;
    private static RMIFailureHandler failureHandler;

    /** For the implementations. */
    public RMISocketFactory() {
    }

    /** It opens a connection to the server. */
    public abstract Socket createSocket(String host, int port) throws IOException;

    /** It opens a listening socket. */
    public abstract ServerSocket createServerSocket(int port) throws IOException;

    /**
     * It sets the global factory.
     *
     * @throws IOException if one had already been set
     */
    public static synchronized void setSocketFactory(RMISocketFactory fac) throws IOException {
        if (chosen != null) {
            throw new IOException("the socket factory was already set");
        }
        chosen = fac;
    }

    /** The global factory, or {@code null} if none was set. */
    public static synchronized RMISocketFactory getSocketFactory() {
        return chosen;
    }

    /**
     * The default factory: plain sockets.
     *
     * <p>It is never {@code null}, unlike {@link #getSocketFactory}. The distinction matters: one
     * says what was configured and the other what is used when nothing was configured.
     */
    public static synchronized RMISocketFactory getDefaultSocketFactory() {
        if (defaultFactory == null) {
            defaultFactory = new CommonFactory();
        }
        return defaultFactory;
    }

    /** It sets what to do when a socket cannot be created; see {@link RMIFailureHandler}. */
    public static synchronized void setFailureHandler(RMIFailureHandler fh) {
        failureHandler = fh;
    }

    /** The failure handler, or {@code null}. */
    public static synchronized RMIFailureHandler getFailureHandler() {
        return failureHandler;
    }
}
