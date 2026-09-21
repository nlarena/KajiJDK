package java.nio.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.Set;

/**
 * This library's {@link ServerSocketChannel}, over the TCP seam of the VM.
 *
 * <h2>The `accept` that returns null</h2>
 *
 * <p>It is the whole difference from a {@link java.net.ServerSocket}, and it is the one that fits
 * most cleanly with this VM: in non-blocking mode {@link #accept()} returns `null` when there is
 * nobody waiting yet. The native of the VM answers exactly that already --a -3, "not yet"-- because
 * it cannot sit waiting without hanging the interpreter. What in the seam is an error code is here
 * the contract word for word.
 *
 * <p>In blocking mode it insists with a short `Thread.sleep` between attempts: sleeping releases the
 * interpreter, so the thread that waits for a connection does not stop the one that is going to open
 * it from advancing.
 *
 * <h2>The options</h2>
 *
 * <p>None of the standard options of a listening socket --`SO_REUSEADDR`, `SO_RCVBUF`-- reaches the
 * system from this VM, so {@link #supportedOptions} comes back **empty** and `setOption` throws
 * {@link UnsupportedOperationException}, which is what the contract requires for an option the
 * channel does not sustain. Keeping the value and returning it from the getter would lie about the
 * one thing that matters: that the option have an effect.
 */
final class KajiServerSocketChannel extends ServerSocketChannel {

    /** The listening socket of the VM, or -1 if it has not been tied yet. */
    private int handle = -1;

    KajiServerSocketChannel(SelectorProvider provider) {
        super(provider);
    }

    private void requireOpen() throws ClosedChannelException {
        if (!this.isOpen()) {
            throw new ClosedChannelException();
        }
    }

    // ---- binding --------------------------------------------------------------------------------

    public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        this.requireOpen();
        if (this.handle >= 0) {
            throw new AlreadyBoundException();
        }
        // With no address, the wildcard on a port the system chooses: it is what the JDK documents
        // for `bind(null)`.
        String host = "0.0.0.0";
        int port = 0;
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
            port = d.getPort();
        }
        int h = jdk.internal.net.Net.listen(host, port, backlog <= 0 ? 50 : backlog);
        if (h < 0) {
            throw new java.net.BindException("Cannot assign requested address: " + host + ":"
                    + port);
        }
        this.handle = h;
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

    public java.net.ServerSocket socket() {
        return (java.net.ServerSocket) jdk.internal.net.Adoption.server(this.handle);
    }

    // ---- accepting -----------------------------------------------------------------------------

    public SocketChannel accept() throws IOException {
        this.requireOpen();
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
            // Without blocking and with nobody waiting: `null`, which is the contract.
            return null;
        }
        if (h < 0) {
            throw new IOException("accept failed");
        }
        return new KajiSocketChannel(this.provider(), h);
    }

    // ---- options ----------------------------------------------------------------------------

    public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        this.requireOpen();
        if (name == null) {
            throw new NullPointerException("name");
        }
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    public <T> T getOption(SocketOption<T> name) throws IOException {
        this.requireOpen();
        if (name == null) {
            throw new NullPointerException("name");
        }
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    public Set<SocketOption<?>> supportedOptions() {
        return Collections.emptySet();
    }

    // ---- closing ------------------------------------------------------------------------------

    protected void implCloseSelectableChannel() throws IOException {
        if (this.handle >= 0) {
            jdk.internal.net.Net.close(this.handle);
            this.handle = -1;
        }
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        // Nothing to say to the system: the socket of the VM is always non-blocking, and the mode is
        // decided by `accept()` when it chooses whether to insist. See the note of the class.
    }

    /**
     * The VM handle of the server socket underneath, or -1 when there is none open.
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
