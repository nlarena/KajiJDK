package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * KajiLibrary's java.nio.channels.AsynchronousSocketChannel — una conexion TCP asincronica.
 *
 * <p>Frente a {@link SocketChannel} cambia quien espera: alli el programa pregunta al selector si
 * hay algo; aca la operacion avisa cuando termino. No hay modo bloqueante que configurar ni
 * {@link Selector} donde registrarse, y por eso esta clase **no** es un
 * {@link SelectableChannel}.
 *
 * <p>La regla que mas cuesta: <strong>una lectura y una escritura pendientes por canal</strong>.
 * Pedir una segunda lectura sin que la primera haya terminado es {@link ReadPendingException}. Es
 * severo a proposito: con dos lecturas en vuelo sobre un mismo flujo de bytes, el orden en que se
 * completan decide donde caen los bytes, y eso no lo controla nadie.
 *
 * <p>Los tiempos limite van por operacion y no por canal, que es lo que permite lo que uno de verdad
 * quiere: esperar dos segundos por la cabecera y treinta por el cuerpo. Al agotarse, la operacion
 * falla con {@link InterruptedByTimeoutException} y --importante-- <strong>el canal queda
 * inservible</strong>: no se puede saber cuantos bytes alcanzo a mover, asi que seguir usandolo
 * seria seguir sobre un flujo desalineado.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Los dos {@code open()} <strong>no estan</strong>: esta VM no tiene nativos de red, y una firma
 * que promete un canal no puede cumplirse con una excepcion. Ver {@link SocketChannel}, donde esta
 * el razonamiento completo. Todo el resto de la clase esta, como contrato.
 */
public abstract class AsynchronousSocketChannel implements AsynchronousByteChannel, NetworkChannel {

    private final AsynchronousChannelProvider proveedor;

    protected AsynchronousSocketChannel(AsynchronousChannelProvider provider) {
        this.proveedor = provider;
    }

    /** El proveedor que lo fabrico. */
    public final AsynchronousChannelProvider provider() {
        return this.proveedor;
    }

    /** Ata el canal a una direccion local. */
    public abstract AsynchronousSocketChannel bind(SocketAddress local) throws IOException;

    /** Fija una opcion de socket. */
    public abstract <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value)
            throws IOException;

    /** Cierra la mitad de lectura. */
    public abstract AsynchronousSocketChannel shutdownInput() throws IOException;

    /** Cierra la mitad de escritura; el otro extremo ve fin de datos. */
    public abstract AsynchronousSocketChannel shutdownOutput() throws IOException;

    /** La direccion del otro extremo, o `null` si no esta conectado. */
    public abstract SocketAddress getRemoteAddress() throws IOException;

    /** Conecta a `remote` y avisa a `handler`. */
    public abstract <A> void connect(SocketAddress remote, A attachment,
            CompletionHandler<Void, ? super A> handler);

    /** Como el otro, devolviendo un {@link Future}. */
    public abstract Future<Void> connect(SocketAddress remote);

    /**
     * Lee con tiempo limite.
     *
     * @param timeout `0` o menos significa sin limite. Al agotarse, el canal queda inservible; ver
     *        la nota de la clase
     */
    public abstract <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment,
            CompletionHandler<Integer, ? super A> handler);

    /** Como el otro, sin limite de tiempo. */
    public final <A> void read(ByteBuffer dst, A attachment,
            CompletionHandler<Integer, ? super A> handler) {
        this.read(dst, 0L, TimeUnit.MILLISECONDS, attachment, handler);
    }

    /** Lee devolviendo un {@link Future}, sin limite de tiempo. */
    public abstract Future<Integer> read(ByteBuffer dst);

    /**
     * Lee repartiendo en varios buffers.
     *
     * <p>Devuelve `Long` y no `Integer` porque el total puede pasar los dos gigas: son varios
     * buffers, no uno.
     */
    public abstract <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout,
            TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler);

    /** Escribe con tiempo limite. Mismas salvedades que la lectura. */
    public abstract <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment,
            CompletionHandler<Integer, ? super A> handler);

    /** Como el otro, sin limite de tiempo. */
    public final <A> void write(ByteBuffer src, A attachment,
            CompletionHandler<Integer, ? super A> handler) {
        this.write(src, 0L, TimeUnit.MILLISECONDS, attachment, handler);
    }

    /** Escribe devolviendo un {@link Future}, sin limite de tiempo. */
    public abstract Future<Integer> write(ByteBuffer src);

    /** Escribe juntando varios buffers. */
    public abstract <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout,
            TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler);

    /** La direccion local, o `null` si no esta atado. */
    public abstract SocketAddress getLocalAddress() throws IOException;
}
