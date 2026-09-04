package java.nio.channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * El {@link DatagramChannel} de esta biblioteca, sobre la costura UDP de la VM.
 *
 * <h2>Recibir sin esperar</h2>
 *
 * <p>{@link #receive} devuelve `null` en modo no bloqueante cuando no llego nada, que es el contrato,
 * y es exactamente lo que la costura ya contesta --un -3--. En modo bloqueante se insiste con un
 * `Thread.sleep` corto: dormir suelta el interprete, asi que el hilo que espera un paquete no le
 * impide avanzar al que lo va a mandar.
 *
 * <h2>El remitente y el datagrama son un solo dato</h2>
 *
 * <p>La costura los entrega en tres llamadas --recibir, y despues preguntar de quien vino-- porque un
 * nativo que devuelve un entero no puede devolver los dos. Por eso {@link #receive} es
 * `synchronized`: sin el candado, dos hilos recibiendo sobre el mismo canal podrian llevarse el
 * remitente del otro. Es la misma razon y el mismo candado que en `java.net.DatagramSocket`.
 *
 * <h2>Conectar, que en UDP no manda nada</h2>
 *
 * <p>{@link #connect} fija con quien se habla para que el resto se filtre. Es una decision local: no
 * hay handshake. Aca el filtro lo aplica esta clase --se descarta lo que venga de otro-- porque la
 * costura no tiene forma de pedirselo al sistema, y el resultado observable es el que promete el
 * contrato.
 */
final class KajiDatagramChannel extends DatagramChannel {

    /** El socket de la VM, o -1 si todavia no se ato. */
    private int handle = -1;

    private InetSocketAddress par = null;

    private int ttl = 1;

    private final List<KajiMembershipKey> membresias = new ArrayList<KajiMembershipKey>();

    private static final Set<SocketOption<?>> OPCIONES;

    static {
        Set<SocketOption<?>> s = new HashSet<SocketOption<?>>();
        s.add(StandardSocketOptions.IP_MULTICAST_TTL);
        OPCIONES = Collections.unmodifiableSet(s);
    }

    KajiDatagramChannel(SelectorProvider provider) {
        super(provider);
    }

    private void exigirAbierto() throws ClosedChannelException {
        if (!this.isOpen()) {
            throw new ClosedChannelException();
        }
    }

    // Atar sin que nadie lo pida: mandar o recibir sobre un canal sin atar lo ata, que es lo que
    // hace el JDK. Sin esto, un canal que solo manda no podria mandar nunca.
    private void asegurarAtado() throws IOException {
        if (this.handle < 0) {
            this.bind(null);
        }
    }

    // ---- atar --------------------------------------------------------------------------------

    public DatagramChannel bind(SocketAddress local) throws IOException {
        this.exigirAbierto();
        if (this.handle >= 0) {
            throw new AlreadyBoundException();
        }
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
        int h = jdk.internal.net.Net.udpBind(host, puerto);
        if (h < 0) {
            throw new java.net.BindException("Cannot bind: " + host + ":" + puerto);
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
        return new InetSocketAddress(InetAddress.getByName(d), p);
    }

    public java.net.DatagramSocket socket() {
        return (java.net.DatagramSocket) jdk.internal.net.Adopcion.datagrama(this.handle);
    }

    // ---- conectar ----------------------------------------------------------------------------

    public boolean isConnected() {
        return this.par != null;
    }

    public DatagramChannel connect(SocketAddress remote) throws IOException {
        this.exigirAbierto();
        if (remote == null) {
            throw new IllegalArgumentException("address is null");
        }
        if (!(remote instanceof InetSocketAddress)) {
            throw new UnsupportedAddressTypeException();
        }
        InetSocketAddress d = (InetSocketAddress) remote;
        if (d.isUnresolved()) {
            throw new UnresolvedAddressException();
        }
        this.asegurarAtado();
        this.par = d;
        return this;
    }

    public DatagramChannel disconnect() throws IOException {
        this.exigirAbierto();
        this.par = null;
        return this;
    }

    public SocketAddress getRemoteAddress() throws IOException {
        this.exigirAbierto();
        return this.par;
    }

    // ---- mover datagramas --------------------------------------------------------------------

    public synchronized SocketAddress receive(ByteBuffer dst) throws IOException {
        this.exigirAbierto();
        if (dst == null) {
            throw new NullPointerException("dst");
        }
        this.asegurarAtado();
        int cuantos = dst.remaining();
        byte[] buf = new byte[cuantos];
        while (true) {
            int n = jdk.internal.net.Net.udpReceive(this.handle, buf, 0, cuantos);
            while (n == -3 && this.isBlocking()) {
                if (!this.isOpen()) {
                    throw new AsynchronousCloseException();
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new java.io.InterruptedIOException("receive interrupted");
                }
                n = jdk.internal.net.Net.udpReceive(this.handle, buf, 0, cuantos);
            }
            if (n == -3) {
                return null;
            }
            if (n < 0) {
                throw new IOException("receive failed");
            }
            String dir = jdk.internal.net.Net.udpSenderAddress(this.handle);
            int puerto = jdk.internal.net.Net.udpSenderPort(this.handle);
            InetSocketAddress fuente = dir == null
                    ? null : new InetSocketAddress(InetAddress.getByName(dir), puerto);
            if (this.par != null && !this.par.equals(fuente)) {
                // Canal conectado: lo que no viene del par se descarta **sin entregarlo**. En modo
                // no bloqueante hay que contestar ya --no se puede quedar dando vueltas-- y por eso
                // el `return null`: no llego nada que este canal deba ver.
                if (!this.isBlocking()) {
                    return null;
                }
                continue;
            }
            dst.put(buf, 0, n);
            return fuente;
        }
    }

    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        this.exigirAbierto();
        if (src == null) {
            throw new NullPointerException("src");
        }
        if (!(target instanceof InetSocketAddress)) {
            throw new UnsupportedAddressTypeException();
        }
        InetSocketAddress d = (InetSocketAddress) target;
        if (d.isUnresolved()) {
            throw new UnresolvedAddressException();
        }
        if (this.par != null && !this.par.equals(d)) {
            throw new IllegalArgumentException("Connected address not equal to target address");
        }
        this.asegurarAtado();
        int cuantos = src.remaining();
        byte[] buf = new byte[cuantos];
        src.get(buf, 0, cuantos);
        if (!jdk.internal.net.Net.udpSend(this.handle, d.getAddress().getHostAddress(), d.getPort(),
                buf, 0, cuantos)) {
            throw new IOException("send failed");
        }
        return cuantos;
    }

    // ---- las formas de flujo, que exigen estar conectado --------------------------------------

    public int read(ByteBuffer dst) throws IOException {
        if (this.par == null) {
            throw new NotYetConnectedException();
        }
        SocketAddress fuente = this.receive(dst);
        return fuente == null ? 0 : dst.position();
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        KajiSocketChannel.exigirRango(dsts, offset, length);
        if (this.par == null) {
            throw new NotYetConnectedException();
        }
        // Un datagrama entra en **un** buffer: no se parte entre varios, porque el limite del
        // mensaje es parte del dato. El primero con lugar se lo lleva entero.
        for (int i = 0; i < length; i++) {
            if (dsts[offset + i].remaining() > 0) {
                return this.read(dsts[offset + i]);
            }
        }
        return 0;
    }

    public int write(ByteBuffer src) throws IOException {
        if (this.par == null) {
            throw new NotYetConnectedException();
        }
        return this.send(src, this.par);
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        KajiSocketChannel.exigirRango(srcs, offset, length);
        if (this.par == null) {
            throw new NotYetConnectedException();
        }
        // Los buffers se juntan en **un solo** datagrama: escribir uno por buffer mandaria varios
        // mensajes donde el que llama pidio uno, y del otro lado eso se nota.
        int total = 0;
        for (int i = 0; i < length; i++) {
            total += srcs[offset + i].remaining();
        }
        byte[] buf = new byte[total];
        int pos = 0;
        for (int i = 0; i < length; i++) {
            ByteBuffer b = srcs[offset + i];
            int n = b.remaining();
            b.get(buf, pos, n);
            pos += n;
        }
        this.asegurarAtado();
        if (!jdk.internal.net.Net.udpSend(this.handle, this.par.getAddress().getHostAddress(),
                this.par.getPort(), buf, 0, total)) {
            throw new IOException("send failed");
        }
        return total;
    }

    // ---- multidifusion -----------------------------------------------------------------------

    public MembershipKey join(InetAddress group, NetworkInterface interf) throws IOException {
        return this.sumarse(group, interf, null);
    }

    public MembershipKey join(InetAddress group, NetworkInterface interf, InetAddress source)
            throws IOException {
        if (source == null) {
            throw new NullPointerException("source");
        }
        // Una membresia por emisor la tiene que sostener el sistema, y la costura de esta VM no la
        // pide. Decirlo es lo que el contrato prevé para este caso, y es informacion util: significa
        // "esta pila no filtra por emisor", que no es lo mismo que "no se pudo".
        throw new UnsupportedOperationException("source-specific multicast not supported");
    }

    private MembershipKey sumarse(InetAddress group, NetworkInterface interf, InetAddress source)
            throws IOException {
        this.exigirAbierto();
        if (group == null) {
            throw new NullPointerException("group");
        }
        if (!group.isMulticastAddress()) {
            throw new IllegalArgumentException("Group not a multicast address");
        }
        this.asegurarAtado();
        for (KajiMembershipKey k : this.membresias) {
            if (k.isValid() && k.group().equals(group) && k.mismaPlaca(interf)) {
                throw new IllegalStateException("Already a member of the group");
            }
        }
        String placa = KajiDatagramChannel.nombrarPlaca(group, interf);
        if (!jdk.internal.net.Net.udpJoin(this.handle, group.getHostAddress(), placa)) {
            throw new IOException("join group failed: " + group);
        }
        KajiMembershipKey k = new KajiMembershipKey(this, group, interf, source, placa);
        this.membresias.add(k);
        return k;
    }

    // Da de baja la membresia. Lo llama la llave, que es quien tiene el contrato de `drop()`.
    void soltar(KajiMembershipKey k) {
        if (this.handle >= 0) {
            jdk.internal.net.Net.udpLeave(this.handle, k.group().getHostAddress(), k.placa());
        }
    }

    // En IPv4 la placa se nombra por direccion y en IPv6 por indice: son dos cadenas distintas. La
    // vacia significa "la que elija el sistema".
    static String nombrarPlaca(InetAddress group, NetworkInterface interf) {
        if (interf == null) {
            return "";
        }
        if (group instanceof java.net.Inet6Address) {
            return Integer.toString(interf.getIndex());
        }
        java.util.Enumeration<InetAddress> dirs = interf.getInetAddresses();
        while (dirs.hasMoreElements()) {
            InetAddress d = dirs.nextElement();
            if (d instanceof java.net.Inet4Address) {
                return d.getHostAddress();
            }
        }
        return "";
    }

    // ---- opciones ----------------------------------------------------------------------------

    public <T> DatagramChannel setOption(SocketOption<T> name, T value) throws IOException {
        this.exigirAbierto();
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!StandardSocketOptions.IP_MULTICAST_TTL.equals(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        int v = ((Integer) value).intValue();
        if (v < 0 || v > 255) {
            throw new IllegalArgumentException("Invalid TTL: " + v);
        }
        this.asegurarAtado();
        this.ttl = v;
        jdk.internal.net.Net.udpSetTtl(this.handle, v);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOption(SocketOption<T> name) throws IOException {
        this.exigirAbierto();
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!StandardSocketOptions.IP_MULTICAST_TTL.equals(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        return (T) Integer.valueOf(this.ttl);
    }

    public Set<SocketOption<?>> supportedOptions() {
        return OPCIONES;
    }

    // ---- cierre ------------------------------------------------------------------------------

    protected void implCloseSelectableChannel() throws IOException {
        // Cerrar da de baja todas las membresias, que es lo que `MulticastChannel.close()` promete.
        for (KajiMembershipKey k : this.membresias) {
            k.invalidar();
        }
        this.membresias.clear();
        if (this.handle >= 0) {
            jdk.internal.net.Net.close(this.handle);
            this.handle = -1;
        }
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        // Nada que decirle al sistema; ver la nota de `KajiSocketChannel`.
    }
}
