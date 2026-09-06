package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import java.nio.channels.spi.AsynchronousChannelProvider;

// Un `AsynchronousServerSocketChannel` sobre el `ServerSocketChannel` bloqueante de esta biblioteca
// y un pool de hilos.
//
// ===============================================================================================
// UNA SOLA ACEPTACION EN VUELO
// ===============================================================================================
//
// Como en el channel de socket, y por la misma clase de razon: abajo hay un `accept` bloqueante, y dos
// hilos llamandolo a la vez se repartirian las conexiones entrantes de forma que ninguno de los dos
// puede predecir. La API ya lo prohibe --`AcceptPendingException`-- asi que no hay nada que inventar.
//
// El channel aceptado se anota en el mismo group: es lo que hace que `shutdown()` del group espere
// tambien por las conexiones que el servidor fue creando, y no solo por el servidor.
//
// De paquete a proposito: se llega por `AsynchronousServerSocketChannel.open`.
final class AsyncServerSocketChannelImpl extends AsynchronousServerSocketChannel {

    private final AsynchronousChannelProvider provider;
    private final ServerSocketChannel channel;
    private final AsyncChannelGroup group;
    private boolean aceptando;

    AsyncServerSocketChannelImpl(AsynchronousChannelProvider provider, ServerSocketChannel channel,
            AsyncChannelGroup group) {
        super(provider);
        this.provider = provider;
        this.channel = channel;
        this.group = group;
        group.register(this);
    }

    @Override
    public AsynchronousServerSocketChannel bind(SocketAddress local, int backlog)
            throws IOException {
        this.channel.bind(local, backlog);
        return this;
    }

    @Override
    public <T> AsynchronousServerSocketChannel setOption(SocketOption<T> name, T value)
            throws IOException {
        this.channel.setOption(name, value);
        return this;
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return this.channel.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return this.channel.supportedOptions();
    }

    @Override
    public <A> void accept(A attachment,
            CompletionHandler<AsynchronousSocketChannel, ? super A> handler) {
        tomar();
        AsyncTask.notifying(this.group.pool(), new Aceptar(this), attachment, handler);
    }

    @Override
    public Future<AsynchronousSocketChannel> accept() {
        tomar();
        return AsyncTask.future(this.group.pool(), new Aceptar(this));
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return this.channel.getLocalAddress();
    }

    @Override
    public boolean isOpen() {
        return this.channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        this.group.unregister(this);
        this.channel.close();
    }

    private synchronized void tomar() {
        if (this.aceptando) {
            throw new AcceptPendingException();
        }
        this.aceptando = true;
    }

    private synchronized void soltar() {
        this.aceptando = false;
    }

    /** La aceptacion, para correr en el pool. */
    private static final class Aceptar implements Callable<AsynchronousSocketChannel> {

        private final AsyncServerSocketChannelImpl duenio;

        Aceptar(AsyncServerSocketChannelImpl duenio) {
            this.duenio = duenio;
        }

        public AsynchronousSocketChannel call() throws IOException {
            try {
                final SocketChannel entrante = this.duenio.channel.accept();
                if (entrante == null) {
                    // El channel de abajo es bloqueante, asi que esto no pasa; si pasara, devolver
                    // null seria peor que decirlo.
                    throw new IOException("accept() no devolvio ningun channel");
                }
                return new AsyncSocketChannelImpl(this.duenio.provider, entrante, this.duenio.group);
            } finally {
                this.duenio.soltar();
            }
        }
    }
}
