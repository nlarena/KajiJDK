package java.nio.channels;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ServiceConfigurationError;

/**
 * The provider this library makes its network channels with.
 *
 * <p>It has existed since the VM had a network seam; before that there was nothing to make any of
 * them with.
 *
 * <h2>How the choice between this one and an installed one is made</h2>
 *
 * <p>The {@code open()}s of {@link SocketChannel}, {@link ServerSocketChannel} and {@link
 * DatagramChannel} ask {@link SelectorProvider#provider()} first: if somebody installed their own
 * --through the system property or through {@code META-INF/services}-- that one wins, which is what
 * the contract of `spi` promises and the only reason that mechanism exists. Only when there is none
 * is this one used.
 *
 * <p>The order matters and is not symmetric on purpose: an installed provider replaces the in-house
 * one, never the other way round.
 *
 * <h2>The selector and the pipe</h2>
 *
 * <p>{@link #openSelector()} and {@link #openPipe()} work: see `KajiSelector`.
 *
 * <p>This section used to explain why they could not. The argument was that a selector multiplexes
 * --it waits over **many** channels at a time and wakes up with the ones that are ready-- and that
 * although the channels of this library know how to say "not yet" --hence the non-blocking mode
 * working-- there was no way of **waiting for several without burning the processor**: that needs a
 * `select`/`poll` of the system, and the seam did not expose it. A selector written over polling
 * would have fulfilled the signature and spent a whole core. The seam exposes `poll` now, so the
 * selector is the real thing and the pipe --two joined selectable channels-- has a purpose again.
 */
final class KajiSelectorProvider extends SelectorProvider {

    /** The in-house one. A single one: a provider has no state. */
    private static final KajiSelectorProvider BUILT_IN = new KajiSelectorProvider();

    private KajiSelectorProvider() {
    }

    /**
     * The provider to use: the installed one if there is one, this one otherwise.
     *
     * <p>Which is now simply what {@link SelectorProvider#provider()} answers. It used to catch the
     * {@link ServiceConfigurationError} that method threw when nothing was installed, because this
     * class was the answer to that absence; now it *is* the last tier of that method, so the error
     * cannot happen and catching it would be catching nothing.
     */
    static SelectorProvider current() {
        return SelectorProvider.provider();
    }

    /**
     * This provider, without asking {@link SelectorProvider#provider()} first.
     *
     * <p>It exists to break a loop: `provider()` falls back to this class, so a `provider()` call
     * from in here would call itself forever. Only the fallback uses it.
     *
     * @return the built-in provider
     */
    static SelectorProvider builtin() {
        return BUILT_IN;
    }

    // The families the seam knows how to open. `INET6` is not left out on a whim: the native ties
    // and connects by name, and the one that chooses the family of that address is the system's
    // resolver, not this class. Asking for IPv6 explicitly would be promising something that cannot
    // be guaranteed.
    private static void requireKnownFamily(ProtocolFamily family) {
        if (family == null) {
            throw new NullPointerException("family");
        }
        if (!StandardProtocolFamily.INET.equals(family)) {
            throw new UnsupportedOperationException("Protocol family not supported: " + family);
        }
    }

    public DatagramChannel openDatagramChannel() throws IOException {
        return new KajiDatagramChannel(this);
    }

    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
        KajiSelectorProvider.requireKnownFamily(family);
        return new KajiDatagramChannel(this);
    }

    public ServerSocketChannel openServerSocketChannel() throws IOException {
        return new KajiServerSocketChannel(this);
    }

    public ServerSocketChannel openServerSocketChannel(ProtocolFamily family) throws IOException {
        KajiSelectorProvider.requireKnownFamily(family);
        return new KajiServerSocketChannel(this);
    }

    public SocketChannel openSocketChannel() throws IOException {
        return new KajiSocketChannel(this);
    }

    public SocketChannel openSocketChannel(ProtocolFamily family) throws IOException {
        KajiSelectorProvider.requireKnownFamily(family);
        return new KajiSocketChannel(this);
    }

    public Pipe openPipe() throws IOException {
        return new KajiPipe(this);
    }

    public AbstractSelector openSelector() throws IOException {
        return new KajiSelector(this);
    }
}
