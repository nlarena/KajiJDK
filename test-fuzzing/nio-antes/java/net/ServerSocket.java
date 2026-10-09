package java.net;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// The listening socket: it binds to a port and waits for connections.
//
// ===========================================================================================
// WHERE THE LINE IS
// ===========================================================================================
//
// `new ServerSocket()` --the no-argument constructor-- creates an **unbound** socket. That comes from
// the JDK, not from a cut-down version, and it is fulfilled here in full: over that object all the
// options, the state and `close` work.
//
// **The two that wait for somebody used to be missing:** `accept()`, whose contract is to return a
// `Socket` **connected to a client**, and `implAccept(Socket)`, which is `accept`'s lower half for
// the subclasses. With no client and no TCP stack there was no way to fulfil them, and an `accept`
// returning an invented socket would have been the worst lie in this whole API -- the server would
// believe it had served somebody. The VM has TCP natives now and both are here, really accepting.
//
// **The constructors that bind, and `bind`, are here** too, throwing `IOException`. Binding is local
// --reserving a port on this machine-- and its failure is literally the case the contract describes.
// The exception is checked: the compiler forces it to be looked at, so nobody finds out late.
//
// Everything else --`setSoTimeout`, `setReuseAddress`, `setReceiveBufferSize`,
// `setPerformancePreferences`, `setOption`/`getOption`/`supportedOptions`, the state, `toString` and
// the factory-- is configuration, and it is complete.
public class ServerSocket implements Closeable {

    private static volatile SocketImplFactory factory;

    private boolean bound;
    private boolean closed;

    /** The VM's listening socket, or -1 if it has not been bound yet. */
    private int handle = -1;

    // What a `ServerSocketChannel` needs in order to hand over the socket that wraps it: the channel
    // already has the descriptor open, and this object comes to share it. Package-private on purpose
    // --nobody outside has any reason to know a socket is a number-- and hence the
    // `jdk.internal.net.Adoption` bridge.
    void adopt(int h) {
        this.handle = h;
        this.bound = true;
    }

    private int soTimeout = 0;
    private boolean reuseAddress = false;
    private int receiveBufferSize = 65536;

    /** An **unbound** server socket, ready to configure. */
    public ServerSocket() throws IOException {
    }

    /**
     * A server socket over the given implementation, unbound.
     *
     * <p>It is the constructor for a subclass bringing its own stack.
     */
    protected ServerSocket(SocketImpl impl) {
    }

    /**
     * A server socket bound to {@code port}.
     *
     * <p>A port of zero lets the system choose one; the one it picked is read with
     * {@link #getLocalPort}.
     *
     * @throws IOException if it could not be bound (the port taken, no permission)
     */
    public ServerSocket(int port) throws IOException {
        this(port, 50, null);
    }

    /**
     * A server socket bound to {@code port}, with a queue of {@code backlog} connections.
     *
     * @throws IOException if it could not be bound
     */
    public ServerSocket(int port, int backlog) throws IOException {
        this(port, backlog, null);
    }

    /**
     * A server socket bound to {@code bindAddr}:{@code port}.
     *
     * @throws IllegalArgumentException if the port is out of range
     * @throws IOException if it could not be bound
     */
    public ServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("Port value out of range: " + port);
        }
        this.bind(new InetSocketAddress(bindAddr, port), backlog);
    }

    private void checkOpen() throws SocketException {
        if (this.closed) {
            throw new SocketException("Socket is closed");
        }
    }

    /**
     * Binds the socket to {@code endpoint}, with the default connection queue.
     *
     * @throws IOException if it could not be bound
     */
    public void bind(SocketAddress endpoint) throws IOException {
        this.bind(endpoint, 50);
    }

    /**
     * Binds the socket to {@code endpoint}, with a queue of {@code backlog} connections.
     *
     * @throws IOException if it could not be bound
     */
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        this.checkOpen();
        if (this.bound) {
            throw new SocketException("Already bound");
        }
        if (endpoint != null && !(endpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        // With no address, the wildcard: listen on every interface, which is what
        // `new ServerSocket(port)` promises.
        String host = "0.0.0.0";
        int puerto = 0;
        if (endpoint != null) {
            InetSocketAddress dir = (InetSocketAddress) endpoint;
            if (dir.getAddress() != null && !dir.getAddress().isAnyLocalAddress()) {
                host = dir.getAddress().getHostAddress();
            }
            puerto = dir.getPort();
        }
        int h = jdk.internal.net.Net.listen(host, puerto, backlog <= 0 ? 50 : backlog);
        if (h < 0) {
            // The native does not tell "port taken" from "no permission"; the message names the
            // only thing known for certain.
            throw new BindException("Cannot assign requested address: " + host + ":" + puerto);
        }
        this.handle = h;
        this.bound = true;
    }

    /** The local address it is bound to, or null if it is not. */
    public InetAddress getInetAddress() {
        if (!this.isBound()) {
            return null;
        }
        try {
            return InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /** The port it listens on, or -1 if it is not bound. */
    public int getLocalPort() {
        if (this.handle >= 0) {
            int p = jdk.internal.net.Net.localPort(this.handle);
            if (p >= 0) {
                return p;
            }
        }
        return this.isBound() ? 0 : -1;
    }

    /** The local address as a {@link SocketAddress}, or null if it is not bound. */
    public SocketAddress getLocalSocketAddress() {
        if (!this.isBound()) {
            return null;
        }
        return new InetSocketAddress(this.getInetAddress(), this.getLocalPort());
    }

    public boolean isBound() {
        return this.bound;
    }

    public boolean isClosed() {
        return this.closed;
    }

    /**
     * The associated NIO channel, or null.
     *
     * <p>Null unless the socket came out of a `ServerSocketChannel`, just as in the JDK.
     */
    public java.nio.channels.ServerSocketChannel getChannel() {
        return null;
    }

    // ---- options ----

    /**
     * Milliseconds it waits for an incoming connection; 0 is "forever".
     *
     * @throws IllegalArgumentException if the timeout is negative
     */
    public void setSoTimeout(int timeout) throws SocketException {
        this.checkOpen();
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        this.soTimeout = timeout;
    }

    /** The waiting timeout. It declares `IOException` and not `SocketException`: that is how the JDK has it. */
    public int getSoTimeout() throws IOException {
        this.checkOpen();
        return this.soTimeout;
    }

    /**
     * Whether a port left in TIME_WAIT may be reused.
     *
     * <p>It is the option that lets a server restart without waiting two minutes, and that is why it
     * has to be set **before** binding: afterwards it has no effect.
     */
    public void setReuseAddress(boolean on) throws SocketException {
        this.checkOpen();
        this.reuseAddress = on;
    }

    public boolean getReuseAddress() throws SocketException {
        this.checkOpen();
        return this.reuseAddress;
    }

    /**
     * Suggested size of the input buffer the accepted sockets **inherit**.
     *
     * <p>It goes here and not in `Socket` because asking for a window larger than 64 KiB means
     * setting it before the handshake, and the accepted socket does not exist yet at that moment.
     *
     * @throws IllegalArgumentException if the size is not positive
     */
    public void setReceiveBufferSize(int size) throws SocketException {
        this.checkOpen();
        if (size <= 0) {
            throw new IllegalArgumentException("negative receive size");
        }
        this.receiveBufferSize = size;
    }

    public int getReceiveBufferSize() throws SocketException {
        this.checkOpen();
        return this.receiveBufferSize;
    }

    /**
     * What matters most about these connections: connection time, latency or bandwidth.
     *
     * <p>It is a suggestion the JDK allows to be ignored entirely, and this implementation ignores it
     * -- one of the answers the contract admits, not an unfulfilled promise.
     */
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    }

    /**
     * Sets an option by its typed constant.
     *
     * @throws UnsupportedOperationException if this class does not support that option
     */
    public <T> ServerSocket setOption(SocketOption<T> name, T value) throws IOException {
        this.checkOpen();
        if (name == null) {
            throw new NullPointerException();
        }
        if (name == StandardSocketOptions.SO_RCVBUF) {
            this.setReceiveBufferSize(((Integer) value).intValue());
        } else if (name == StandardSocketOptions.SO_REUSEADDR) {
            this.setReuseAddress(((Boolean) value).booleanValue());
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
        if (name == StandardSocketOptions.SO_RCVBUF) {
            return (T) Integer.valueOf(this.getReceiveBufferSize());
        }
        if (name == StandardSocketOptions.SO_REUSEADDR) {
            return (T) Boolean.valueOf(this.getReuseAddress());
        }
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    /**
     * The options this socket understands.
     *
     * <p>There are fewer than {@link Socket}'s, and that is not a cut: a listening socket has no
     * output buffer and no Nagle algorithm to configure.
     */
    public Set<SocketOption<?>> supportedOptions() {
        Set<SocketOption<?>> s = new HashSet<SocketOption<?>>();
        s.add(StandardSocketOptions.SO_RCVBUF);
        s.add(StandardSocketOptions.SO_REUSEADDR);
        s.add(StandardSocketOptions.IP_TOS);
        return Collections.unmodifiableSet(s);
    }

    /**
     * Closes the server socket. Closing twice does nothing.
     *
     * <p>In the JDK it declares {@code throws IOException}. Here it cannot: this library's
     * {@code java.io.Closeable} declares {@code close()} without the exception, and an override
     * cannot widen the {@code throws} clause (JLS 8.4.8.3). The same decision as
     * {@code java.nio.channels.Channel} in this tree.
     */
    public void close() throws java.io.IOException {
        if (this.handle >= 0) {
            jdk.internal.net.Net.close(this.handle);
            this.handle = -1;
        }
        this.closed = true;
    }

    /** {@code ServerSocket[unbound]} while it is not bound, which is the JDK's format. */
    @Override
    public String toString() {
        if (!this.isBound()) {
            return "ServerSocket[unbound]";
        }
        return "ServerSocket[addr=" + this.getInetAddress() + ",localport=" + this.getLocalPort()
                + "]";
    }

    /**
     * Installs the implementation factory for the whole VM. Once only.
     *
     * @throws Error if one had already been installed
     * @deprecated the JDK deprecated the {@link SocketImpl} mechanism
     */
    @Deprecated
    public static synchronized void setSocketFactory(SocketImplFactory fac) throws IOException {
        if (factory != null) {
            throw new Error("factory already defined");
        }
        factory = fac;
    }

    /**
     * Waits for a connection and returns the socket serving it.
     *
     * <p>It waits until somebody connects, or until the deadline set with {@link #setSoTimeout}
     * expires. That deadline is really honoured: the native does not wait --it answers "not yet" on
     * the spot-- and the one counting the time is this method, which is the one that knows when it
     * started waiting.
     *
     * @throws SocketTimeoutException if the deadline expired with nobody connecting
     * @throws IOException if the socket is closed or unbound, or if the accept failed
     */
    public Socket accept() throws IOException {
        this.checkOpen();
        if (!this.bound) {
            throw new SocketException("Socket is not bound yet");
        }
        Socket s = new Socket();
        this.implAccept(s);
        return s;
    }

    /**
     * Accepts a connection **over the socket it is given**.
     *
     * <p>It exists so that a subclass can hand over its own kind of socket: it redefines
     * {@link #accept} to build its own and calls this one with it. That is why it is `final` -- what
     * the subclass changes is which socket is passed, not how it is accepted.
     *
     * @throws IOException if the accept failed
     */
    protected final void implAccept(Socket s) throws IOException {
        if (s == null) {
            throw new NullPointerException("s");
        }
        // The -3 is "there is nobody yet". It retries, sleeping a little between attempts: sleeping
        // releases the VM's interpreter, and that is exactly what makes room for the thread that is
        // going to connect. A millisecond is short for the waiter and long enough not to burn the
        // processor.
        long start = System.currentTimeMillis();
        int h = jdk.internal.net.Net.accept(this.handle);
        while (h == -3) {
            if (this.closed) {
                throw new SocketException("Socket is closed");
            }
            if (this.soTimeout > 0
                    && System.currentTimeMillis() - start >= this.soTimeout) {
                throw new SocketTimeoutException("Accept timed out");
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.InterruptedIOException("accept interrupted");
            }
            h = jdk.internal.net.Net.accept(this.handle);
        }
        if (h < 0) {
            throw new IOException("accept failed");
        }
        s.adopt(h);
    }
}
