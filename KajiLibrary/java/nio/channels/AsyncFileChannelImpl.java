package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

// An `AsynchronousFileChannel` over this library's `FileChannel` and a thread pool.
//
// ===============================================================================================
// WHAT ASYNCHRONY BUYS HERE, AND WHAT IT DOES NOT
// ===============================================================================================
//
// **It buys** what the API really promises: asking for a read does not block the asker, many can be
// in flight, and the result arrives through a `Future` or a handler. That is enough for what one
// chooses this API for: not tying up a thread per operation.
//
// **It does not buy** speed. Underneath there is no asynchronous read from the system: there is a
// blocking `FileChannel` running on another thread, and that `FileChannel` --as its own header
// explains-- reads the whole file on every operation. Eight reads in flight are eight threads
// reading the file eight times. The JDK does the same --a pool over blocking reads-- on the
// platforms with no `aio`, so the shape is its own; what changes is the cost of the operation
// underneath, and that is documented where it belongs.
//
// **Cancellation is the JDK's on those same platforms**: `Future.cancel(true)` interrupts the
// thread that is in the operation. If the operation has already started writing, cancelling it does
// not undo it.
//
// Package-private on purpose: it is reached through `AsynchronousFileChannel.open`.
final class AsyncFileChannelImpl extends AsynchronousFileChannel {

    private final KajiFileChannel channel;
    private final AsyncChannelGroup group;

    AsyncFileChannelImpl(KajiFileChannel channel, AsyncChannelGroup group) {
        this.channel = channel;
        this.group = group;
        group.register(this);
    }

    @Override
    public long size() throws IOException {
        return this.channel.size();
    }

    @Override
    public AsynchronousFileChannel truncate(long size) throws IOException {
        this.channel.truncate(size);
        return this;
    }

    @Override
    public void force(boolean metaData) throws IOException {
        this.channel.force(metaData);
    }

    @Override
    public <A> void read(ByteBuffer dst, long position, A attachment,
            CompletionHandler<Integer, ? super A> handler) {
        check(dst, position);
        AsyncTask.notifying(this.group.pool(), new Read(this.channel, dst, position), attachment,
                handler);
    }

    @Override
    public Future<Integer> read(ByteBuffer dst, long position) {
        check(dst, position);
        return AsyncTask.future(this.group.pool(), new Read(this.channel, dst, position));
    }

    @Override
    public <A> void write(ByteBuffer src, long position, A attachment,
            CompletionHandler<Integer, ? super A> handler) {
        check(src, position);
        AsyncTask.notifying(this.group.pool(), new Write(this.channel, src, position), attachment,
                handler);
    }

    @Override
    public Future<Integer> write(ByteBuffer src, long position) {
        check(src, position);
        return AsyncTask.future(this.group.pool(), new Write(this.channel, src, position));
    }

    // ---- locks -----------------------------------------------------------------------------------
    //
    // Taking a lock can block --that is what `wait` means-- so it goes to the pool like every other
    // blocking operation here. `tryLock` does not block by definition, so it runs on the calling
    // thread: sending it to the pool would add a hop and buy nothing.

    @Override
    public <A> void lock(long position, long size, boolean shared, A attachment,
            CompletionHandler<FileLock, ? super A> handler) {
        final KajiFileLock reserved = reserve(position, size, shared);
        AsyncTask.notifying(this.group.pool(), new Lock(this.channel, reserved), attachment,
                handler);
    }

    @Override
    public Future<FileLock> lock(long position, long size, boolean shared) {
        final KajiFileLock reserved = reserve(position, size, shared);
        return AsyncTask.future(this.group.pool(), new Lock(this.channel, reserved));
    }

    /**
     * Claims the region before going to the pool.
     *
     * <p>An overlap has to be refused on the caller's thread: the API throws {@link
     * java.nio.channels.OverlappingFileLockException} instead of delivering it through the future,
     * so the check cannot wait. `IOException` cannot be thrown from here either -- these two
     * methods do not declare it -- so a failure to even check becomes a reservation that the pool
     * will fail on.
     */
    private KajiFileLock reserve(long position, long size, boolean shared) {
        try {
            return this.channel.reserveFor(this, position, size, shared);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return this.channel.acquireFor(this, position, size, shared, false);
    }

    /** Turning a reservation into a real lock, to run on the pool. */
    private static final class Lock implements Callable<FileLock> {

        private final KajiFileChannel channel;
        private final KajiFileLock reserved;

        Lock(KajiFileChannel channel, KajiFileLock reserved) {
            this.channel = channel;
            this.reserved = reserved;
        }

        public FileLock call() throws IOException {
            // `wait` is true: the whole point of the asynchronous form is that waiting costs the
            // caller nothing.
            this.channel.complete(this.reserved, true);
            return this.reserved;
        }
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

    /**
     * The checks that belong on the calling thread and not on the pool.
     *
     * <p>A negative position is a mistake in the program and has to turn up where it was made, not
     * in a `Future` somebody looks at three steps later.
     */
    private void check(ByteBuffer b, long position) {
        if (b == null) {
            throw new NullPointerException("buffer");
        }
        if (position < 0) {
            throw new IllegalArgumentException("Negative position");
        }
        // A closed channel is NOT checked here: the API says that failure arrives through the
        // `Future` or through `failed`, not through the call. `FileChannel` throws it inside the
        // pool, which is where it belongs.
    }

    /** A read, to run on the pool. */
    private static final class Read implements Callable<Integer> {

        private final FileChannel channel;
        private final ByteBuffer dst;
        private final long position;

        Read(FileChannel channel, ByteBuffer dst, long position) {
            this.channel = channel;
            this.dst = dst;
            this.position = position;
        }

        public Integer call() throws IOException {
            return Integer.valueOf(this.channel.read(this.dst, this.position));
        }
    }

    /** A write, to run on the pool. */
    private static final class Write implements Callable<Integer> {

        private final FileChannel channel;
        private final ByteBuffer src;
        private final long position;

        Write(FileChannel channel, ByteBuffer src, long position) {
            this.channel = channel;
            this.src = src;
            this.position = position;
        }

        public Integer call() throws IOException {
            return Integer.valueOf(this.channel.write(this.src, this.position));
        }
    }
}
