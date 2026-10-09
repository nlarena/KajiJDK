package java.util.logging;

import java.io.IOException;
import java.net.Socket;

/**
 * KajiLibrary's java.util.logging.SocketHandler -- it sends the log over a TCP connection.
 *
 * <p>It is a {@link StreamHandler} over a socket's output, and almost all of its behaviour comes
 * from there. What is its own is three things, and all three have a reason worth having at hand.
 *
 * <h2>It flushes on EVERY record</h2>
 *
 * <p>{@link #publish} calls `flush()` after each one, which {@link StreamHandler} does not. It is not
 * tidiness: a network handler exists so that somebody on the other side sees what happens **while**
 * it happens. A record that sits in the sender's buffer until it fills is no use to anybody, and if
 * the program crashes --which is when the log matters-- it never arrives.
 *
 * <h2>The target is configurable, and if it is missing that is an error</h2>
 *
 * <p>The no-argument constructor takes `java.util.logging.SocketHandler.host` and `.port` from the
 * configuration. If they are not there, it **throws**: a network handler with no target has no
 * reasonable default behaviour. Writing to `localhost` would be guessing, and staying quiet would be
 * worse -- the log would be lost without anybody finding out, which is exactly what a handler cannot
 * do.
 *
 * <h2>Closing closes the socket</h2>
 *
 * <p>{@link #close} closes the stream through {@link StreamHandler} --which writes the formatter's
 * tail-- and then the socket. In that order: the other way round, the tail would never get out.
 */
public class SocketHandler extends StreamHandler {

    private Socket socket;

    /**
     * A handler to the configuration's host and port.
     *
     * @throws IllegalArgumentException if the configuration does not say where to connect
     * @throws IOException if it could not connect
     */
    public SocketHandler() throws IOException {
        this.configure("java.util.logging.SocketHandler");
        LogManager m = LogManager.getLogManager();
        String host = m.getStringProperty("java.util.logging.SocketHandler.host", null);
        String port = m.getStringProperty("java.util.logging.SocketHandler.port", null);
        int portNum = SocketHandler.portFrom(port);
        if (host == null || host.length() == 0 || portNum <= 0) {
            throw new IllegalArgumentException(
                    "SocketHandler needs java.util.logging.SocketHandler.host and .port");
        }
        this.connect(host, portNum);
    }

    /**
     * A handler to that host and port.
     *
     * @throws IllegalArgumentException if the port is not valid
     * @throws IOException if it could not connect
     */
    public SocketHandler(String host, int port) throws IOException {
        this.configure("java.util.logging.SocketHandler");
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port out of range: " + port);
        }
        this.connect(host, port);
    }

    private static int portFrom(String s) {
        if (s == null) {
            return -1;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            // A port that is not a number is the same as not having set one: the constructor throws
            // with the message that names both properties, which says more than a format error.
            return -1;
        }
    }

    private void connect(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.setOutputStream(this.socket.getOutputStream());
    }

    /**
     * It writes the record and sends it at once. See the class's note on the `flush`.
     */
    public void publish(LogRecord record) {
        super.publish(record);
        this.flush();
    }

    /** It closes the stream and then the socket. See the class's note on the order. */
    public synchronized void close() {
        super.close();
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                // Closing is the last thing this handler does: if the socket was already down, the
                // log either got out or was lost anyway, and throwing here would change neither.
                this.reportError(null, e, ErrorManager.CLOSE_FAILURE);
            }
            this.socket = null;
        }
    }
}
