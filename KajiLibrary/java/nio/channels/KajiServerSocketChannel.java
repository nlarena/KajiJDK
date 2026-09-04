package java.nio.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.Set;

/**
 * El {@link ServerSocketChannel} de esta biblioteca, sobre la costura TCP de la VM.
 *
 * <h2>El `accept` que devuelve null</h2>
 *
 * <p>Es la diferencia entera con un {@link java.net.ServerSocket}, y es la que encaja mas limpio con
 * esta VM: en modo no bloqueante {@link #accept()} devuelve `null` cuando todavia no hay nadie
 * esperando. El nativo de la VM ya contesta eso --un -3, "todavia no"-- porque no puede quedarse
 * esperando sin colgar el interprete. Lo que en la costura es un codigo de error es aca el contrato
 * palabra por palabra.
 *
 * <p>En modo bloqueante se insiste con un `Thread.sleep` corto entre intentos: dormir suelta el
 * interprete, asi que el hilo que espera una conexion no le impide avanzar al que la va a abrir.
 *
 * <h2>Las opciones</h2>
 *
 * <p>Ninguna de las opciones estandar de un socket a la escucha --`SO_REUSEADDR`, `SO_RCVBUF`-- llega
 * al sistema desde esta VM, asi que {@link #supportedOptions} viene **vacio** y `setOption` tira
 * {@link UnsupportedOperationException}, que es lo que el contrato manda para una opcion que el canal
 * no sostiene. Guardar el valor y devolverlo desde el getter mentiria en lo unico que importa: que la
 * opcion tenga efecto.
 */
final class KajiServerSocketChannel extends ServerSocketChannel {

    /** El socket a la escucha de la VM, o -1 si todavia no se ato. */
    private int handle = -1;

    KajiServerSocketChannel(SelectorProvider provider) {
        super(provider);
    }

    private void exigirAbierto() throws ClosedChannelException {
        if (!this.isOpen()) {
            throw new ClosedChannelException();
        }
    }

    // ---- atar --------------------------------------------------------------------------------

    public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        this.exigirAbierto();
        if (this.handle >= 0) {
            throw new AlreadyBoundException();
        }
        // Sin direccion, el comodin en un puerto que elija el sistema: es lo que el JDK documenta
        // para `bind(null)`.
        String host = "0.0.0.0";
        int puerto = 0;
        if (local != null) {
            if (!(local instanceof InetSocketAddress)) {
                throw new UnsupportedAddressTypeException();
            }
            InetSocketAddress d = (InetSocketAddress) local;
            if (d.isUnresolved()) {
                throw new UnresolvedAddressException();
            }
            if (d.getAddress() != null && !d.getAddress().isAnyLocalAddress()) {
                host = d.getAddress().getHostAddress();
            }
            puerto = d.getPort();
        }
        int h = jdk.internal.net.Net.listen(host, puerto, backlog <= 0 ? 50 : backlog);
        if (h < 0) {
            throw new java.net.BindException("Cannot assign requested address: " + host + ":"
                    + puerto);
        }
        this.handle = h;
        return this;
    }

    public SocketAddress getLocalAddress() throws IOException {
        this.exigirAbierto();
        if (this.handle < 0) {
            return null;
        }
        String d = jdk.internal.net.Net.localAddress(this.handle);
        int p = jdk.internal.net.Net.localPort(this.handle);
        if (d == null || p < 0) {
            return null;
        }
        return new InetSocketAddress(java.net.InetAddress.getByName(d), p);
    }

    public java.net.ServerSocket socket() {
        return (java.net.ServerSocket) jdk.internal.net.Adopcion.servidor(this.handle);
    }

    // ---- aceptar -----------------------------------------------------------------------------

    public SocketChannel accept() throws IOException {
        this.exigirAbierto();
        if (this.handle < 0) {
            throw new NotYetBoundException();
        }
        int h = jdk.internal.net.Net.accept(this.handle);
        while (h == -3 && this.isBlocking()) {
            if (!this.isOpen()) {
                throw new AsynchronousCloseException();
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.InterruptedIOException("accept interrupted");
            }
            h = jdk.internal.net.Net.accept(this.handle);
        }
        if (h == -3) {
            // Sin bloquear y sin nadie esperando: `null`, que es el contrato.
            return null;
        }
        if (h < 0) {
            throw new IOException("accept failed");
        }
        return new KajiSocketChannel(this.provider(), h);
    }

    // ---- opciones ----------------------------------------------------------------------------

    public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        this.exigirAbierto();
        if (name == null) {
            throw new NullPointerException("name");
        }
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    public <T> T getOption(SocketOption<T> name) throws IOException {
        this.exigirAbierto();
        if (name == null) {
            throw new NullPointerException("name");
        }
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    public Set<SocketOption<?>> supportedOptions() {
        return Collections.emptySet();
    }

    // ---- cierre ------------------------------------------------------------------------------

    protected void implCloseSelectableChannel() throws IOException {
        if (this.handle >= 0) {
            jdk.internal.net.Net.close(this.handle);
            this.handle = -1;
        }
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        // Nada que decirle al sistema: el socket de la VM siempre es no bloqueante, y el modo lo
        // decide `accept()` al elegir si insiste. Ver la nota de la clase.
    }
}
