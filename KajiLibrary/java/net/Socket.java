package java.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// A TCP socket: the object that is configured before connecting, and the one that remains
// afterwards.
//
// ===========================================================================================
// WHERE THE LINE IS, AND WHY JUST THERE
// ===========================================================================================
//
// A `Socket` freshly built with `new Socket()` **is connected to nothing**, and that is not a
// limitation of KajiJDK: it is the JDK's design. That object --unconnected, with all its options
// settable-- can be fulfilled here a hundred per cent, and it is what this class gives.
//
// **IT REALLY CONNECTS NOW.** This header used to say that nothing needing a peer on the other side
// went in --`connect`, the six connecting constructors, the two streams-- and that was true while the
// VM had nothing to open a socket with. It has it now: `jdk.internal.net.Net`, the same kind of seam
// as `Proc`. So `connect(SocketAddress)`, `connect(SocketAddress, int)`, `Socket(String,int)`,
// `Socket(InetAddress,int)`, `getInputStream()` and `getOutputStream()` are here and speak TCP.
//
// **AND THE SIX THAT WERE MISSING ARE HERE TOO.** The two constructors with a **local address** really
// choose the local end, and with them `sendUrgentData(int)`: all three need what `std::net` does not
// expose --binding before connecting, and sending with the out-of-band flag-- and that is why the VM
// goes down to the system calls (`socket`/`bind`/`connect`/`send`), declared by hand like any other
// seam in this house.
//
// The two with the **`stream`** flag go in for a different reason and it is worth saying, because it
// is the one case in this file where the contract changed: they promised a **UDP** socket wearing a
// `Socket`'s face when given `false`, and the JDK stopped standing behind that -- it throws
// `IllegalArgumentException("Socket constructor does not support creation of datagram sockets")`. It
// was checked against JDK 25 and this class does exactly that. What was impossible to fulfil stopped
// being part of the contract.
//
// **Everything else is real**: the eleven socket options with their validations, the state
// (`isConnected`, `isBound`, `isClosed`, `isInputShutdown`, `isOutputShutdown`), `close`,
// `shutdownInput`/`shutdownOutput` --which throw `SocketException("Socket is not connected")`, which
// is what the JDK throws over an unconnected socket--, `toString`, the implementation factory and the
// `setOption`/`getOption`/`supportedOptions` trio.
//
// The options' default values are set by this class and are documented; in the JDK the operating
// system sets them and they vary from machine to machine, which is why the JDK never promises them.
// What is guaranteed is the only thing that matters: what is set is what is read.
public class Socket implements Closeable {

    static {
        // The bridge `java.nio.channels` uses to get a `Socket` over a handle it already has
        // open. It is installed when this class loads; see `jdk.internal.net.Adoption`.
        jdk.internal.net.Adoption.register(new SocketAdoption());
    }

    private static volatile SocketImplFactory factory;

    private final Proxy proxy;

    private boolean bound;
    private boolean connected;
    private boolean closed;
    private boolean shutIn;
    private boolean shutOut;

    /** The VM's socket, or -1 if this one has not connected yet. */
    private int handle = -1;

    // The local end `bind` asked for, so that the `connect` that follows goes out through it. The
    // empty string is the wildcard. See `bind`'s note.
    private String bindHost = "";
    private int bindPort = 0;

    // The two streams are manufactured **once** and the same ones are always returned: the contract
    // says that closing either of them closes the socket, so two different objects over the same
    // handle would make closing one leave the other believing itself open.
    private InputStream input;
    private OutputStream output;

    // The defaults are the header's. `soLinger` at -1 means "off", which is how the JDK represents
    // "do not wait on close".
    private boolean tcpNoDelay = false;
    private int soLinger = -1;
    private boolean oobInline = false;
    private int soTimeout = 0;
    private boolean keepAlive = false;
    private int trafficClass = 0;
    private boolean reuseAddress = false;
    private int sendBufferSize = 65536;
    private int receiveBufferSize = 65536;

    /** An **unconnected** socket, ready to configure. */
    public Socket() {
        this.proxy = null;
    }

    /**
     * An unconnected socket that, when it connects, would go out through {@code proxy}.
     *
     * <p>{@link Proxy#NO_PROXY} explicitly asks for a direct connection, skipping the VM's
     * {@link ProxySelector}.
     *
     * @throws IllegalArgumentException if {@code proxy} is null
     */
    public Socket(Proxy proxy) {
        if (proxy == null) {
            throw new IllegalArgumentException("Invalid Proxy");
        }
        this.proxy = proxy;
    }

    /**
     * An unconnected socket over the given implementation.
     *
     * <p>It is the constructor for a subclass bringing its own stack.
     *
     * @throws SocketException if the implementation cannot be used
     */
    protected Socket(SocketImpl impl) throws SocketException {
        this.proxy = null;
    }

    private void checkOpen() throws SocketException {
        if (this.closed) {
            throw new SocketException("Socket is closed");
        }
    }

    /**
     * Binds the socket to a local address.
     *
     * @throws IOException if the socket is closed or already bound
     */
    public void bind(SocketAddress bindpoint) throws IOException {
        this.checkOpen();
        if (this.bound) {
            throw new SocketException("Already bound");
        }
        if (bindpoint != null && !(bindpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        // It is recorded, and the `connect` that follows goes out through it. **It does not reserve
        // the port yet**: for that the socket would have to be open from now on, and this class does
        // not open it until it knows where it is going --its `connect` creates the socket with the
        // destination's family, which is unknown until then. The difference shows in a single case:
        // two sockets bound to the same local port fail only when the second connects, not when it
        // binds. It is said here and in the method's javadoc.
        if (bindpoint == null) {
            this.bindHost = "";
            this.bindPort = 0;
        } else {
            InetSocketAddress isa = (InetSocketAddress) bindpoint;
            this.bindHost = isa.getAddress() == null || isa.getAddress().isAnyLocalAddress()
                    ? "" : isa.getAddress().getHostAddress();
            this.bindPort = isa.getPort();
        }
        this.bound = true;
    }

    // ---- estado ----

    /** The far end's address, or null if it is not connected. */
    public InetAddress getInetAddress() {
        if (this.handle < 0) {
            return null;
        }
        String d = jdk.internal.net.Net.remoteAddress(this.handle);
        if (d == null) {
            return null;
        }
        try {
            // It is a numeric literal, so this consults no DNS.
            return InetAddress.getByName(d);
        } catch (UnknownHostException e) {
            // It cannot happen with a numeric literal; if it did, "I do not know" is `null`, which is
            // what this method returns for an unconnected socket.
            return null;
        }
    }

    /**
     * The local address.
     *
     * <p>The wildcard address while the socket is not bound, which is what the JDK returns in the
     * same situation.
     */
    public InetAddress getLocalAddress() {
        if (this.handle >= 0) {
            String d = jdk.internal.net.Net.localAddress(this.handle);
            if (d != null) {
                try {
                    // Es un literal numerico: esto no consulta ningun DNS.
                    return InetAddress.getByName(d);
                } catch (UnknownHostException e) {
                    // It cannot happen with a numeric literal; if it did, it falls to the wildcard below.
                }
            }
        }
        // With no socket yet, the wildcard: it is what the JDK returns over an unbound socket.
        try {
            return InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /** The far end's port, or 0 if it is not connected. */
    public int getPort() {
        if (this.handle < 0) {
            return 0;
        }
        int p = jdk.internal.net.Net.remotePort(this.handle);
        return p < 0 ? 0 : p;
    }

    /** The local port, or -1 if it is not bound. */
    public int getLocalPort() {
        if (this.handle < 0) {
            return this.bound ? 0 : -1;
        }
        int p = jdk.internal.net.Net.localPort(this.handle);
        return p < 0 ? -1 : p;
    }

    /** The far end as a {@link SocketAddress}, or null if it is not connected. */
    public SocketAddress getRemoteSocketAddress() {
        if (!this.isConnected()) {
            return null;
        }
        return new InetSocketAddress(this.getInetAddress(), this.getPort());
    }

    /** The local address as a {@link SocketAddress}, or null if it is not bound. */
    public SocketAddress getLocalSocketAddress() {
        if (!this.isBound()) {
            return null;
        }
        return new InetSocketAddress(this.getLocalAddress(), this.getLocalPort());
    }

    /**
     * The associated NIO channel, or null.
     *
     * <p>Null unless the socket came out of a `SocketChannel`: a socket created with `new` has no
     * channel, neither in the JDK nor here.
     */
    public java.nio.channels.SocketChannel getChannel() {
        return null;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public boolean isBound() {
        return this.bound;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public boolean isInputShutdown() {
        return this.shutIn;
    }

    public boolean isOutputShutdown() {
        return this.shutOut;
    }

    // ---- opciones ----

    /**
     * Sends the data as soon as it is written, without gathering it into a full packet (Nagle's
     * algorithm).
     *
     * @throws SocketException if the socket is closed
     */
    public void setTcpNoDelay(boolean on) throws SocketException {
        this.checkOpen();
        this.tcpNoDelay = on;
        if (this.handle >= 0) {
            jdk.internal.net.Net.setTcpNoDelay(this.handle, on);
        }
    }

    public boolean getTcpNoDelay() throws SocketException {
        this.checkOpen();
        return this.tcpNoDelay;
    }

    /**
     * How many seconds {@link #close} waits for the output buffer to empty.
     *
     * <p>Switching it off and the value are a single state, and that is why there is a single getter:
     * off reads as -1.
     *
     * @throws IllegalArgumentException if it is on with a negative value
     */
    public void setSoLinger(boolean on, int linger) throws SocketException {
        this.checkOpen();
        if (!on) {
            this.soLinger = -1;
            return;
        }
        if (linger < 0) {
            throw new IllegalArgumentException("invalid value for SO_LINGER");
        }
        this.soLinger = linger > 65535 ? 65535 : linger;
    }

    /** The seconds to wait, or -1 if it is off. */
    public int getSoLinger() throws SocketException {
        this.checkOpen();
        return this.soLinger;
    }

    /** Whether urgent data (TCP OOB) arrives mixed in with the rest. */
    public void setOOBInline(boolean on) throws SocketException {
        this.checkOpen();
        this.oobInline = on;
    }

    public boolean getOOBInline() throws SocketException {
        this.checkOpen();
        return this.oobInline;
    }

    /**
     * Milliseconds a read waits; 0 is "forever".
     *
     * @throws IllegalArgumentException if the timeout is negative
     */
    public void setSoTimeout(int timeout) throws SocketException {
        this.checkOpen();
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        this.soTimeout = timeout;
        if (this.handle >= 0) {
            jdk.internal.net.Net.setSoTimeout(this.handle, timeout);
        }
    }

    public int getSoTimeout() throws SocketException {
        this.checkOpen();
        return this.soTimeout;
    }

    /**
     * Suggested size of the output buffer.
     *
     * @throws IllegalArgumentException if the size is not positive
     */
    public void setSendBufferSize(int size) throws SocketException {
        this.checkOpen();
        if (size <= 0) {
            throw new IllegalArgumentException("invalid send size");
        }
        this.sendBufferSize = size;
    }

    public int getSendBufferSize() throws SocketException {
        this.checkOpen();
        return this.sendBufferSize;
    }

    /**
     * Suggested size of the input buffer.
     *
     * @throws IllegalArgumentException if the size is not positive
     */
    public void setReceiveBufferSize(int size) throws SocketException {
        this.checkOpen();
        if (size <= 0) {
            throw new IllegalArgumentException("invalid receive size");
        }
        this.receiveBufferSize = size;
    }

    public int getReceiveBufferSize() throws SocketException {
        this.checkOpen();
        return this.receiveBufferSize;
    }

    /** Sends periodic probes to detect a dead connection. */
    public void setKeepAlive(boolean on) throws SocketException {
        this.checkOpen();
        this.keepAlive = on;
    }

    public boolean getKeepAlive() throws SocketException {
        this.checkOpen();
        return this.keepAlive;
    }

    /**
     * El campo "type of service" de la cabecera IP.
     *
     * @throws IllegalArgumentException si no entra en 0..255
     */
    public void setTrafficClass(int tc) throws SocketException {
        this.checkOpen();
        if (tc < 0 || tc > 255) {
            throw new IllegalArgumentException("tc is not in range 0 -- 255");
        }
        this.trafficClass = tc;
    }

    public int getTrafficClass() throws SocketException {
        this.checkOpen();
        return this.trafficClass;
    }

    /** Whether an address left in TIME_WAIT may be reused. */
    public void setReuseAddress(boolean on) throws SocketException {
        this.checkOpen();
        this.reuseAddress = on;
    }

    public boolean getReuseAddress() throws SocketException {
        this.checkOpen();
        return this.reuseAddress;
    }

    /**
     * What matters most about this connection: connection time, latency or bandwidth.
     *
     * <p>It is a **suggestion**, and the JDK documents that an implementation may ignore it entirely.
     * This one ignores it, which is one of the answers the contract allows -- not an unfulfilled
     * promise.
     */
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    }

    /**
     * Sets an option by its typed constant.
     *
     * @throws UnsupportedOperationException if this class does not support that option
     */
    public <T> Socket setOption(SocketOption<T> name, T value) throws IOException {
        this.checkOpen();
        if (name == null) {
            throw new NullPointerException();
        }
        if (name == StandardSocketOptions.TCP_NODELAY) {
            this.setTcpNoDelay(((Boolean) value).booleanValue());
        } else if (name == StandardSocketOptions.SO_KEEPALIVE) {
            this.setKeepAlive(((Boolean) value).booleanValue());
        } else if (name == StandardSocketOptions.SO_SNDBUF) {
            this.setSendBufferSize(((Integer) value).intValue());
        } else if (name == StandardSocketOptions.SO_RCVBUF) {
            this.setReceiveBufferSize(((Integer) value).intValue());
        } else if (name == StandardSocketOptions.SO_REUSEADDR) {
            this.setReuseAddress(((Boolean) value).booleanValue());
        } else if (name == StandardSocketOptions.SO_LINGER) {
            int v = ((Integer) value).intValue();
            this.setSoLinger(v >= 0, v);
        } else if (name == StandardSocketOptions.IP_TOS) {
            this.setTrafficClass(((Integer) value).intValue());
        } else {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        return this;
    }

    /**
     * An option's value.
     *
     * @throws UnsupportedOperationException if this class does not support that option
     */
    public <T> T getOption(SocketOption<T> name) throws IOException {
        this.checkOpen();
        if (name == null) {
            throw new NullPointerException();
        }
        if (name == StandardSocketOptions.TCP_NODELAY) {
            return (T) Boolean.valueOf(this.getTcpNoDelay());
        }
        if (name == StandardSocketOptions.SO_KEEPALIVE) {
            return (T) Boolean.valueOf(this.getKeepAlive());
        }
        if (name == StandardSocketOptions.SO_SNDBUF) {
            return (T) Integer.valueOf(this.getSendBufferSize());
        }
        if (name == StandardSocketOptions.SO_RCVBUF) {
            return (T) Integer.valueOf(this.getReceiveBufferSize());
        }
        if (name == StandardSocketOptions.SO_REUSEADDR) {
            return (T) Boolean.valueOf(this.getReuseAddress());
        }
        if (name == StandardSocketOptions.SO_LINGER) {
            return (T) Integer.valueOf(this.getSoLinger());
        }
        if (name == StandardSocketOptions.IP_TOS) {
            return (T) Integer.valueOf(this.getTrafficClass());
        }
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    /** The options this socket understands. */
    public Set<SocketOption<?>> supportedOptions() {
        Set<SocketOption<?>> s = new HashSet<SocketOption<?>>();
        s.add(StandardSocketOptions.SO_SNDBUF);
        s.add(StandardSocketOptions.SO_RCVBUF);
        s.add(StandardSocketOptions.SO_KEEPALIVE);
        s.add(StandardSocketOptions.SO_REUSEADDR);
        s.add(StandardSocketOptions.SO_LINGER);
        s.add(StandardSocketOptions.TCP_NODELAY);
        s.add(StandardSocketOptions.IP_TOS);
        return Collections.unmodifiableSet(s);
    }

    // ---- cierre ----

    /**
     * Closes the reading half.
     *
     * @throws SocketException if the socket is not connected -- which is exactly what the JDK throws
     *     over an unconnected socket
     */
    public void shutdownInput() throws IOException {
        this.checkOpen();
        if (!this.isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        this.shutIn = true;
        if (this.handle >= 0) {
            jdk.internal.net.Net.shutdownIn(this.handle);
        }
    }

    /**
     * Closes the writing half, sending a FIN.
     *
     * @throws SocketException if the socket is not connected
     */
    public void shutdownOutput() throws IOException {
        this.checkOpen();
        if (!this.isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        this.shutOut = true;
        if (this.handle >= 0) {
            jdk.internal.net.Net.shutdownOut(this.handle);
        }
    }

    /**
     * Closes the socket. Closing twice does nothing.
     *
     * <p>In the JDK it declares {@code throws IOException}. Here it cannot: this library's
     * {@code java.io.Closeable} declares {@code close()} without the exception, and an override
     * cannot widen the {@code throws} clause (JLS 8.4.8.3). It is the same decision --and for the
     * same reason-- that {@code java.nio.channels.Channel} already took in this tree.
     */
    public void close() throws java.io.IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.handle >= 0) {
            jdk.internal.net.Net.close(this.handle);
            this.handle = -1;
        }
    }

    /**
     * {@code Socket[unconnected]} while it is not connected, which is the JDK's format.
     */
    @Override
    public String toString() {
        if (this.isConnected()) {
            return "Socket[addr=" + this.getInetAddress() + ",port=" + this.getPort()
                    + ",localport=" + this.getLocalPort() + "]";
        }
        return "Socket[unconnected]";
    }

    /**
     * Installs the implementation factory for the whole VM. Once only.
     *
     * @throws Error if one had already been installed
     * @deprecated el JDK deprecio el mecanismo de {@link SocketImpl}
     */
    @Deprecated
    public static synchronized void setSocketImplFactory(SocketImplFactory fac) throws IOException {
        if (factory != null) {
            throw new Error("factory already defined");
        }
        factory = fac;
    }

    // ---- conectar ---------------------------------------------------------------------------

    /**
     * Connects to that address, with no time limit.
     *
     * @throws IOException if it could not connect
     * @throws IllegalArgumentException if the address is not an {@link InetSocketAddress}
     */
    public void connect(SocketAddress endpoint) throws IOException {
        this.connect(endpoint, 0);
    }

    /**
     * Connects to that address, waiting at most `timeout` milliseconds.
     *
     * @param timeout zero means no limit, as in the JDK
     * @throws IOException if it could not connect
     * @throws SocketTimeoutException if the deadline expired
     * @throws IllegalArgumentException if the address is not an {@link InetSocketAddress}, or if the
     *     deadline is negative
     */
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if (endpoint == null) {
            throw new IllegalArgumentException("connect: The address can't be null");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("connect: timeout can't be negative");
        }
        this.checkOpen();
        if (this.connected) {
            throw new SocketException("already connected");
        }
        if (!(endpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        InetSocketAddress isa = (InetSocketAddress) endpoint;
        if (isa.isUnresolved()) {
            throw new UnknownHostException(isa.getHostName());
        }
        String host = isa.getAddress().getHostAddress();
        if (!this.bindHost.isEmpty() || this.bindPort != 0) {
            // There was a `bind` before: it has to go out through there, and that needs the path that
            // binds first.
            this.conectarDesde(isa.getAddress(), isa.getPort(), InetAddress.getByName(
                    this.bindHost.isEmpty() ? "0.0.0.0" : this.bindHost), this.bindPort);
            return;
        }
        int h = jdk.internal.net.Net.connect(host, isa.getPort(), timeout);
        if (h < 0) {
            // The native does not tell "refused" from "no route" from "deadline expired", so the
            // message names the only thing known for certain: where the connection was attempted to.
            // Inventing a reason would be guessing which of the three it was.
            throw new java.net.ConnectException(
                    "Connection refused: " + host + ":" + isa.getPort());
        }
        this.handle = h;
        this.connected = true;
        this.bound = true;
        // What was configured before connecting is applied now: until there is a socket there is
        // nowhere to put it, and losing it would make a `setSoTimeout` before the `connect` do
        // nothing.
        jdk.internal.net.Net.setSoTimeout(h, this.soTimeout);
        jdk.internal.net.Net.setTcpNoDelay(h, this.tcpNoDelay);
    }

    /**
     * Un socket ya conectado a ese host y puerto.
     *
     * @throws UnknownHostException si el nombre no resuelve
     * @throws IOException si no se pudo conectar
     */
    public Socket(String host, int port) throws IOException {
        this.proxy = null;
        this.connect(new InetSocketAddress(InetAddress.getByName(host), port), 0);
    }

    /**
     * A socket already connected to that address and port.
     *
     * @throws IOException if it could not connect
     */
    public Socket(InetAddress address, int port) throws IOException {
        this.proxy = null;
        if (address == null) {
            throw new NullPointerException("address");
        }
        this.connect(new InetSocketAddress(address, port), 0);
    }

    // ---- the streams ------------------------------------------------------------------------

    /**
     * The bytes arriving from the peer.
     *
     * <p>Always the same object: closing it closes the socket, so two different streams over the same
     * socket would leave one believing itself open after the other was closed.
     *
     * @throws IOException if the socket is closed, unconnected, or its reading half is already closed
     */
    public InputStream getInputStream() throws IOException {
        this.checkOpen();
        if (!this.connected) {
            throw new SocketException("Socket is not connected");
        }
        if (this.shutIn) {
            throw new SocketException("Socket input is shutdown");
        }
        if (this.input == null) {
            this.input = new SocketInput(this);
        }
        return this.input;
    }

    /**
     * The bytes going to the peer.
     *
     * @throws IOException if the socket is closed, unconnected, or its writing half is already
     *     closed
     */
    public OutputStream getOutputStream() throws IOException {
        this.checkOpen();
        if (!this.connected) {
            throw new SocketException("Socket is not connected");
        }
        if (this.shutOut) {
            throw new SocketException("Socket output is shutdown");
        }
        if (this.output == null) {
            this.output = new SocketOutput(this);
        }
        return this.output;
    }

    // The handle, for the streams and for `ServerSocket`. Package-private: nobody outside has any
    // reason to know a socket is a number.
    int handle() {
        return this.handle;
    }

    boolean cerrado() {
        return this.closed;
    }

    // The read deadline, which `SocketInput` enforces because the native does not count time.
    int readDeadline() {
        return this.soTimeout;
    }

    // What `ServerSocket.implAccept` needs in order to hand over an already connected socket.
    void adopt(int h) {
        this.handle = h;
        this.connected = true;
        this.bound = true;
    }
    // ---- the constructors that choose the local end -------------------------------------------

    /**
     * A socket connected to {@code host}:{@code port}, **going out through** {@code localAddr}:{@code
     * localPort}.
     *
     * <p>Choosing the local end serves two real purposes: going out through a particular interface on
     * a machine with several, and taking a source port the other side expects. A null
     * {@code localAddr} is the wildcard and a {@code localPort} of zero lets the system choose, which
     * is the same as asking for nothing.
     *
     * @throws UnknownHostException if the name does not resolve
     * @throws IOException if it could not bind or could not connect
     */
    public Socket(String host, int port, InetAddress localAddr, int localPort) throws IOException {
        this.proxy = null;
        this.conectarDesde(InetAddress.getByName(host), port, localAddr, localPort);
    }

    /**
     * A socket connected to {@code address}:{@code port}, going out through {@code localAddr}:{@code
     * localPort}. See {@link #Socket(String, int, InetAddress, int)}.
     *
     * @throws NullPointerException if {@code address} is null
     * @throws IOException if it could not bind or could not connect
     */
    public Socket(InetAddress address, int port, InetAddress localAddr, int localPort)
            throws IOException {
        this.proxy = null;
        if (address == null) {
            throw new NullPointerException("address");
        }
        this.conectarDesde(address, port, localAddr, localPort);
    }

    // The body of both. The waiting is on this side: the native starts the connect on a system thread
    // --it has to block in order to bind first-- and answers through a pigeonhole.
    private void conectarDesde(InetAddress address, int port, InetAddress localAddr, int localPort)
            throws IOException {
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("port out of range:" + port);
        }
        if (localPort < 0 || localPort > 0xFFFF) {
            throw new IllegalArgumentException("localPort out of range:" + localPort);
        }
        String local = localAddr == null ? "" : localAddr.getHostAddress();
        int casillero = jdk.internal.net.Net.connectFromStart(
                address.getHostAddress(), port, local, localPort);
        int h;
        if (casillero < 0) {
            h = -1;
        } else {
            try {
                h = jdk.internal.net.Net.answerPoll(casillero);
                while (h == -3) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new java.io.InterruptedIOException("connect interrupted");
                    }
                    h = jdk.internal.net.Net.answerPoll(casillero);
                }
            } finally {
                jdk.internal.net.Net.answerFree(casillero);
            }
        }
        if (h < 0) {
            // The native does not tell "could not bind" from "could not connect", and both are equally
            // likely here --a taken local port is as common as a downed destination. The message names
            // both ends, which is the only thing known for certain.
            throw new java.net.ConnectException("Connection failed: "
                    + (local.isEmpty() ? "*" : local) + ":" + localPort
                    + " -> " + address.getHostAddress() + ":" + port);
        }
        this.handle = h;
        this.connected = true;
        this.bound = true;
        jdk.internal.net.Net.setSoTimeout(h, this.soTimeout);
        jdk.internal.net.Net.setTcpNoDelay(h, this.tcpNoDelay);
    }

    // ---- the two with the `stream` flag --------------------------------------------------------

    /**
     * A socket connected to {@code host}:{@code port}.
     *
     * @param stream has to be {@code true}. With {@code false} this constructor promised a **UDP**
     *     socket wearing a {@code Socket}'s face, and the JDK stopped standing behind it: it throws
     *     {@link IllegalArgumentException}. This does the same, word for word.
     * @throws IllegalArgumentException if {@code stream} is false
     * @throws IOException if it could not connect
     * @deprecated as in the JDK: use {@link #Socket(String, int)} for TCP and {@link DatagramSocket}
     *     for UDP
     */
    @Deprecated
    public Socket(String host, int port, boolean stream) throws IOException {
        this.proxy = null;
        Socket.exigirFlujo(stream);
        this.connect(new InetSocketAddress(InetAddress.getByName(host), port), 0);
    }

    /**
     * Un socket conectado a {@code address}:{@code port}. Ver {@link #Socket(String, int, boolean)}.
     *
     * @throws IllegalArgumentException si {@code stream} es false
     * @throws NullPointerException si {@code address} es null
     * @throws IOException si no se pudo conectar
     * @deprecated como en el JDK
     */
    @Deprecated
    public Socket(InetAddress address, int port, boolean stream) throws IOException {
        this.proxy = null;
        Socket.exigirFlujo(stream);
        if (address == null) {
            throw new NullPointerException("address");
        }
        this.connect(new InetSocketAddress(address, port), 0);
    }

    // The message is JDK 25's, and it was checked against it: a datagram `Socket` stopped existing,
    // and whoever passes `false` has to find that out and not a connection failure.
    private static void exigirFlujo(boolean stream) {
        if (!stream) {
            throw new IllegalArgumentException(
                    "Socket constructor does not support creation of datagram sockets");
        }
    }

    // ---- fuera de banda -----------------------------------------------------------------------

    /**
     * Sends a byte **out of band**.
     *
     * <p>It is not writing to the stream: it goes with a protocol flag and arrives by a separate
     * path, jumping ahead of whatever is already queued. Only the least significant byte of
     * {@code data} is sent, which is what the contract says.
     *
     * @throws IOException if the socket is not connected or if it could not be sent
     */
    public void sendUrgentData(int data) throws IOException {
        this.checkOpen();
        if (!this.connected) {
            throw new SocketException("Socket is not connected");
        }
        if (this.shutOut) {
            throw new SocketException("Socket output is shutdown");
        }
        if (!jdk.internal.net.Net.sendUrgent(this.handle, data & 0xFF)) {
            throw new IOException("sendUrgentData failed");
        }
    }
}


// The bytes arriving from a socket. It is a view over the handle and not a buffer of its own: reading
// from here reads from the connection at that moment, which is what an `InputStream` promises.
final class SocketInput extends InputStream {

    private final Socket socket;

    SocketInput(Socket socket) {
        this.socket = socket;
    }

    public int read() throws IOException {
        byte[] uno = new byte[1];
        int n = this.read(uno, 0, 1);
        if (n <= 0) {
            return -1;
        }
        // To 0..255: `read()` returns an unsigned byte and -1 means end.
        return uno[0] & 0xFF;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        if (this.socket.cerrado()) {
            throw new SocketException("Socket is closed");
        }
        // The native does not wait: -3 is "nothing has arrived yet". The waiting is done here,
        // sleeping a little between attempts, because sleeping releases the VM's interpreter and lets
        // the thread that has to write on the other side run. It is also what allows the deadline to
        // be counted for real: the -2 is decided by this side, not by the system.
        int deadline = this.socket.readDeadline();
        long comienzo = System.currentTimeMillis();
        int n = jdk.internal.net.Net.read(this.socket.handle(), b, off, len);
        while (n == -3) {
            if (this.socket.cerrado()) {
                throw new SocketException("Socket is closed");
            }
            if (deadline > 0 && System.currentTimeMillis() - comienzo >= deadline) {
                // An expired deadline is not end of stream: the connection is still alive, only
                // quiet.
                throw new SocketTimeoutException("Read timed out");
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.InterruptedIOException("read interrupted");
            }
            n = jdk.internal.net.Net.read(this.socket.handle(), b, off, len);
        }
        if (n == -2) {
            throw new SocketTimeoutException("Read timed out");
        }
        return n;
    }

    public void close() throws IOException {
        this.socket.close();
    }
}

// The bytes going to the socket. Every write goes out at once: a buffer on this side would mean the
// peer did not see what was already written until a `flush()`, and whoever writes has no reason to
// know that.
final class SocketOutput extends OutputStream {

    private final Socket socket;

    SocketOutput(Socket socket) {
        this.socket = socket;
    }

    public void write(int b) throws IOException {
        this.write(new byte[] { (byte) b }, 0, 1);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }
        if (this.socket.cerrado()) {
            throw new SocketException("Socket is closed");
        }
        if (!jdk.internal.net.Net.write(this.socket.handle(), b, off, len)) {
            throw new IOException("Connection reset by peer");
        }
    }

    public void close() throws IOException {
        this.socket.close();
    }

}
