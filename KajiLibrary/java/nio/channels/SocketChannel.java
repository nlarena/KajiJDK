package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * KajiLibrary's java.nio.channels.SocketChannel — un canal sobre una conexion TCP.
 *
 * <p>Lo que lo separa de un `java.net.Socket` es que se puede poner en modo no bloqueante y meter en
 * un {@link Selector}. De ahi salen las dos rarezas que definen su ciclo de vida:
 *
 * <ul>
 *   <li>{@link #connect} puede volver con `false`. Significa "arranque, todavia no termino", no un
 *       fracaso. Quien lo llame en modo no bloqueante tiene que esperar `OP_CONNECT` y despues
 *       llamar a {@link #finishConnect()};
 *   <li>{@link #shutdownOutput()} no es {@link #close()}. Cierra **una mitad**: el otro extremo ve
 *       fin de datos y puede seguir mandando. Es como se despide un protocolo de pedido y respuesta
 *       sin cortarle la palabra a la respuesta.
 * </ul>
 *
 * <h2>Los `open()`, que antes no estaban</h2>
 *
 * <p>Este archivo decia que la VM no tenia ningun nativo de red y que por eso no habia manera de
 * fabricar un canal. Ya los tiene, y los tres {@code open()} estan, con la implementacion de la casa
 * detras. Tambien {@link #socket()}, que faltaba porque devuelve `java.net.Socket` y esa clase no
 * existia en este arbol.
 *
 * <p>El canal que sale de {@code open()} es el del proveedor **instalado**, si alguien instalo uno
 * por el mecanismo de `spi`; el de la casa solo cuando no hay ninguno. Un proveedor propio reemplaza
 * al nuestro, nunca al reves.
 */
public abstract class SocketChannel extends AbstractSelectableChannel
        implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel {

    protected SocketChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * Lectura, escritura y conexion; nunca aceptacion.
     *
     * <p>Es `final` y no abstracto porque el juego no depende del estado: aunque el canal todavia no
     * este conectado, las operaciones **validas** para su tipo son siempre estas tres.
     */
    public final int validOps() {
        return SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT;
    }

    /** Ata el canal a una direccion local. */
    public abstract SocketChannel bind(SocketAddress local) throws IOException;

    /** Fija una opcion de socket. */
    public abstract <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException;

    /**
     * Cierra la mitad de lectura: lo que llegue despues se descarta y `read` devuelve -1.
     */
    public abstract SocketChannel shutdownInput() throws IOException;

    /** Cierra la mitad de escritura: el otro extremo ve fin de datos. */
    public abstract SocketChannel shutdownOutput() throws IOException;

    /** Si la conexion esta hecha. */
    public abstract boolean isConnected();

    /** Si hay una conexion empezada y sin terminar. */
    public abstract boolean isConnectionPending();

    /**
     * Conecta a `remote`.
     *
     * @return `true` si quedo conectado; `false` si arranco y hay que terminar con
     *         {@link #finishConnect()}. Lo segundo solo pasa en modo no bloqueante
     */
    public abstract boolean connect(SocketAddress remote) throws IOException;

    /**
     * Termina una conexion empezada.
     *
     * <p>Hay que llamarlo aunque el selector diga que esta listo: es donde aparece el error si la
     * conexion fallo. Sin este paso, un rechazo del otro extremo se veria como un canal conectado.
     */
    public abstract boolean finishConnect() throws IOException;

    /** La direccion del otro extremo, o `null` si no esta conectado. */
    public abstract SocketAddress getRemoteAddress() throws IOException;

    public abstract int read(ByteBuffer dst) throws IOException;

    public abstract long read(ByteBuffer[] dsts, int offset, int length) throws IOException;

    public final long read(ByteBuffer[] dsts) throws IOException {
        return this.read(dsts, 0, dsts.length);
    }

    public abstract int write(ByteBuffer src) throws IOException;

    public abstract long write(ByteBuffer[] srcs, int offset, int length) throws IOException;

    public final long write(ByteBuffer[] srcs) throws IOException {
        return this.write(srcs, 0, srcs.length);
    }

    /** La direccion local, o `null` si no esta atado. */
    public abstract SocketAddress getLocalAddress() throws IOException;

    /**
     * Un canal sin conectar.
     *
     * <p>El canal nace en modo bloqueante, como manda el contrato: quien quiera el otro modo llama a
     * {@link #configureBlocking}.
     *
     * @throws IOException si no se pudo abrir
     */
    public static SocketChannel open() throws IOException {
        return KajiSelectorProvider.actual().openSocketChannel();
    }

    /**
     * Un canal sin conectar de esa familia de protocolos.
     *
     * @throws UnsupportedOperationException si el proveedor no sostiene esa familia
     * @throws IOException si no se pudo abrir
     */
    public static SocketChannel open(java.net.ProtocolFamily family) throws IOException {
        return KajiSelectorProvider.actual().openSocketChannel(family);
    }

    /**
     * Un canal **ya conectado** a esa direccion.
     *
     * <p>Es la conveniencia que el JDK documenta: abrir, conectar en modo bloqueante, y devolver. Si
     * la conexion falla el canal se cierra, para no dejar un descriptor colgado de una llamada que
     * tiro.
     *
     * @throws IOException si no se pudo abrir o no se pudo conectar
     */
    public static SocketChannel open(SocketAddress remote) throws IOException {
        SocketChannel c = SocketChannel.open();
        try {
            c.connect(remote);
        } catch (RuntimeException e) {
            c.close();
            throw e;
        } catch (IOException e) {
            c.close();
            throw e;
        }
        return c;
    }

    /**
     * El socket que envuelve a este canal.
     *
     * <p>Comparte el descriptor: cerrar cualquiera de los dos cierra el mismo socket, que es lo que
     * promete el contrato para el par canal/socket.
     */
    public abstract java.net.Socket socket();
}
