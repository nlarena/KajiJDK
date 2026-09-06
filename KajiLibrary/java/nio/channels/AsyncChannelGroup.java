package java.nio.channels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import java.nio.channels.spi.AsynchronousChannelProvider;

// This library's channel group: an `ExecutorService` and the list of channels opened with it.
//
// ===============================================================================================
// WHAT MAKES A GROUP MORE THAN A POOL
// ===============================================================================================
//
// A pool runs tasks. A group also **knows which channels depend on it**, and that is the whole
// difference between its two ways of shutting down: `shutdown()` stops taking new channels and
// waits for the open ones to close themselves; `shutdownNow()` closes them itself. Without the list
// of channels the first could not tell when it was done, and the second would have nobody to close.
//
// That is where the commonest way to get stuck with this API comes from: a `shutdown()` followed by
// an `awaitTermination` on a group holding a channel nobody closes never returns. It is not a fault
// of this implementation, it is what the class promises.
//
// Package-private on purpose: nobody outside builds one. The three statics on
// `AsynchronousChannelGroup` are the way in.
final class AsyncChannelGroup extends AsynchronousChannelGroup {

    private final ExecutorService pool;
    private final boolean ownsPool;
    private final List<AsynchronousChannel> channels = new ArrayList<AsynchronousChannel>();
    private boolean shuttingDown;

    AsyncChannelGroup(AsynchronousChannelProvider provider, ExecutorService pool,
            boolean ownsPool) {
        super(provider);
        this.pool = pool;
        this.ownsPool = ownsPool;
    }

    /** Where the `CompletionHandler`s of this group's channels run. */
    ExecutorService pool() {
        return this.pool;
    }

    /**
     * Records a channel that was just opened.
     *
     * @param channel the channel
     * @throws ShutdownChannelGroupException if the group takes no more channels
     */
    synchronized void register(AsynchronousChannel channel) {
        if (this.shuttingDown) {
            throw new ShutdownChannelGroupException();
        }
        this.channels.add(channel);
    }

    /** Drops a channel that closed. */
    synchronized void unregister(AsynchronousChannel channel) {
        this.channels.remove(channel);
        if (this.shuttingDown && this.channels.isEmpty()) {
            this.pool.shutdown();
        }
    }

    @Override
    public synchronized boolean isShutdown() {
        return this.shuttingDown;
    }

    @Override
    public synchronized boolean isTerminated() {
        return this.shuttingDown && this.channels.isEmpty() && this.pool.isTerminated();
    }

    @Override
    public void shutdown() {
        final boolean empty;
        synchronized (this) {
            this.shuttingDown = true;
            empty = this.channels.isEmpty();
        }
        if (empty) {
            this.pool.shutdown();
        }
    }

    @Override
    public void shutdownNow() throws IOException {
        final List<AsynchronousChannel> copy;
        synchronized (this) {
            this.shuttingDown = true;
            copy = new ArrayList<AsynchronousChannel>(this.channels);
        }
        for (int i = 0; i < copy.size(); i++) {
            try {
                copy.get(i).close();
            } catch (IOException ignored) {
                // Closing the rest matters more than reporting this one: the caller asked for
                // everything to go down.
            }
        }
        this.pool.shutdownNow();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.pool.awaitTermination(timeout, unit);
    }

    /**
     * Whether the group built the pool rather than being handed one.
     *
     * <p>It matters for {@code withThreadPool}: a pool that came from outside is not shut down when
     * the group terminates, because whoever lent it may still be using it for something else.
     *
     * @return true when the group owns the pool
     */
    boolean ownsPool() {
        return this.ownsPool;
    }
}
