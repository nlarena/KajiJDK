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
 * Las opciones de socket, alcanzadas desde afuera del socket.
 *
 * <h2>Por que existio esta clase</h2>
 *
 * <p>Hasta Java 9 un {@link Socket} no tenia {@code setOption}: las opciones se manejaban con un
 * getter y un setter por cada una —{@code setTcpNoDelay}, {@code setSoTimeout}— y agregar una nueva
 * significaba agregar dos metodos a una clase publica. Esta clase fue la salida: un lugar fuera de
 * {@code java.net} donde poner opciones sin tocar el socket.
 *
 * <p>Java 9 le dio {@code setOption} y {@code getOption} a los tres sockets, asi que la necesidad
 * desaparecio. Estos metodos quedaron y hoy son literalmente delegacion — que es exactamente lo que
 * hacen mas abajo.
 *
 * <p>Lo unico que todavia no tiene equivalente directo es {@link #supportedOptions(Class)}, que
 * pregunta por el <strong>tipo</strong> y no por una instancia: sirve para saber que se puede pedir
 * antes de tener un socket abierto.
 *
 * @deprecated desde Java 9 hay que usar los {@code setOption}/{@code getOption} de
 *     {@link Socket}, {@link ServerSocket} y {@link DatagramSocket}.
 */
@Deprecated(since = "16")
public class Sockets {

    private Sockets() {
    }

    /** Fija una opcion. Delega en {@link Socket#setOption}. */
    public static <T> void setOption(Socket s, SocketOption<T> name, T value) throws IOException {
        s.setOption(name, value);
    }

    /** El valor de una opcion. Delega en {@link Socket#getOption}. */
    public static <T> T getOption(Socket s, SocketOption<T> name) throws IOException {
        return s.getOption(name);
    }

    /** Fija una opcion en un socket servidor. */
    public static <T> void setOption(ServerSocket s, SocketOption<T> name, T value)
            throws IOException {
        s.setOption(name, value);
    }

    /** El valor de una opcion de un socket servidor. */
    public static <T> T getOption(ServerSocket s, SocketOption<T> name) throws IOException {
        return s.getOption(name);
    }

    /** Fija una opcion en un socket de datagramas. */
    public static <T> void setOption(DatagramSocket s, SocketOption<T> name, T value)
            throws IOException {
        s.setOption(name, value);
    }

    /** El valor de una opcion de un socket de datagramas. */
    public static <T> T getOption(DatagramSocket s, SocketOption<T> name) throws IOException {
        return s.getOption(name);
    }

    /**
     * Que opciones admite un tipo de socket, sin necesidad de tener uno.
     *
     * <p>Los conjuntos son los mismos que devuelve el {@code supportedOptions()} de instancia de
     * cada clase, y tienen que serlo: dos respuestas distintas a la misma pregunta segun por donde
     * se entre serian un bug esperando. Estan escritos aca porque preguntar por el tipo no da
     * ninguna instancia a la que delegarle.
     *
     * @throws IllegalArgumentException si {@code socketType} no es uno de los tres tipos de socket
     */
    public static Set<SocketOption<?>> supportedOptions(Class<?> socketType) {
        if (socketType == Socket.class) {
            return conjunto(StandardSocketOptions.SO_SNDBUF, StandardSocketOptions.SO_RCVBUF,
                    StandardSocketOptions.SO_KEEPALIVE, StandardSocketOptions.SO_REUSEADDR,
                    StandardSocketOptions.SO_LINGER, StandardSocketOptions.TCP_NODELAY,
                    StandardSocketOptions.IP_TOS);
        }
        if (socketType == ServerSocket.class) {
            return conjunto(StandardSocketOptions.SO_RCVBUF, StandardSocketOptions.SO_REUSEADDR,
                    StandardSocketOptions.IP_TOS);
        }
        if (socketType == DatagramSocket.class) {
            return conjunto(StandardSocketOptions.SO_SNDBUF, StandardSocketOptions.SO_RCVBUF,
                    StandardSocketOptions.SO_REUSEADDR, StandardSocketOptions.SO_BROADCAST,
                    StandardSocketOptions.IP_TOS);
        }
        throw new IllegalArgumentException("no es un tipo de socket: " + String.valueOf(socketType));
    }

    private static Set<SocketOption<?>> conjunto(SocketOption<?>... opciones) {
        Set<SocketOption<?>> s = new HashSet<SocketOption<?>>();
        for (int i = 0; i < opciones.length; i++) {
            s.add(opciones[i]);
        }
        return Collections.unmodifiableSet(s);
    }
}
