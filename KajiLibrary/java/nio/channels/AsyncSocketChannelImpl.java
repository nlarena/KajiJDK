package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.nio.channels.spi.AsynchronousChannelProvider;

// Un `AsynchronousSocketChannel` sobre el `SocketChannel` bloqueante de esta biblioteca y un pool
// de hilos.
//
// ===============================================================================================
// LA REGLA DE UNA SOLA OPERACION EN VUELO
// ===============================================================================================
//
// Una lectura y una escritura pendientes por channel, y ni una mas: pedir una segunda lectura sin que
// la primera termine es `ReadPendingException`. No es una limitacion de esta implementacion, es la
// regla de la API, y la razon es buena: con dos lecturas en vuelo sobre un mismo flujo de bytes, el
// orden en que se completan decide donde caen los bytes, y eso no lo controla nadie.
//
// Aca ademas hace falta de verdad: abajo hay UN socket bloqueante, y dos hilos leyendolo a la vez se
// repartirian el flujo al azar.
//
// ===============================================================================================
// LOS TIEMPOS LIMITE
// ===============================================================================================
//
// Al agotarse, la operacion falla con `InterruptedByTimeoutException` y **el channel queda
// inservible**: no se puede saber cuantos bytes alcanzo a mover el hilo que quedo adentro de la
// lectura, asi que seguir usandolo seria seguir sobre un flujo desalineado. Se lo marca y las
// operaciones siguientes fallan.
//
// De paquete a proposito: se llega por `AsynchronousSocketChannel.open`.
final class AsyncSocketChannelImpl extends AsynchronousSocketChannel {

    private final SocketChannel channel;
    private final AsyncChannelGroup group;
    private boolean leyendo;
    private boolean escribiendo;
    private boolean desalineado;

    AsyncSocketChannelImpl(AsynchronousChannelProvider provider, SocketChannel channel, AsyncChannelGroup group) {
        super(provider);
        this.channel = channel;
        this.group = group;
        group.register(this);
    }

    @Override
    public AsynchronousSocketChannel bind(SocketAddress local) throws IOException {
        this.channel.bind(local);
        return this;
    }

    @Override
    public <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value)
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
    public AsynchronousSocketChannel shutdownInput() throws IOException {
        this.channel.shutdownInput();
        return this;
    }

    @Override
    public AsynchronousSocketChannel shutdownOutput() throws IOException {
        this.channel.shutdownOutput();
        return this;
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return this.channel.getRemoteAddress();
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return this.channel.getLocalAddress();
    }

    @Override
    public <A> void connect(SocketAddress remote, A attachment,
            CompletionHandler<Void, ? super A> handler) {
        comprobarConexion(remote);
        AsyncTask.notifying(this.group.pool(), new Conectar(this.channel, remote), attachment, handler);
    }

    @Override
    public Future<Void> connect(SocketAddress remote) {
        comprobarConexion(remote);
        return AsyncTask.future(this.group.pool(), new Conectar(this.channel, remote));
    }

    @Override
    public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment,
            CompletionHandler<Integer, ? super A> handler) {
        tomarLectura();
        AsyncTask.notifying(this.group.pool(),
                new Mover(this, dst, timeout, unit, true), attachment, handler);
    }

    @Override
    public Future<Integer> read(ByteBuffer dst) {
        tomarLectura();
        return AsyncTask.future(this.group.pool(),
                new Mover(this, dst, 0L, TimeUnit.MILLISECONDS, true));
    }

    @Override
    public <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit,
            A attachment, CompletionHandler<Long, ? super A> handler) {
        tomarLectura();
        AsyncTask.notifying(this.group.pool(),
                new MoverLargo(this, dsts, offset, length, timeout, unit, true), attachment,
                handler);
    }

    @Override
    public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment,
            CompletionHandler<Integer, ? super A> handler) {
        tomarEscritura();
        AsyncTask.notifying(this.group.pool(),
                new Mover(this, src, timeout, unit, false), attachment, handler);
    }

    @Override
    public Future<Integer> write(ByteBuffer src) {
        tomarEscritura();
        return AsyncTask.future(this.group.pool(),
                new Mover(this, src, 0L, TimeUnit.MILLISECONDS, false));
    }

    @Override
    public <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout, TimeUnit unit,
            A attachment, CompletionHandler<Long, ? super A> handler) {
        tomarEscritura();
        AsyncTask.notifying(this.group.pool(),
                new MoverLargo(this, srcs, offset, length, timeout, unit, false), attachment,
                handler);
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

    private void comprobarConexion(SocketAddress remote) {
        if (remote == null) {
            throw new NullPointerException("remote");
        }
        if (this.channel.isConnected()) {
            throw new AlreadyConnectedException();
        }
    }

    private synchronized void tomarLectura() {
        if (this.leyendo) {
            throw new ReadPendingException();
        }
        this.leyendo = true;
    }

    private synchronized void tomarEscritura() {
        if (this.escribiendo) {
            throw new WritePendingException();
        }
        this.escribiendo = true;
    }

    synchronized void soltar(boolean lectura) {
        if (lectura) {
            this.leyendo = false;
        } else {
            this.escribiendo = false;
        }
    }

    /** Marca que quedo un hilo adentro de una operacion y no se sabe cuanto movio. */
    synchronized void desalinear() {
        this.desalineado = true;
    }

    synchronized void comprobarAlineado() throws IOException {
        if (this.desalineado) {
            throw new IOException("el channel quedo desalineado: una operacion agoto su tiempo");
        }
    }

    SocketChannel bruto() {
        return this.channel;
    }

    /** La conexion, para correr en el pool. */
    private static final class Conectar implements Callable<Void> {

        private final SocketChannel channel;
        private final SocketAddress remoto;

        Conectar(SocketChannel channel, SocketAddress remoto) {
            this.channel = channel;
            this.remoto = remoto;
        }

        public Void call() throws IOException {
            this.channel.connect(this.remoto);
            return null;
        }
    }

    /**
     * Una lectura o escritura que devuelve cuantos bytes movio, como entero.
     *
     * <p>El tiempo limite se aplica aca adentro y no en el `Future`, porque quien tiene que enterarse
     * de que se agoto es el channel --para marcarse desalineado-- y no solo el que espera.
     */
    private static final class Mover implements Callable<Integer> {

        private final AsyncSocketChannelImpl duenio;
        private final ByteBuffer buffer;
        private final long limite;
        private final TimeUnit unidad;
        private final boolean lectura;

        Mover(AsyncSocketChannelImpl duenio, ByteBuffer buffer, long limite, TimeUnit unidad,
                boolean lectura) {
            this.duenio = duenio;
            this.buffer = buffer;
            this.limite = limite;
            this.unidad = unidad;
            this.lectura = lectura;
        }

        public Integer call() throws IOException {
            try {
                this.duenio.comprobarAlineado();
                aplicarLimite(this.duenio, this.limite, this.unidad);
                final SocketChannel c = this.duenio.bruto();
                try {
                    return Integer.valueOf(
                            this.lectura ? c.read(this.buffer) : c.write(this.buffer));
                } catch (java.net.SocketTimeoutException e) {
                    this.duenio.desalinear();
                    throw new InterruptedByTimeoutException();
                }
            } finally {
                this.duenio.soltar(this.lectura);
            }
        }
    }

    /** Lo mismo con varios buffers, que devuelve un `long`. */
    private static final class MoverLargo implements Callable<Long> {

        private final AsyncSocketChannelImpl duenio;
        private final ByteBuffer[] buffers;
        private final int desde;
        private final int cuantos;
        private final long limite;
        private final TimeUnit unidad;
        private final boolean lectura;

        MoverLargo(AsyncSocketChannelImpl duenio, ByteBuffer[] buffers, int desde, int cuantos,
                long limite, TimeUnit unidad, boolean lectura) {
            this.duenio = duenio;
            this.buffers = buffers;
            this.desde = desde;
            this.cuantos = cuantos;
            this.limite = limite;
            this.unidad = unidad;
            this.lectura = lectura;
        }

        public Long call() throws IOException {
            try {
                this.duenio.comprobarAlineado();
                aplicarLimite(this.duenio, this.limite, this.unidad);
                final SocketChannel c = this.duenio.bruto();
                try {
                    return Long.valueOf(this.lectura
                            ? c.read(this.buffers, this.desde, this.cuantos)
                            : c.write(this.buffers, this.desde, this.cuantos));
                } catch (java.net.SocketTimeoutException e) {
                    this.duenio.desalinear();
                    throw new InterruptedByTimeoutException();
                }
            } finally {
                this.duenio.soltar(this.lectura);
            }
        }
    }

    /**
     * Le pone al socket el tiempo limite de esta operacion.
     *
     * <p>Abajo hay un socket bloqueante, asi que el limite se cumple con el suyo: la lectura vuelve
     * con {@code SocketTimeoutException} y aca se la traduce a la que la API declara, marcando el
     * channel como desalineado.
     */
    private static void aplicarLimite(AsyncSocketChannelImpl duenio, long limite, TimeUnit unidad)
            throws IOException {
        if (limite <= 0) {
            return;
        }
        final long ms = unidad.toMillis(limite);
        duenio.bruto().socket().setSoTimeout(ms > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) ms);
    }
}
