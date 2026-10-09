package java.nio.channels;

import java.io.IOException;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

/**
 * KajiLibrary's java.nio.channels.AsynchronousFileChannel — a file channel whose operations are
 * asked for and answered afterwards.
 *
 * <p>It does not inherit from {@link FileChannel} and that is not an oversight: they are two
 * different hierarchies over the same file. The visible difference is that **it has no current
 * position**. All of its reads and writes carry the position as an argument, and it has to be that
 * way: with several operations in flight at a time, a shared position would mean nothing --which of
 * the three advanced it first?--.
 *
 * <p>Each operation can be asked for in two ways, and both are there for a different reason: with a
 * {@link CompletionHandler} for code that reacts to events, and returning a {@link Future} for code
 * that at some moment wants to sit down and wait.
 *
 * <h2>State in this library</h2>
 *
 * <p><strong>Both {@code open()}s are here.</strong> This note used to argue that a thread pool
 * over blocking reads would be a facade, because it would promise the properties one chooses this
 * API for. It is worth separating which:
 *
 * <ul>
 *   <li><strong>Not tying up a thread of the program per operation</strong> --which is what it is
 *       chosen for-- is fulfilled: asking for a read returns on the spot and the result arrives
 *       through a `Future` or through a handler;
 *   <li><strong>one operation per thread of the system</strong> is not fulfilled, and cannot be:
 *       the file natives of this VM are synchronous. It is exactly what happens to the JDK on the
 *       platforms without `aio`, where it uses a pool just like this one.
 * </ul>
 *
 * <p>What is left to say is the cost, and it is said where it belongs: the {@link FileChannel}
 * underneath reads the whole file on every operation. See {@code AsyncFileChannelImpl}.
 *
 * <p><strong>The six locking members are in.</strong> This note used to say they were out because a
 * file lock excludes **other processes** and the VM had nothing to do it with. It does now -- see
 * the header of {@link FileChannel}. Waiting for a lock is exactly the kind of thing this API is
 * for: {@link #lock(long, long, boolean)} hands back a {@link java.util.concurrent.Future} instead
 * of parking the caller.
 *
 * <p>What remains, then, is the contract: {@link #size()}, {@link #truncate}, {@link #force} and
 * the four ways of reading and writing, which is what any implementation has to fulfil.
 */
public abstract class AsynchronousFileChannel implements AsynchronousChannel {

    protected AsynchronousFileChannel() {
    }

    /**
     * Opens an asynchronous channel over that file, in the default group.
     *
     * <p>With no option it is opened for reading, just as in {@link FileChannel#open}.
     *
     * @param file the file
     * @param options how to open it
     * @return the channel
     * @throws IOException if it cannot be opened
     */
    public static AsynchronousFileChannel open(java.nio.file.Path file,
            java.nio.file.OpenOption... options) throws IOException {
        final java.util.Set<java.nio.file.OpenOption> set =
                new java.util.HashSet<java.nio.file.OpenOption>();
        if (options != null) {
            for (int i = 0; i < options.length; i++) {
                set.add(options[i]);
            }
        }
        if (set.isEmpty()) {
            set.add(java.nio.file.StandardOpenOption.READ);
        }
        return open(file, set, null);
    }

    /**
     * The same, saying in which pool the handlers run and with which attributes to create the file.
     *
     * <p>The pool goes loose and not as a group because that is what the JDK's signature asks for.
     * It is wrapped in a group here; if it is {@code null}, the default one is used.
     *
     * @param file the file
     * @param options how to open it
     * @param executor where the handlers run, or {@code null} for the default pool
     * @param attrs the attributes to create it with
     * @return the channel
     * @throws IOException if it cannot be opened
     */
    public static AsynchronousFileChannel open(java.nio.file.Path file,
            java.util.Set<? extends java.nio.file.OpenOption> options,
            java.util.concurrent.ExecutorService executor,
            java.nio.file.attribute.FileAttribute<?>... attrs) throws IOException {
        if (options == null) {
            throw new NullPointerException("options");
        }
        final java.nio.file.OpenOption[] array =
                options.toArray(new java.nio.file.OpenOption[options.size()]);
        final FileChannel raw = FileChannel.open(file, array);
        final AsynchronousChannelGroup group = executor == null
                ? AsynchronousChannelProvider.provider().openAsynchronousChannelGroup(
                        java.util.concurrent.Executors.newCachedThreadPool(), 0)
                : AsynchronousChannelGroup.withThreadPool(executor);
        return AsyncChannelFactory.file(raw, group);
    }

    // ---- locks -----------------------------------------------------------------------------------

    /**
     * Takes an exclusive lock over the whole file, and reports through the handler.
     *
     * @param <A> the type of the attachment
     * @param attachment handed back to the handler untouched
     * @param handler told when the lock is taken, or why it was not
     */
    public final <A> void lock(A attachment, CompletionHandler<FileLock, ? super A> handler) {
        lock(0L, Long.MAX_VALUE, false, attachment, handler);
    }

    /**
     * Takes a lock over that region, and reports through the handler.
     *
     * @param <A> the type of the attachment
     * @param position the first byte
     * @param size how many bytes
     * @param shared true for a lock other readers may share
     * @param attachment handed back to the handler untouched
     * @param handler told when the lock is taken, or why it was not
     */
    public abstract <A> void lock(long position, long size, boolean shared, A attachment,
            CompletionHandler<FileLock, ? super A> handler);

    /**
     * Takes an exclusive lock over the whole file.
     *
     * @return a future for the lock
     */
    public final Future<FileLock> lock() {
        return lock(0L, Long.MAX_VALUE, false);
    }

    /**
     * Takes a lock over that region.
     *
     * @param position the first byte
     * @param size how many bytes
     * @param shared true for a lock other readers may share
     * @return a future for the lock
     */
    public abstract Future<FileLock> lock(long position, long size, boolean shared);

    /**
     * Takes an exclusive lock over the whole file without waiting.
     *
     * <p>This one is not asynchronous and does not need to be: not waiting is the whole point, so
     * there is nothing for a future to carry.
     *
     * @return the lock, or {@code null} when another process holds it
     * @throws IOException if it cannot be taken
     */
    public final FileLock tryLock() throws IOException {
        return tryLock(0L, Long.MAX_VALUE, false);
    }

    /**
     * Takes a lock over that region without waiting.
     *
     * @param position the first byte
     * @param size how many bytes
     * @param shared true for a lock other readers may share
     * @return the lock, or {@code null} when another process holds it
     * @throws IOException if it cannot be taken
     */
    public abstract FileLock tryLock(long position, long size, boolean shared) throws IOException;

    /**
     * The size of the file. It is synchronous, in the JDK as well: there is nothing to wait for.
     */
    public abstract long size() throws IOException;

    /** Cuts the file down to `size`. If it was smaller already, it does nothing. */
    public abstract AsynchronousFileChannel truncate(long size) throws IOException;

    /** Forces whatever is pending to the disk. */
    public abstract void force(boolean metaData) throws IOException;

    /**
     * Reads from `position` and tells `handler` when it finishes.
     *
     * <p>`attachment` travels as far as the handler without anything touching it: it is how the
     * context of the operation is carried without a separate map.
     */
    public abstract <A> void read(ByteBuffer dst, long position, A attachment,
            CompletionHandler<Integer, ? super A> handler);

    /** Like the other one, returning the result as a {@link Future}. */
    public abstract Future<Integer> read(ByteBuffer dst, long position);

    /** Writes at `position` and tells `handler` when it finishes. */
    public abstract <A> void write(ByteBuffer src, long position, A attachment,
            CompletionHandler<Integer, ? super A> handler);

    /** Like the other one, returning the result as a {@link Future}. */
    public abstract Future<Integer> write(ByteBuffer src, long position);
}
