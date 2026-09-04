package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * KajiLibrary's java.nio.channels.DatagramChannel — un canal de datagramas (UDP).
 *
 * <p>Tiene dos juegos de operaciones y mezclarlos es el error tipico:
 *
 * <ul>
 *   <li>{@link #send} y {@link #receive} llevan la direccion en cada llamada. Es el uso normal de
 *       UDP: cada paquete va a donde diga;
 *   <li>{@link #read} y {@link #write} no la llevan, y por eso **exigen** haber llamado antes a
 *       {@link #connect}. Un `read` sin conectar es {@link NotYetConnectedException}.
 * </ul>
 *
 * <p>{@link #connect} sobre un canal de datagramas no negocia nada con nadie --UDP no tiene
 * conexion-- sino que **filtra**: fija el otro extremo, y desde ahi el sistema descarta lo que venga
 * de cualquier otro. Sirve por eso y porque, ya fijado, mandar sale mas barato: no hay que resolver
 * la direccion en cada paquete.
 *
 * <p>Un detalle de {@link #receive} que sorprende: si el datagrama no entra en el buffer, **el resto
 * se pierde en silencio**. No hay lectura parcial que continuar, porque un datagrama es todo o nada.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Los dos {@code open()} estaticos y {@code socket()} <strong>no estan</strong>, por lo mismo que
 * en {@link SocketChannel}: no hay nativos de red en esta VM.
 *
 * <p>La clase implementa {@link MulticastChannel}, que aca quedo sin sus dos `join` porque toman un
 * `java.net.NetworkInterface` inexistente; ver la nota de esa interfaz.
 */
public abstract class DatagramChannel extends AbstractSelectableChannel
        implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, MulticastChannel {

    protected DatagramChannel(SelectorProvider provider) {
        super(provider);
    }

    /** Lectura y escritura; un canal de datagramas nunca conecta ni acepta en el sentido del selector. */
    public final int validOps() {
        return SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    }

    /** Ata el canal a una direccion local. `null` deja elegir al sistema. */
    public abstract DatagramChannel bind(SocketAddress local) throws IOException;

    /** Fija una opcion de socket. */
    public abstract <T> DatagramChannel setOption(SocketOption<T> name, T value) throws IOException;

    /** Si esta atado a un extremo remoto. */
    public abstract boolean isConnected();

    /** Fija el otro extremo: filtra lo que llega y abarata lo que sale. Ver la nota de la clase. */
    public abstract DatagramChannel connect(SocketAddress remote) throws IOException;

    /** Deshace {@link #connect}; el canal vuelve a aceptar de cualquiera. */
    public abstract DatagramChannel disconnect() throws IOException;

    /** El extremo fijado, o `null` si no hay. */
    public abstract SocketAddress getRemoteAddress() throws IOException;

    /**
     * Recibe un datagrama.
     *
     * @return de donde vino, o `null` en modo no bloqueante si no habia ninguno
     */
    public abstract SocketAddress receive(ByteBuffer dst) throws IOException;

    /**
     * Manda lo que quede en `src` como un datagrama a `target`.
     *
     * @return los bytes mandados, o `0` en modo no bloqueante si no habia lugar de salida
     */
    public abstract int send(ByteBuffer src, SocketAddress target) throws IOException;

    /** Lee del extremo fijado. Exige {@link #connect} previo. */
    public abstract int read(ByteBuffer dst) throws IOException;

    public abstract long read(ByteBuffer[] dsts, int offset, int length) throws IOException;

    public final long read(ByteBuffer[] dsts) throws IOException {
        return this.read(dsts, 0, dsts.length);
    }

    /** Escribe al extremo fijado. Exige {@link #connect} previo. */
    public abstract int write(ByteBuffer src) throws IOException;

    public abstract long write(ByteBuffer[] srcs, int offset, int length) throws IOException;

    public final long write(ByteBuffer[] srcs) throws IOException {
        return this.write(srcs, 0, srcs.length);
    }

    /** La direccion a la que esta atado, o `null`. */
    public abstract SocketAddress getLocalAddress() throws IOException;

    /**
     * Un canal de datagramas sin atar.
     *
     * <p>Nace en modo bloqueante, como manda el contrato.
     *
     * @throws IOException si no se pudo abrir
     */
    public static DatagramChannel open() throws IOException {
        return KajiSelectorProvider.actual().openDatagramChannel();
    }

    /**
     * Un canal de datagramas sin atar de esa familia de protocolos.
     *
     * @throws UnsupportedOperationException si el proveedor no sostiene esa familia
     * @throws IOException si no se pudo abrir
     */
    public static DatagramChannel open(java.net.ProtocolFamily family) throws IOException {
        return KajiSelectorProvider.actual().openDatagramChannel(family);
    }

    /**
     * El socket que envuelve a este canal.
     *
     * <p>Comparte el descriptor: cerrar cualquiera de los dos cierra el mismo socket.
     */
    public abstract java.net.DatagramSocket socket();
}
