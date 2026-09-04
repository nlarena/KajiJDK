package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.util.Set;

/**
 * KajiLibrary's java.nio.channels.NetworkChannel — un canal atado a un socket de red.
 *
 * <p>Lo que agrega sobre un canal comun son las dos cosas que solo un socket tiene: una **direccion
 * local** a la que estar atado, y **opciones** que ajustan como se comporta.
 *
 * <p>Las opciones estan tipadas ({@link SocketOption}) y no son pares de cadenas, y eso es lo que
 * permite que {@link #setOption} verifique en compilacion que el valor corresponde a la opcion.
 * {@link #supportedOptions()} existe porque el juego de opciones depende del sistema: preguntar es la
 * unica forma correcta de saber, en vez de probar y atajar.
 *
 * <p><strong>Esta biblioteca no trae implementaciones.</strong> Las harian `SocketChannel`,
 * `ServerSocketChannel` y `DatagramChannel`, y esta VM no tiene nativos de red. La interfaz esta
 * completa: es una declaracion, y su contrato no depende de que haya quien lo cumpla.
 */
public interface NetworkChannel extends Channel {

    /**
     * Ata el canal a una direccion local.
     *
     * @param local la direccion, o `null` para que el sistema elija
     */
    NetworkChannel bind(SocketAddress local) throws IOException;

    /** La direccion a la que esta atado, o `null` si no lo esta. */
    SocketAddress getLocalAddress() throws IOException;

    /** Fija una opcion. */
    <T> NetworkChannel setOption(SocketOption<T> name, T value) throws IOException;

    /** El valor de una opcion. */
    <T> T getOption(SocketOption<T> name) throws IOException;

    /** Las opciones que este canal admite. */
    Set<SocketOption<?>> supportedOptions();
}
