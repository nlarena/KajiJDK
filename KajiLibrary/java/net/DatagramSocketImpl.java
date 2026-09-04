package java.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// La mitad de abajo de un `DatagramSocket`: lo que efectivamente habla con la pila de UDP.
//
// La division existe para que `DatagramSocket` sea una fachada estable y la implementacion sea
// reemplazable --por la del sistema operativo, por un tunel, por una simulacion-- sin tocar el
// codigo que usa la API.
//
// ===========================================================================================
// ES ABSTRACTA, Y POR ESO ENTRA COMPLETA
// ===========================================================================================
//
// Diecisiete de sus miembros son `abstract`: `create`, `bind`, `send`, `receive`, `peek`, `join`,
// `leave` y compania. Declarar un abstracto **no promete nada** -- dice "quien implemente esto tiene
// que saber hacerlo", que es exactamente la verdad: KajiJDK no tiene quien lo implemente, y lo dice
// no teniendo ninguna subclase concreta.
//
// Los pocos que no son abstractos son de esta clase y no tocan la red:
//
//  - `connect`/`disconnect`: en el JDK la base **no hace nada**, y no es un descuido -- para UDP,
//    "conectarse" es solo recordar con quien se habla, y una implementacion que no quiera
//    optimizarlo no tiene nada que hacer.
//  - `getLocalPort`/`getFileDescriptor`: leen los dos campos protegidos.
//  - `setOption`/`getOption`/`supportedOptions`: traducen del vocabulario nuevo
//    (`SocketOption<T>`) al viejo (`SocketOptions`, con enteros), que es lo que hace la base del
//    JDK. La traduccion es una tabla, y esta entera.
//
// Nada omitido.
//
// @deprecated El JDK deprecio el mecanismo entero de `DatagramSocketImpl`.
@Deprecated
public abstract class DatagramSocketImpl implements SocketOptions {

    /** El puerto local al que quedo atado el socket. */
    protected int localPort;

    /** El descriptor del sistema operativo, o null si todavia no hay socket. */
    protected FileDescriptor fd;

    public DatagramSocketImpl() {
    }

    /** Crea el socket en el sistema, todavia sin atarlo a un puerto. */
    protected abstract void create() throws SocketException;

    /** Ata el socket a {@code laddr}:{@code lport}. */
    protected abstract void bind(int lport, InetAddress laddr) throws SocketException;

    /** Manda el datagrama. */
    protected abstract void send(DatagramPacket p) throws IOException;

    /**
     * Recuerda con quien habla este socket.
     *
     * <p>La base no hace nada, y es lo correcto: para UDP no hay handshake, y una implementacion
     * que no filtre por origen no necesita enterarse. La que si quiera aprovecharlo --para que el
     * sistema descarte los datagramas de otro origen-- lo sobreescribe.
     */
    protected void connect(InetAddress address, int port) throws SocketException {
    }

    /** Deja de estar "conectado". La base no hace nada, por la misma razon que {@link #connect}. */
    protected void disconnect() {
    }

    /**
     * Espia el origen del proximo datagrama sin sacarlo de la cola.
     *
     * @return el puerto del que viene
     */
    protected abstract int peek(InetAddress i) throws IOException;

    /** Como {@link #peek}, pero ademas copia los datos en {@code p} sin consumirlo. */
    protected abstract int peekData(DatagramPacket p) throws IOException;

    /** Saca el proximo datagrama y lo deja en {@code p}. */
    protected abstract void receive(DatagramPacket p) throws IOException;

    /**
     * El TTL de los datagramas multicast.
     *
     * @deprecated el TTL es un entero sin signo de 8 bits y este metodo usa `byte`, que tiene
     *     signo; usar {@link #setTimeToLive(int)}.
     */
    @Deprecated
    protected abstract void setTTL(byte ttl) throws IOException;

    /**
     * El TTL de los datagramas multicast.
     *
     * @deprecated ver {@link #setTTL(byte)}.
     */
    @Deprecated
    protected abstract byte getTTL() throws IOException;

    /** Cuantos saltos viven los datagramas multicast que salgan por aca. */
    protected abstract void setTimeToLive(int ttl) throws IOException;

    /** El TTL de los datagramas multicast. */
    protected abstract int getTimeToLive() throws IOException;

    /**
     * Entra al grupo multicast {@code inetaddr}.
     *
     * @deprecated no deja elegir la placa; usar {@link #joinGroup(SocketAddress, NetworkInterface)}.
     */
    @Deprecated
    protected abstract void join(InetAddress inetaddr) throws IOException;

    /**
     * Sale del grupo multicast.
     *
     * @deprecated ver {@link #join}.
     */
    @Deprecated
    protected abstract void leave(InetAddress inetaddr) throws IOException;

    /** Entra al grupo {@code mcastaddr} por la placa {@code netIf}. */
    protected abstract void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf)
            throws IOException;

    /** Sale del grupo {@code mcastaddr} en la placa {@code netIf}. */
    protected abstract void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf)
            throws IOException;

    /** Cierra el socket. */
    protected abstract void close();

    /** El puerto local. */
    protected int getLocalPort() {
        return this.localPort;
    }

    /** El descriptor del sistema, o null. */
    protected FileDescriptor getFileDescriptor() {
        return this.fd;
    }

    /**
     * Fija una opcion, traduciendo del vocabulario tipado al de enteros de {@link SocketOptions}.
     *
     * <p>La traduccion es lo unico que aporta la base; el trabajo real lo hace el `setOption(int,
     * Object)` que escribe la subclase. Se hace asi --y no al reves-- porque `SocketOptions` es la
     * interfaz vieja y es la que las implementaciones existentes ya tienen escrita.
     *
     * @throws UnsupportedOperationException si la opcion no tiene equivalente
     */
    protected <T> void setOption(SocketOption<T> name, T value) throws IOException {
        int id = idDeLaOpcion(name);
        this.setOption(id, value);
    }

    /**
     * El valor de una opcion, con la misma traduccion que {@link #setOption(SocketOption, Object)}.
     *
     * @throws UnsupportedOperationException si la opcion no tiene equivalente
     */
    protected <T> T getOption(SocketOption<T> name) throws IOException {
        int id = idDeLaOpcion(name);
        return (T) this.getOption(id);
    }

    // La tabla de traduccion. Se compara por identidad porque las constantes de
    // `StandardSocketOptions` son unicas, que es justamente para lo que se hicieron constantes.
    private static int idDeLaOpcion(SocketOption<?> name) {
        if (name == null) {
            throw new NullPointerException();
        }
        if (name == StandardSocketOptions.SO_SNDBUF) {
            return SocketOptions.SO_SNDBUF;
        }
        if (name == StandardSocketOptions.SO_RCVBUF) {
            return SocketOptions.SO_RCVBUF;
        }
        if (name == StandardSocketOptions.SO_REUSEADDR) {
            return SocketOptions.SO_REUSEADDR;
        }
        if (name == StandardSocketOptions.SO_REUSEPORT) {
            return SocketOptions.SO_REUSEPORT;
        }
        if (name == StandardSocketOptions.SO_BROADCAST) {
            return SocketOptions.SO_BROADCAST;
        }
        if (name == StandardSocketOptions.IP_TOS) {
            return SocketOptions.IP_TOS;
        }
        if (name == StandardSocketOptions.IP_MULTICAST_IF) {
            return SocketOptions.IP_MULTICAST_IF2;
        }
        if (name == StandardSocketOptions.IP_MULTICAST_TTL) {
            return SocketOptions.IP_MULTICAST_IF2 + 1;
        }
        if (name == StandardSocketOptions.IP_MULTICAST_LOOP) {
            return SocketOptions.IP_MULTICAST_LOOP;
        }
        throw new UnsupportedOperationException("unsupported option: " + name);
    }

    /** Las opciones que esta implementacion entiende. */
    protected Set<SocketOption<?>> supportedOptions() {
        Set<SocketOption<?>> s = new HashSet<SocketOption<?>>();
        s.add(StandardSocketOptions.SO_SNDBUF);
        s.add(StandardSocketOptions.SO_RCVBUF);
        s.add(StandardSocketOptions.SO_REUSEADDR);
        s.add(StandardSocketOptions.IP_TOS);
        return Collections.unmodifiableSet(s);
    }
}
