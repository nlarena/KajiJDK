package java.nio.channels;

import java.io.IOException;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * KajiLibrary's java.nio.channels.AsynchronousChannelGroup — the thread pool where the
 * `CompletionHandler`s run.
 *
 * <p>It exists because an asynchronous channel does not attend its own answers: when a read
 * finishes, somebody has to run the {@link CompletionHandler}, and that somebody is a thread of the
 * group. Sharing a group between many channels is the whole point --a thousand connections, eight
 * threads-- and it is what separates this API from "one thread per operation".
 *
 * <p>The two ways of shutting it down are not degrees of the same thing:
 *
 * <ul>
 *   <li>{@link #shutdown()} closes the door: no new channels are accepted, but what is there goes
 *       on until they are all closed. **It returns on the spot** and waits for nothing;
 *   <li>{@link #shutdownNow()} closes the open channels, which makes the operations under way fail
 *       with {@link AsynchronousCloseException}.
 * </ul>
 *
 * <p>A group with an open channel nobody closes **never finishes**, and that is the most common way
 * of failing with this class: a `shutdown()` followed by an `awaitTermination` that does not
 * return.
 *
 * <h2>State in this library</h2>
 *
 * <p><strong>The three statics are here.</strong> This note used to say they were not, with two
 * arguments: that there was no system provider because the VM had no network natives, and that a
 * group with no channels to put into it would be ceremony over nothing. Both stopped holding at the
 * same time: the VM has network natives --{@code jdk.internal.net.Net}-- so
 * {@link AsynchronousSocketChannel} and {@link AsynchronousServerSocketChannel} really open, and
 * with channels inside it the group does exactly what it promises. See
 * {@code KajiAsyncChannelProvider}, which is the stock provider.
 */
public abstract class AsynchronousChannelGroup {

    private final AsynchronousChannelProvider theProvider;

    protected AsynchronousChannelGroup(AsynchronousChannelProvider provider) {
        this.theProvider = provider;
    }

    /**
     * A group with a fixed-size pool.
     *
     * @param nThreads how many threads
     * @param threadFactory which thread factory to build them with
     * @return the group
     * @throws IOException if it cannot be built
     * @throws IllegalArgumentException if `nThreads` is not positive
     * @throws NullPointerException if the factory is null
     */
    public static AsynchronousChannelGroup withFixedThreadPool(int nThreads,
            ThreadFactory threadFactory) throws IOException {
        return AsynchronousChannelProvider.provider()
                .openAsynchronousChannelGroup(nThreads, threadFactory);
    }

    /**
     * A group over a pool that grows as needed.
     *
     * <p>`initialSize` is a suggestion about how many threads to start with; this implementation
     * needs none waiting, so it accepts it and does not use it. See `KajiAsyncChannelProvider`.
     *
     * @param executor the pool
     * @param initialSize how many threads to start with, as a suggestion
     * @return the group
     * @throws IOException if it cannot be built
     * @throws NullPointerException if the pool is null
     */
    public static AsynchronousChannelGroup withCachedThreadPool(ExecutorService executor,
            int initialSize) throws IOException {
        return AsynchronousChannelProvider.provider()
                .openAsynchronousChannelGroup(executor, initialSize);
    }

    /**
     * A group over that pool.
     *
     * <p>The pool comes from outside and **does not shut itself down** when the group finishes:
     * whoever lent it may be using it for something else. Shutting it down is the business of
     * whoever built it.
     *
     * @param executor the pool
     * @return the group
     * @throws IOException if it cannot be built
     * @throws NullPointerException if the pool is null
     */
    public static AsynchronousChannelGroup withThreadPool(ExecutorService executor)
            throws IOException {
        return AsynchronousChannelProvider.provider().openAsynchronousChannelGroup(executor, 0);
    }

    /** The provider that made it. */
    public final AsynchronousChannelProvider provider() {
        return this.theProvider;
    }

    /** Whether it no longer accepts new channels. */
    public abstract boolean isShutdown();

    /** Whether besides that there is nothing left running and the threads have gone. */
    public abstract boolean isTerminated();

    /** Closes the door to new channels and returns on the spot. See the note of the class. */
    public abstract void shutdown();

    /** Closes the open channels; the operations under way fail. */
    public abstract void shutdownNow() throws IOException;

    /**
     * Waits for the group to finish.
     *
     * @return `true` if it finished, `false` if the wait ran out. Telling them apart matters: a
     *         `false` almost always means a channel was left unclosed
     */
    public abstract boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException;
}
