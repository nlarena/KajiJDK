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
 * This library's {@link SocketChannel}, over the TCP seam of the VM.
 *
 * <h2>The non-blocking mode, which is this class's reason for being</h2>
 *
 * <p>A channel is told apart from a {@link java.net.Socket} in that it can not wait, and that fits
 * the VM without friction: its network natives **do not wait either** --they answer -3 when there is
 * nothing yet-- because a native that sat waiting would hang the whole interpreter. What in the seam
 * is an error code is here exactly the semantics the contract asks for:
 *
 * <ul>
 *   <li>in non-blocking mode, a -3 is translated to `0` bytes read, which is what a channel answers
 *       when there is nothing;
 *   <li>in blocking mode it is retried with a short `Thread.sleep` between attempts --sleeping
 *       releases the interpreter and lets the other threads run-- until something arrives.
 * </ul>
 *
 * <p>Both branches come out of the same `-3`. There are not two roads: there is one, and the mode
 * decides whether to insist.
 *
 * <h2>Connecting without waiting</h2>
 *
 * <p>{@link #connect} in non-blocking mode returns `false` --"I started, I have not finished yet"--
 * and {@link #finishConnect} completes it. That needs a `connect` that does not block, and the one
 * of the seam blocks: it leans then on `connectFromStart`, which runs it in a separate thread of the
 * system and leaves the answer in a pigeonhole. `finishConnect` looks at the pigeonhole. It is the
 * same mechanism with which `InetAddress.isReachable` tests without hanging the VM.
 *
 * <h2>The options that are declared</h2>
 *
 * <p>{@link #supportedOptions} lists **only** the ones the VM can really apply, and the rest throw
 * {@link UnsupportedOperationException} --which is what the contract requires for an option the
 * channel does not sustain--. Keeping a value that does not reach the system and returning it from
 * the getter would fulfil the letter of "what is set is what is read" and would lie about the one
 * thing that matters: that the option have an effect.
 */
final class KajiSocketChannel extends SocketChannel {

    /** The socket of the VM, or -1 if there is no connection yet. */
    private int handle = -1;

    /** The pigeonhole of the connect under way, or -1. See the note of the class. */
    private int pending = -1;

    private boolean connected = false;

    /** What a previous `bind` asked for; the empty string is the wildcard. */
    private String bindHost = "";
    private int bindPort = 0;
    private boolean bound = false;

    private boolean noDelay = false;

    private static final Set<SocketOption<?>> OPTIONS;

    static {
        Set<SocketOption<?>> s = new HashSet<SocketOption<?>>();
        s.add(StandardSocketOptions.TCP_NODELAY);
        OPTIONS = Collections.unmodifiableSet(s);
    }

    KajiSocketChannel(SelectorProvider provider) {
        super(provider);
    }

    /** The one `accept()` makes: it is born connected over the socket the listener accepted. */
    KajiSocketChannel(SelectorProvider provider, int handle) {
        super(provider);
        this.handle = handle;
        this.connected = true;
        this.bound = true;
    }

    // ---- addresses -------------------------------------------------------------------------

    private static InetSocketAddress requireInet(SocketAddress dir) {
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

    private void requireOpen() throws ClosedChannelException {
        if (!this.isOpen()) {
            throw new ClosedChannelException();
        }
    }

    // ---- life cycle --------------------------------------------------------------------------

    public SocketChannel bind(SocketAddress local) throws IOException {
        this.requireOpen();
        if (this.bound) {
            throw new AlreadyBoundException();
        }
        if (local == null) {
            this.bindHost = "";
            this.bindPort = 0;
        } else {
            InetSocketAddress d = KajiSocketChannel.requireInet(local);
            this.bindHost = d.getAddress() == null || d.getAddress().isAnyLocalAddress()
                    ? "" : d.getAddress().getHostAddress();
            this.bindPort = d.getPort();
        }
        // It is noted and the `connect` goes out through there. **It does not reserve the port yet**,
        // for the same reason as `java.net.Socket.bind`: the socket is created only on connecting, when
        // the family of the destination is known. The difference shows in a single case --two channels
        // tied to the same port fail on connecting the second and not on tying it-- and it is said here.
        this.bound = true;
        return this;
    }

    public boolean connect(SocketAddress remote) throws IOException {
        this.requireOpen();
        if (this.connected) {
            throw new AlreadyConnectedException();
        }
        if (this.pending >= 0) {
            throw new ConnectionPendingException();
        }
        InetSocketAddress d = KajiSocketChannel.requireInet(remote);
        this.pending = jdk.internal.net.Net.connectFromStart(
                d.getAddress().getHostAddress(), d.getPort(), this.bindHost, this.bindPort);
        this.bound = true;
        if (this.pending < 0) {
            throw new IOException("connect failed");
        }
        if (this.isBlocking()) {
            return this.finishConnect();
        }
        // Without blocking: it may be ready already --a connection to the loopback is usually resolved
        // on the spot-- and answering `true` straight away is right and saves the caller a whole round
        // through the selector.
        return this.complete(false);
    }

    public boolean finishConnect() throws IOException {
        this.requireOpen();
        if (this.connected) {
            return true;
        }
        if (this.pending < 0) {
            throw new NoConnectionPendingException();
        }
        return this.complete(this.isBlocking());
    }

    // It looks at the pigeonhole. With `wait`, it insists until the answer arrives.
    private boolean complete(boolean wait_) throws IOException {
        int r = jdk.internal.net.Net.answerPoll(this.pending);
        while (r == -3 && wait_) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.InterruptedIOException("connect interrupted");
            }
            r = jdk.internal.net.Net.answerPoll(this.pending);
        }
        if (r == -3) {
            return false;
        }
        jdk.internal.net.Net.answerFree(this.pending);
        this.pending = -1;
        if (r < 0) {
            throw new java.net.ConnectException("Connection refused");
        }
        this.handle = r;
        this.connected = true;
        jdk.internal.net.Net.setTcpNoDelay(this.handle, this.noDelay);
        return true;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public boolean isConnectionPending() {
        return this.pending >= 0;
    }

    public SocketChannel shutdownInput() throws IOException {
        this.requireOpen();
        if (!this.connected) {
            throw new NotYetConnectedException();
        }
        jdk.internal.net.Net.shutdownIn(this.handle);
        return this;
    }

    public SocketChannel shutdownOutput() throws IOException {
        this.requireOpen();
        if (!this.connected) {
            throw new NotYetConnectedException();
        }
        jdk.internal.net.Net.shutdownOut(this.handle);
        return this;
    }

    public SocketAddress getLocalAddress() throws IOException {
        this.requireOpen();
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
        this.requireOpen();
        if (!this.connected) {
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
        return (java.net.Socket) jdk.internal.net.Adoption.tcp(this.handle);
    }

    // ---- options ----------------------------------------------------------------------------

    public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        this.requireOpen();
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
        this.requireOpen();
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!StandardSocketOptions.TCP_NODELAY.equals(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        return (T) Boolean.valueOf(this.noDelay);
    }

    public Set<SocketOption<?>> supportedOptions() {
        return OPTIONS;
    }

    // ---- mover bytes -------------------------------------------------------------------------

    public int read(ByteBuffer dst) throws IOException {
        this.requireOpen();
        if (!this.connected) {
            throw new NotYetConnectedException();
        }
        if (dst == null) {
            throw new NullPointerException("dst");
        }
        int count = dst.remaining();
        if (count == 0) {
            return 0;
        }
        byte[] buf = new byte[count];
        int n = jdk.internal.net.Net.read(this.handle, buf, 0, count);
        while (n == -3 && this.isBlocking()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.InterruptedIOException("read interrupted");
            }
            n = jdk.internal.net.Net.read(this.handle, buf, 0, count);
        }
        if (n == -3) {
            // Without blocking and with nothing to read: zero. **It is not -1**, which means end of
            // stream, and confusing them would make a channel with no traffic read as a closed connection.
            return 0;
        }
        if (n <= 0) {
            return -1;
        }
        dst.put(buf, 0, n);
        return n;
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        KajiSocketChannel.requireRange(dsts, offset, length);
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
                // Less was read than fitted: there is no more for now, and going on with the next buffer
                // would only add a read that is going to return zero.
                break;
            }
        }
        return total;
    }

    public int write(ByteBuffer src) throws IOException {
        this.requireOpen();
        if (!this.connected) {
            throw new NotYetConnectedException();
        }
        if (src == null) {
            throw new NullPointerException("src");
        }
        int count = src.remaining();
        if (count == 0) {
            return 0;
        }
        byte[] buf = new byte[count];
        src.get(buf, 0, count);
        if (!jdk.internal.net.Net.write(this.handle, buf, 0, count)) {
            throw new IOException("Connection reset by peer");
        }
        return count;
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        KajiSocketChannel.requireRange(srcs, offset, length);
        long total = 0;
        for (int i = 0; i < length; i++) {
            total += this.write(srcs[offset + i]);
        }
        return total;
    }

    // The check both scattering forms share. It is of the contract and not a convenience: a bad
    // range has to come out as `IndexOutOfBoundsException` before touching the network.
    static void requireRange(ByteBuffer[] bufs, int offset, int length) {
        if (bufs == null) {
            throw new NullPointerException("bufs");
        }
        if (offset < 0 || length < 0 || offset + length > bufs.length) {
            throw new IndexOutOfBoundsException();
        }
    }

    // ---- closing ------------------------------------------------------------------------------

    protected void implCloseSelectableChannel() throws IOException {
        if (this.pending >= 0) {
            jdk.internal.net.Net.answerFree(this.pending);
            this.pending = -1;
        }
        if (this.handle >= 0) {
            jdk.internal.net.Net.close(this.handle);
            this.handle = -1;
        }
        this.connected = false;
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        // There is nothing to say to the system: **the socket of the VM is always non-blocking**, and
        // the mode is decided by this class when it chooses whether to insist. See the note of the
        // class.
    }

    /**
     * The VM handle of the socket underneath, or -1 when there is none open.
     *
     * <p>Package-private and only for the selector: it is the one thing `poll` needs and the one
     * thing no public method of a channel hands out.
     *
     * @return the handle
     */
    int pollHandle() {
        return this.handle;
    }
}
