package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.Future;

/**
 * KajiLibrary's java.nio.channels.AsynchronousServerSocketChannel — un canal de escucha asincronico.
 *
 * <p>Solo acepta; no lee ni escribe. {@link #accept()} pide **una** conexion, no abre un flujo de
 * conexiones: para seguir aceptando hay que volver a llamarlo, y lo normal es hacerlo desde el mismo
 * {@link CompletionHandler} que atendio la anterior. Ese re-pedido dentro del handler es el modo
 * idiomatico de usar esta clase, y olvidarlo produce un servidor que atiende exactamente una
 * conexion.
 *
 * <p>Como en {@link AsynchronousSocketChannel}, hay **una sola operacion pendiente** a la vez: un
 * segundo `accept` antes de que el primero termine es {@link AcceptPendingException}.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Los dos {@code open()} <strong>no estan</strong>, por lo mismo que en todos los canales de red:
 * esta VM no tiene nativos de red. El resto de la clase esta, como contrato.
 */
public abstract class AsynchronousServerSocketChannel
        implements AsynchronousChannel, NetworkChannel {

    private final AsynchronousChannelProvider proveedor;

    protected AsynchronousServerSocketChannel(AsynchronousChannelProvider provider) {
        this.proveedor = provider;
    }

    /** El proveedor que lo fabrico. */
    public final AsynchronousChannelProvider provider() {
        return this.proveedor;
    }

    /** Ata el canal a `local` con la cola de pendientes que el sistema prefiera. */
    public final AsynchronousServerSocketChannel bind(SocketAddress local) throws IOException {
        return this.bind(local, 0);
    }

    /**
     * Ata el canal a `local`.
     *
     * @param backlog cuantas conexiones pueden esperar sin aceptar; `0` o menos deja elegir al
     *        sistema
     */
    public abstract AsynchronousServerSocketChannel bind(SocketAddress local, int backlog)
            throws IOException;

    /** Fija una opcion de socket. */
    public abstract <T> AsynchronousServerSocketChannel setOption(SocketOption<T> name, T value)
            throws IOException;

    /** Acepta **una** conexion y avisa a `handler`. Ver la nota de la clase. */
    public abstract <A> void accept(A attachment,
            CompletionHandler<AsynchronousSocketChannel, ? super A> handler);

    /** Como el otro, devolviendo un {@link Future}. */
    public abstract Future<AsynchronousSocketChannel> accept();

    /** La direccion a la que esta atado, o `null` si no lo esta. */
    public abstract SocketAddress getLocalAddress() throws IOException;
}
