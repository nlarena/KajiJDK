package java.nio.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * El {@link SocketChannel} de esta biblioteca, sobre la costura TCP de la VM.
 *
 * <h2>El modo no bloqueante, que es la razon de ser de esta clase</h2>
 *
 * <p>Un canal se distingue de un {@link java.net.Socket} en que puede no esperar, y eso encaja sin
 * fricciones con la VM: sus nativos de red **tampoco esperan** --contestan -3 cuando todavia no hay
 * nada-- porque un nativo que se quedara esperando colgaria el interprete entero. Lo que en la
 * costura es un codigo de error es aca exactamente la semantica que el contrato pide:
 *
 * <ul>
 *   <li>en modo no bloqueante, un -3 se traduce a `0` bytes leidos, que es lo que un canal contesta
 *       cuando no hay nada;
 *   <li>en modo bloqueante se reintenta con un `Thread.sleep` corto entre intentos --dormir suelta
 *       el interprete y deja correr a los demas hilos-- hasta que llegue algo.
 * </ul>
 *
 * <p>Las dos ramas salen del mismo `-3`. No hay dos caminos: hay uno, y el modo decide si se insiste.
 *
 * <h2>Conectar sin esperar</h2>
 *
 * <p>{@link #connect} en modo no bloqueante devuelve `false` --"arranque, todavia no termino"-- y
 * {@link #finishConnect} lo completa. Eso necesita un `connect` que no bloquee, y el de la costura
 * bloquea: se apoya entonces en `connectFromStart`, que lo corre en un hilo del sistema aparte y
 * deja la respuesta en un casillero. `finishConnect` mira el casillero. Es el mismo mecanismo con el
 * que `InetAddress.isReachable` prueba sin colgar la VM.
 *
 * <h2>Las opciones que se declaran</h2>
 *
 * <p>{@link #supportedOptions} lista **solo** las que la VM puede aplicar de verdad, y las demas
 * tiran {@link UnsupportedOperationException} --que es lo que el contrato manda para una opcion que
 * el canal no sostiene--. Guardar un valor que no llega al sistema y devolverlo desde el getter
 * cumpliria la letra de "lo que se fija es lo que se lee" y mentiria en lo unico que importa: que la
 * opcion tenga efecto.
 */
final class KajiSocketChannel extends SocketChannel {

    /** El socket de la VM, o -1 si todavia no hay conexion. */
    private int handle = -1;

    /** El casillero del connect en curso, o -1. Ver la nota de la clase. */
    private int pendiente = -1;

    private boolean conectado = false;

    /** Lo que pidio un `bind` previo; la cadena vacia es el comodin. */
    private String bindHost = "";
    private int bindPort = 0;
    private boolean atado = false;

    private boolean noDelay = false;

    private static final Set<SocketOption<?>> OPCIONES;

    static {
        Set<SocketOption<?>> s = new HashSet<SocketOption<?>>();
        s.add(StandardSocketOptions.TCP_NODELAY);
        OPCIONES = Collections.unmodifiableSet(s);
    }

    KajiSocketChannel(SelectorProvider provider) {
        super(provider);
    }

    /** El que fabrica `accept()`: nace ya conectado sobre el socket que acepto el escucha. */
    KajiSocketChannel(SelectorProvider provider, int handle) {
        super(provider);
        this.handle = handle;
        this.conectado = true;
        this.atado = true;
    }

    // ---- direcciones -------------------------------------------------------------------------

    private static InetSocketAddress exigirInet(SocketAddress dir) {
        if (dir == null) {
            throw new IllegalArgumentException("address is null");
        }
        if (!(dir instanceof InetSocketAddress)) {
            throw new UnsupportedAddressTypeException();
        }
        InetSocketAddress d = (InetSocketAddress) dir;
        if (d.isUnresolved()) {
            throw new UnresolvedAddressException();
        }
        return d;
    }

    private void exigirAbierto() throws ClosedChannelException {
        if (!this.isOpen()) {
            throw new ClosedChannelException();
        }
    }

    // ---- ciclo de vida -----------------------------------------------------------------------

    public SocketChannel bind(SocketAddress local) throws IOException {
        this.exigirAbierto();
        if (this.atado) {
            throw new AlreadyBoundException();
        }
        if (local == null) {
            this.bindHost = "";
            this.bindPort = 0;
        } else {
            InetSocketAddress d = KajiSocketChannel.exigirInet(local);
            this.bindHost = d.getAddress() == null || d.getAddress().isAnyLocalAddress()
                    ? "" : d.getAddress().getHostAddress();
            this.bindPort = d.getPort();
        }
        // Se anota y el `connect` sale por ahi. **No reserva el puerto todavia**, por lo mismo que
        // `java.net.Socket.bind`: el socket se crea recien al conectar, cuando se sabe la familia
        // del destino. La diferencia se nota en un solo caso --dos canales atados al mismo puerto
        // fallan al conectar el segundo y no al atarlo-- y esta dicho aca.
        this.atado = true;
        return this;
    }

    public boolean connect(SocketAddress remote) throws IOException {
        this.exigirAbierto();
        if (this.conectado) {
            throw new AlreadyConnectedException();
        }
        if (this.pendiente >= 0) {
            throw new ConnectionPendingException();
        }
        InetSocketAddress d = KajiSocketChannel.exigirInet(remote);
        this.pendiente = jdk.internal.net.Net.connectFromStart(
                d.getAddress().getHostAddress(), d.getPort(), this.bindHost, this.bindPort);
        this.atado = true;
        if (this.pendiente < 0) {
            throw new IOException("connect failed");
        }
        if (this.isBlocking()) {
            return this.finishConnect();
        }
        // Sin bloquear: puede que ya este listo --una conexion al loopback suele resolverse en el
        // acto-- y contestar `true` de una es correcto y le ahorra al que llama una vuelta entera
        // por el selector.
        return this.completar(false);
    }

    public boolean finishConnect() throws IOException {
        this.exigirAbierto();
        if (this.conectado) {
            return true;
        }
        if (this.pendiente < 0) {
            throw new NoConnectionPendingException();
        }
        return this.completar(this.isBlocking());
    }

    // Mira el casillero. Con `esperar`, insiste hasta que llegue la respuesta.
    private boolean completar(boolean esperar) throws IOException {
        int r = jdk.internal.net.Net.answerPoll(this.pendiente);
        while (r == -3 && esperar) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.InterruptedIOException("connect interrupted");
            }
            r = jdk.internal.net.Net.answerPoll(this.pendiente);
        }
        if (r == -3) {
            return false;
        }
        jdk.internal.net.Net.answerFree(this.pendiente);
        this.pendiente = -1;
        if (r < 0) {
            throw new java.net.ConnectException("Connection refused");
        }
        this.handle = r;
        this.conectado = true;
        jdk.internal.net.Net.setTcpNoDelay(this.handle, this.noDelay);
        return true;
    }

    public boolean isConnected() {
        return this.conectado;
    }

    public boolean isConnectionPending() {
        return this.pendiente >= 0;
    }

    public SocketChannel shutdownInput() throws IOException {
        this.exigirAbierto();
        if (!this.conectado) {
            throw new NotYetConnectedException();
        }
        jdk.internal.net.Net.shutdownIn(this.handle);
        return this;
    }

    public SocketChannel shutdownOutput() throws IOException {
        this.exigirAbierto();
        if (!this.conectado) {
            throw new NotYetConnectedException();
        }
        jdk.internal.net.Net.shutdownOut(this.handle);
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

    public SocketAddress getRemoteAddress() throws IOException {
        this.exigirAbierto();
        if (!this.conectado) {
            return null;
        }
        String d = jdk.internal.net.Net.remoteAddress(this.handle);
        int p = jdk.internal.net.Net.remotePort(this.handle);
        if (d == null || p < 0) {
            return null;
        }
        return new InetSocketAddress(java.net.InetAddress.getByName(d), p);
    }

    public java.net.Socket socket() {
        return (java.net.Socket) jdk.internal.net.Adopcion.tcp(this.handle);
    }

    // ---- opciones ----------------------------------------------------------------------------

    public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        this.exigirAbierto();
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!StandardSocketOptions.TCP_NODELAY.equals(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        this.noDelay = Boolean.TRUE.equals(value);
        if (this.handle >= 0) {
            jdk.internal.net.Net.setTcpNoDelay(this.handle, this.noDelay);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOption(SocketOption<T> name) throws IOException {
        this.exigirAbierto();
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!StandardSocketOptions.TCP_NODELAY.equals(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        return (T) Boolean.valueOf(this.noDelay);
    }

    public Set<SocketOption<?>> supportedOptions() {
        return OPCIONES;
    }

    // ---- mover bytes -------------------------------------------------------------------------

    public int read(ByteBuffer dst) throws IOException {
        this.exigirAbierto();
        if (!this.conectado) {
            throw new NotYetConnectedException();
        }
        if (dst == null) {
            throw new NullPointerException("dst");
        }
        int cuantos = dst.remaining();
        if (cuantos == 0) {
            return 0;
        }
        byte[] buf = new byte[cuantos];
        int n = jdk.internal.net.Net.read(this.handle, buf, 0, cuantos);
        while (n == -3 && this.isBlocking()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.InterruptedIOException("read interrupted");
            }
            n = jdk.internal.net.Net.read(this.handle, buf, 0, cuantos);
        }
        if (n == -3) {
            // Sin bloquear y sin nada que leer: cero. **No es -1**, que significa fin de flujo, y
            // confundirlos haria que un canal sin trafico se leyera como una conexion cerrada.
            return 0;
        }
        if (n <= 0) {
            return -1;
        }
        dst.put(buf, 0, n);
        return n;
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        KajiSocketChannel.exigirRango(dsts, offset, length);
        long total = 0;
        for (int i = 0; i < length; i++) {
            ByteBuffer b = dsts[offset + i];
            if (b.remaining() == 0) {
                continue;
            }
            int n = this.read(b);
            if (n < 0) {
                return total == 0 ? -1 : total;
            }
            total += n;
            if (n < b.capacity() && total > 0) {
                // Se leyo menos de lo que entraba: no hay mas por ahora, y seguir con el proximo
                // buffer solo agregaria una lectura que va a devolver cero.
                break;
            }
        }
        return total;
    }

    public int write(ByteBuffer src) throws IOException {
        this.exigirAbierto();
        if (!this.conectado) {
            throw new NotYetConnectedException();
        }
        if (src == null) {
            throw new NullPointerException("src");
        }
        int cuantos = src.remaining();
        if (cuantos == 0) {
            return 0;
        }
        byte[] buf = new byte[cuantos];
        src.get(buf, 0, cuantos);
        if (!jdk.internal.net.Net.write(this.handle, buf, 0, cuantos)) {
            throw new IOException("Connection reset by peer");
        }
        return cuantos;
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        KajiSocketChannel.exigirRango(srcs, offset, length);
        long total = 0;
        for (int i = 0; i < length; i++) {
            total += this.write(srcs[offset + i]);
        }
        return total;
    }

    // La validacion que comparten las dos formas dispersas. Es del contrato y no una comodidad:
    // un rango malo tiene que salir como `IndexOutOfBoundsException` antes de tocar la red.
    static void exigirRango(ByteBuffer[] bufs, int offset, int length) {
        if (bufs == null) {
            throw new NullPointerException("bufs");
        }
        if (offset < 0 || length < 0 || offset + length > bufs.length) {
            throw new IndexOutOfBoundsException();
        }
    }

    // ---- cierre ------------------------------------------------------------------------------

    protected void implCloseSelectableChannel() throws IOException {
        if (this.pendiente >= 0) {
            jdk.internal.net.Net.answerFree(this.pendiente);
            this.pendiente = -1;
        }
        if (this.handle >= 0) {
            jdk.internal.net.Net.close(this.handle);
            this.handle = -1;
        }
        this.conectado = false;
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        // No hay nada que decirle al sistema: **el socket de la VM siempre es no bloqueante**, y el
        // modo lo decide esta clase al elegir si insiste o no. Ver la nota de la clase.
    }
}
