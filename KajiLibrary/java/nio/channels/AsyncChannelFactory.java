package java.nio.channels;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import java.nio.channels.spi.AsynchronousChannelProvider;

/**
 * Factory of the asynchronous channels, for the provider that lives in
 * {@code java.nio.channels.spi}.
 *
 * <p>Not a JDK class: it is scaffolding of ours, the same kind as {@link MapModes}, which lives in
 * this very package and for the same reason. The implementations --the group and the three
 * channels-- are package-private on purpose: nobody outside builds them, and making them public
 * would add to {@code java.nio.channels} names the JDK does not have. But the provider is in
 * {@code java.nio.channels.spi}, which is another package and does not see them.
 *
 * <p>This is the only bridge between the two, and that is why it is the only extra public thing
 * here: four methods returning JDK types.
 */
public final class AsyncChannelFactory {

    private AsyncChannelFactory() {
    }

    /**
     * A group over that pool.
     *
     * @param provider whoever builds it
     * @param pool where the handlers run
     * @param ownsPool whether the pool was built by the group and not by the caller
     * @return the group
     */
    public static AsynchronousChannelGroup group(AsynchronousChannelProvider provider,
            ExecutorService pool, boolean ownsPool) {
        return new AsyncChannelGroup(provider, pool, ownsPool);
    }

    /**
     * An asynchronous socket channel of that group.
     *
     * @param provider whoever builds it
     * @param group which group it belongs to
     * @return the channel
     * @throws IOException if the socket underneath cannot be opened
     */
    public static AsynchronousSocketChannel socket(AsynchronousChannelProvider provider,
            AsynchronousChannelGroup group) throws IOException {
        return new AsyncSocketChannelImpl(provider, SocketChannel.open(), (AsyncChannelGroup) group);
    }

    /**
     * An asynchronous server channel of that group.
     *
     * @param provider whoever builds it
     * @param group which group it belongs to
     * @return the channel
     * @throws IOException if the socket underneath cannot be opened
     */
    public static AsynchronousServerSocketChannel serverSocket(AsynchronousChannelProvider provider,
            AsynchronousChannelGroup group) throws IOException {
        return new AsyncServerSocketChannelImpl(provider, ServerSocketChannel.open(), (AsyncChannelGroup) group);
    }

    /**
     * An asynchronous file channel of that group.
     *
     * @param channel the blocking channel underneath
     * @param group which group it belongs to
     * @return the channel
     */
    public static AsynchronousFileChannel file(FileChannel channel,
            AsynchronousChannelGroup group) {
        return new AsyncFileChannelImpl((KajiFileChannel) channel, (AsyncChannelGroup) group);
    }

    /**
     * The built-in selector provider, without consulting the installed one.
     *
     * <p>Only {@code SelectorProvider.provider()} uses it, as its last tier: asking for the
     * installed provider from in here would be asking itself.
     *
     * @return the provider, which is a singleton
     */
    public static java.nio.channels.spi.SelectorProvider selectorProvider() {
        return KajiSelectorProvider.builtin();
    }
}
