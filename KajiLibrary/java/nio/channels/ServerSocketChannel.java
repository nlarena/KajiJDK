package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * KajiLibrary's java.nio.channels.ServerSocketChannel — un canal que escucha conexiones.
 *
 * <p>No lee ni escribe: lo unico que hace es {@link #accept()}, y por eso no implementa
 * {@link ByteChannel}. Un canal de escucha con `read` seria una firma que nunca tiene sentido.
 *
 * <p>{@link #accept()} en modo no bloqueante devuelve `null` cuando no hay nadie esperando. Es la
 * unica forma de decirlo sin excepcion, y es lo que permite el lazo del selector: despertar,
 * aceptar, y si vino `null` seguir de largo.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Los dos {@code open()} estaticos y {@code socket()} <strong>no estan</strong>, por lo mismo que
 * en {@link SocketChannel}: esta VM no tiene nativos de red, y un `open()` que prometiera un canal
 * para entregar una excepcion pasaria la compilacion de quien lo use y fallaria despues.
 * {@code socket()} ademas devuelve `java.net.ServerSocket`, que no existe en el `java.net` de esta
 * biblioteca.
 *
 * <p>El resto de la clase esta: es el contrato que cualquier implementacion de escucha tiene que
 * cumplir, y encaja con el resto de `java.nio.channels` sin cambiar nada.
 */
public abstract class ServerSocketChannel extends AbstractSelectableChannel
        implements NetworkChannel {

    protected ServerSocketChannel(SelectorProvider provider) {
        super(provider);
    }

    /** Solo aceptacion: un canal de escucha nunca esta "listo para leer". */
    public final int validOps() {
        return SelectionKey.OP_ACCEPT;
    }

    /** Ata el canal a `local` con la cola de pendientes que el sistema prefiera. */
    public final ServerSocketChannel bind(SocketAddress local) throws IOException {
        return this.bind(local, 0);
    }

    /**
     * Ata el canal a `local`.
     *
     * @param backlog cuantas conexiones pueden esperar sin aceptar; `0` o menos deja elegir al
     *        sistema. No es un limite de conexiones sino de las que se acumulan **sin atender**
     */
    public abstract ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException;

    /** Fija una opcion de socket. */
    public abstract <T> ServerSocketChannel setOption(SocketOption<T> name, T value)
            throws IOException;

    /**
     * Acepta una conexion.
     *
     * @return el canal de la conexion aceptada, o `null` en modo no bloqueante si no habia ninguna
     */
    public abstract SocketChannel accept() throws IOException;

    /** La direccion a la que esta atado, o `null` si no lo esta. */
    public abstract SocketAddress getLocalAddress() throws IOException;

    /**
     * Un canal de escucha sin atar.
     *
     * <p>Nace en modo bloqueante, como manda el contrato.
     *
     * @throws IOException si no se pudo abrir
     */
    public static ServerSocketChannel open() throws IOException {
        return KajiSelectorProvider.actual().openServerSocketChannel();
    }

    /**
     * Un canal de escucha sin atar de esa familia de protocolos.
     *
     * @throws UnsupportedOperationException si el proveedor no sostiene esa familia
     * @throws IOException si no se pudo abrir
     */
    public static ServerSocketChannel open(java.net.ProtocolFamily family) throws IOException {
        return KajiSelectorProvider.actual().openServerSocketChannel(family);
    }

    /**
     * El socket que envuelve a este canal.
     *
     * <p>Comparte el descriptor: cerrar cualquiera de los dos cierra el mismo socket.
     */
    public abstract java.net.ServerSocket socket();
}
