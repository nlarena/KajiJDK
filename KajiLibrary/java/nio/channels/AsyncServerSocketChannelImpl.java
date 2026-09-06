package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import java.nio.channels.spi.AsynchronousChannelProvider;

// An `AsynchronousServerSocketChannel` over this library's blocking `ServerSocketChannel` and a
// thread pool.
//
// ===============================================================================================
// ONE ACCEPT IN FLIGHT
// ===============================================================================================
//
// As in the socket channel, and for the same kind of reason: underneath there is a blocking `accept`,
// and two threads calling it at once would split the incoming connections between them in a way
// neither can predict. The API already forbids it --`AcceptPendingException`-- so there is nothing to
// invent.
//
// The accepted channel is registered in the same group: it is what makes the group's `shutdown()`
// wait for the connections the server has been creating as well, and not only for the server.
//
// Package-private on purpose: it is reached through `AsynchronousServerSocketChannel.open`.
final class AsyncServerSocketChannelImpl extends AsynchronousServerSocketChannel {

    private final AsynchronousChannelProvider provider;
    private final ServerSocketChannel channel;
    private final AsyncChannelGroup group;
    private boolean accepting;

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
        take();
        AsyncTask.notifying(this.group.pool(), new Accept(this), attachment, handler);
    }

    @Override
    public Future<AsynchronousSocketChannel> accept() {
        take();
        return AsyncTask.future(this.group.pool(), new Accept(this));
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

    private synchronized void take() {
        if (this.accepting) {
            throw new AcceptPendingException();
        }
        this.accepting = true;
    }

    private synchronized void release() {
        this.accepting = false;
    }

    /** The accept, to run on the pool. */
    private static final class Accept implements Callable<AsynchronousSocketChannel> {

        private final AsyncServerSocketChannelImpl owner;

        Accept(AsyncServerSocketChannelImpl owner) {
            this.owner = owner;
        }

        public AsynchronousSocketChannel call() throws IOException {
            try {
                final SocketChannel incoming = this.owner.channel.accept();
                if (incoming == null) {
                    // The channel underneath is blocking, so this does not happen; if it did,
                    // returning null would be worse than saying so.
                    throw new IOException("accept() returned no channel");
                }
                return new AsyncSocketChannelImpl(this.owner.provider, incoming, this.owner.group);
            } finally {
                this.owner.release();
            }
        }
    }
}
