package jdk.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The socket options, reached from outside the socket.
 *
 * <h2>Why this class existed</h2>
 *
 * <p>Until Java 9 a {@link Socket} had no {@code setOption}: the options were handled with a getter
 * and a setter for each one —{@code setTcpNoDelay}, {@code setSoTimeout}— and adding a new one meant
 * adding two methods to a public class. This class was the way out: a place outside {@code java.net}
 * where options could be put without touching the socket.
 *
 * <p>Java 9 gave {@code setOption} and {@code getOption} to the three sockets, so the need
 * disappeared. These methods stayed and today they are literally delegation — which is exactly what
 * they do further down.
 *
 * <p>The only thing that still has no direct equivalent is {@link #supportedOptions(Class)}, which
 * asks about the <strong>type</strong> and not about an instance: it serves for knowing what can be
 * asked for before having an open socket.
 *
 * @deprecated since Java 9 the {@code setOption}/{@code getOption} of {@link Socket},
 *     {@link ServerSocket} and {@link DatagramSocket} should be used.
 */
@Deprecated(since = "16")
public class Sockets {

    private Sockets() {
    }

    /** It sets an option. It delegates to {@link Socket#setOption}. */
    public static <T> void setOption(Socket s, SocketOption<T> name, T value) throws IOException {
        s.setOption(name, value);
    }

    /** The value of an option. It delegates to {@link Socket#getOption}. */
    public static <T> T getOption(Socket s, SocketOption<T> name) throws IOException {
        return s.getOption(name);
    }

    /** It sets an option on a server socket. */
    public static <T> void setOption(ServerSocket s, SocketOption<T> name, T value)
            throws IOException {
        s.setOption(name, value);
    }

    /** The value of an option of a server socket. */
    public static <T> T getOption(ServerSocket s, SocketOption<T> name) throws IOException {
        return s.getOption(name);
    }

    /** It sets an option on a datagram socket. */
    public static <T> void setOption(DatagramSocket s, SocketOption<T> name, T value)
            throws IOException {
        s.setOption(name, value);
    }

    /** The value of an option of a datagram socket. */
    public static <T> T getOption(DatagramSocket s, SocketOption<T> name) throws IOException {
        return s.getOption(name);
    }

    /**
     * Which options a type of socket admits, without needing to have one.
     *
     * <p>The sets are the same ones the instance `supportedOptions()` of each class returns, and they
     * have to be: two different answers to the same question depending on which way one comes in would
     * be a bug waiting. They are written here because asking about the type gives no instance to
     * delegate to.
     *
     * @throws IllegalArgumentException if {@code socketType} is not one of the three types of socket
     */
    public static Set<SocketOption<?>> supportedOptions(Class<?> socketType) {
        if (socketType == Socket.class) {
            return setOf(StandardSocketOptions.SO_SNDBUF, StandardSocketOptions.SO_RCVBUF,
                    StandardSocketOptions.SO_KEEPALIVE, StandardSocketOptions.SO_REUSEADDR,
                    StandardSocketOptions.SO_LINGER, StandardSocketOptions.TCP_NODELAY,
                    StandardSocketOptions.IP_TOS);
        }
        if (socketType == ServerSocket.class) {
            return setOf(StandardSocketOptions.SO_RCVBUF, StandardSocketOptions.SO_REUSEADDR,
                    StandardSocketOptions.IP_TOS);
        }
        if (socketType == DatagramSocket.class) {
            return setOf(StandardSocketOptions.SO_SNDBUF, StandardSocketOptions.SO_RCVBUF,
                    StandardSocketOptions.SO_REUSEADDR, StandardSocketOptions.SO_BROADCAST,
                    StandardSocketOptions.IP_TOS);
        }
        throw new IllegalArgumentException("not a type of socket: " + String.valueOf(socketType));
    }

    private static Set<SocketOption<?>> setOf(SocketOption<?>... options) {
        Set<SocketOption<?>> s = new HashSet<SocketOption<?>>();
        for (int i = 0; i < options.length; i++) {
            s.add(options[i]);
        }
        return Collections.unmodifiableSet(s);
    }
}
