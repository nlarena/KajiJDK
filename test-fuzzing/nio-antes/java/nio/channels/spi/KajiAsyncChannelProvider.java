package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

// The only asynchronous channel provider this library registers out of the box.
//
// ===============================================================================================
// WHAT IT PUTS UNDERNEATH
// ===============================================================================================
//
// A thread pool and the blocking channels the library already had: `SocketChannel` and
// `ServerSocketChannel`, which talk to the real network through `jdk.internal.net.Net`. Every
// asynchronous operation is a blocking one running on a thread of the pool.
//
// **That is a legitimate implementation and not a shortcut**: it is the one the JDK uses on the
// platforms that have no asynchronous I/O of their own. What it buys is what one comes to this API
// for --not tying up a thread of the program per operation-- and what it does not buy is one
// operation per system thread, which is a different thing.
//
// Package-private on purpose: it is not JDK API. It is reached through
// `AsynchronousChannelProvider.provider()`, which returns it when neither the system property nor
// the `ServiceLoader` named another.
final class KajiAsyncChannelProvider extends AsynchronousChannelProvider {

    KajiAsyncChannelProvider() {
    }

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads,
            ThreadFactory threadFactory) throws IOException {
        if (nThreads <= 0) {
            throw new IllegalArgumentException("nThreads <= 0");
        }
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        return group(Executors.newFixedThreadPool(nThreads, threadFactory), true);
    }

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor,
            int initialSize) throws IOException {
        if (executor == null) {
            throw new NullPointerException("executor");
        }
        // `initialSize` is a hint about how many threads to start reading from the pool. Here there
        // are no threads reading from anywhere --each operation is sent to the pool when it is
        // asked for-- so there is nothing to size. Any value is accepted, as in the JDK.
        return group(executor, false);
    }

    @Override
    public AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(
            AsynchronousChannelGroup group) throws IOException {
        return java.nio.channels.AsyncChannelFactory.serverSocket(this, groupOf(group));
    }

    @Override
    public AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup group)
            throws IOException {
        return java.nio.channels.AsyncChannelFactory.socket(this, groupOf(group));
    }

    /**
     * The fallback group, built the first time somebody opens a channel with no group.
     *
     * <p>It is a pool that grows --`newCachedThreadPool`-- because that is what the JDK uses for
     * its own: a group shared by the whole program cannot have a cap chosen in advance.
     */
    synchronized AsynchronousChannelGroup defaultGroup() {
        if (this.fallback == null) {
            this.fallback = group(Executors.newCachedThreadPool(), true);
        }
        return this.fallback;
    }

    private AsynchronousChannelGroup fallback;

    /** The group it was given, or the fallback one when it was given none. */
    private AsynchronousChannelGroup groupOf(AsynchronousChannelGroup group) {
        return group == null ? defaultGroup() : group;
    }

    private AsynchronousChannelGroup group(ExecutorService pool, boolean owned) {
        return java.nio.channels.AsyncChannelFactory.group(this, pool, owned);
    }
}
