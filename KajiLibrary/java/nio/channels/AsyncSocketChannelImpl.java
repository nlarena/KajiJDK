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

// An `AsynchronousSocketChannel` over this library's blocking `SocketChannel` and a thread pool.
//
// ===============================================================================================
// THE RULE OF ONE OPERATION IN FLIGHT
// ===============================================================================================
//
// One read and one write pending per channel, and not one more: asking for a second read before the
// first finishes is `ReadPendingException`. It is not a limitation of this implementation, it is the
// API's rule, and the reason is a good one: with two reads in flight over one byte stream, the order
// they complete in decides where the bytes land, and nobody controls that.
//
// Here it is also genuinely necessary: underneath there is ONE blocking socket, and two threads
// reading it at once would split the stream between them at random.
//
// ===============================================================================================
// THE TIMEOUTS
// ===============================================================================================
//
// On running out, the operation fails with `InterruptedByTimeoutException` and **the channel is left
// useless**: there is no telling how many bytes the thread stuck inside the read managed to move, so
// going on using it would be going on over a misaligned stream. It is marked and the following
// operations fail.
//
// Package-private on purpose: it is reached through `AsynchronousSocketChannel.open`.
final class AsyncSocketChannelImpl extends AsynchronousSocketChannel {

    private final SocketChannel channel;
    private final AsyncChannelGroup group;
    private boolean reading;
    private boolean writing;
    private boolean misaligned;

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
        checkConnection(remote);
        AsyncTask.notifying(this.group.pool(), new Connect(this.channel, remote), attachment, handler);
    }

    @Override
    public Future<Void> connect(SocketAddress remote) {
        checkConnection(remote);
        return AsyncTask.future(this.group.pool(), new Connect(this.channel, remote));
    }

    @Override
    public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment,
            CompletionHandler<Integer, ? super A> handler) {
        takeRead();
        AsyncTask.notifying(this.group.pool(),
                new Move(this, dst, timeout, unit, true), attachment, handler);
    }

    @Override
    public Future<Integer> read(ByteBuffer dst) {
        takeRead();
        return AsyncTask.future(this.group.pool(),
                new Move(this, dst, 0L, TimeUnit.MILLISECONDS, true));
    }

    @Override
    public <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit,
            A attachment, CompletionHandler<Long, ? super A> handler) {
        takeRead();
        AsyncTask.notifying(this.group.pool(),
                new MoveLong(this, dsts, offset, length, timeout, unit, true), attachment,
                handler);
    }

    @Override
    public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment,
            CompletionHandler<Integer, ? super A> handler) {
        takeWrite();
        AsyncTask.notifying(this.group.pool(),
                new Move(this, src, timeout, unit, false), attachment, handler);
    }

    @Override
    public Future<Integer> write(ByteBuffer src) {
        takeWrite();
        return AsyncTask.future(this.group.pool(),
                new Move(this, src, 0L, TimeUnit.MILLISECONDS, false));
    }

    @Override
    public <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout, TimeUnit unit,
            A attachment, CompletionHandler<Long, ? super A> handler) {
        takeWrite();
        AsyncTask.notifying(this.group.pool(),
                new MoveLong(this, srcs, offset, length, timeout, unit, false), attachment,
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

    private void checkConnection(SocketAddress remote) {
        if (remote == null) {
            throw new NullPointerException("remote");
        }
        if (this.channel.isConnected()) {
            throw new AlreadyConnectedException();
        }
    }

    private synchronized void takeRead() {
        if (this.reading) {
            throw new ReadPendingException();
        }
        this.reading = true;
    }

    private synchronized void takeWrite() {
        if (this.writing) {
            throw new WritePendingException();
        }
        this.writing = true;
    }

    synchronized void release(boolean read) {
        if (read) {
            this.reading = false;
        } else {
            this.writing = false;
        }
    }

    /** Marks that a thread was left inside an operation and how much it moved is unknown. */
    synchronized void misalign() {
        this.misaligned = true;
    }

    synchronized void checkAligned() throws IOException {
        if (this.misaligned) {
            throw new IOException("the channel was left misaligned: an operation timed out");
        }
    }

    SocketChannel raw() {
        return this.channel;
    }

    /** The connection, to run on the pool. */
    private static final class Connect implements Callable<Void> {

        private final SocketChannel channel;
        private final SocketAddress remote;

        Connect(SocketChannel channel, SocketAddress remote) {
            this.channel = channel;
            this.remote = remote;
        }

        public Void call() throws IOException {
            this.channel.connect(this.remote);
            return null;
        }
    }

    /**
     * A read or a write that returns how many bytes it moved, as an int.
     *
     * <p>The timeout is applied in here and not in the `Future`, because the one that has to learn
     * it ran out is the channel --so it can mark itself misaligned-- and not only whoever waits.
     */
    private static final class Move implements Callable<Integer> {

        private final AsyncSocketChannelImpl owner;
        private final ByteBuffer buffer;
        private final long limit;
        private final TimeUnit unit;
        private final boolean read;

        Move(AsyncSocketChannelImpl owner, ByteBuffer buffer, long limit, TimeUnit unit,
                boolean read) {
            this.owner = owner;
            this.buffer = buffer;
            this.limit = limit;
            this.unit = unit;
            this.read = read;
        }

        public Integer call() throws IOException {
            try {
                this.owner.checkAligned();
                applyTimeout(this.owner, this.limit, this.unit);
                final SocketChannel c = this.owner.raw();
                try {
                    return Integer.valueOf(
                            this.read ? c.read(this.buffer) : c.write(this.buffer));
                } catch (java.net.SocketTimeoutException e) {
                    this.owner.misalign();
                    throw new InterruptedByTimeoutException();
                }
            } finally {
                this.owner.release(this.read);
            }
        }
    }

    /** The same with several buffers, returning a `long`. */
    private static final class MoveLong implements Callable<Long> {

        private final AsyncSocketChannelImpl owner;
        private final ByteBuffer[] buffers;
        private final int from;
        private final int count;
        private final long limit;
        private final TimeUnit unit;
        private final boolean read;

        MoveLong(AsyncSocketChannelImpl owner, ByteBuffer[] buffers, int from, int count,
                long limit, TimeUnit unit, boolean read) {
            this.owner = owner;
            this.buffers = buffers;
            this.from = from;
            this.count = count;
            this.limit = limit;
            this.unit = unit;
            this.read = read;
        }

        public Long call() throws IOException {
            try {
                this.owner.checkAligned();
                applyTimeout(this.owner, this.limit, this.unit);
                final SocketChannel c = this.owner.raw();
                try {
                    return Long.valueOf(this.read
                            ? c.read(this.buffers, this.from, this.count)
                            : c.write(this.buffers, this.from, this.count));
                } catch (java.net.SocketTimeoutException e) {
                    this.owner.misalign();
                    throw new InterruptedByTimeoutException();
                }
            } finally {
                this.owner.release(this.read);
            }
        }
    }

    /**
     * Sets this operation's timeout on the socket.
     *
     * <p>Underneath there is a blocking socket, so the limit is met with its own: the read comes back
     * with {@code SocketTimeoutException} and here it is translated into the one the API declares,
     * marking the channel misaligned.
     */
    private static void applyTimeout(AsyncSocketChannelImpl owner, long limit, TimeUnit unit)
            throws IOException {
        if (limit <= 0) {
            return;
        }
        final long ms = unit.toMillis(limit);
        owner.raw().socket().setSoTimeout(ms > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) ms);
    }
}
