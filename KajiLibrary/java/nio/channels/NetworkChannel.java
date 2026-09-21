package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.util.Set;

/**
 * KajiLibrary's java.nio.channels.NetworkChannel — a channel tied to a network socket.
 *
 * <p>What it adds over an ordinary channel are the two things only a socket has: a **local
 * address** to be tied to, and **options** that adjust how it behaves.
 *
 * <p>The options are typed ({@link SocketOption}) and are not pairs of strings, and that is what
 * allows {@link #setOption} to check at compile time that the value corresponds to the option.
 * {@link #supportedOptions()} exists because the set of options depends on the system: asking is
 * the only right way of knowing, instead of trying and catching.
 *
 * <p><strong>The implementations are here.</strong> This note used to say that the library brought
 * none because this VM had no network natives; it has them, and `SocketChannel`,
 * `ServerSocketChannel` and `DatagramChannel` are implemented over that seam.
 */
public interface NetworkChannel extends Channel {

    /**
     * Ties the channel to a local address.
     *
     * @param local the address, or `null` for the system to choose
     */
    NetworkChannel bind(SocketAddress local) throws IOException;

    /** The address it is tied to, or `null` if it is not tied. */
    SocketAddress getLocalAddress() throws IOException;

    /** Sets an option. */
    <T> NetworkChannel setOption(SocketOption<T> name, T value) throws IOException;

    /** The value of an option. */
    <T> T getOption(SocketOption<T> name) throws IOException;

    /** The options this channel admits. */
    Set<SocketOption<?>> supportedOptions();
}
