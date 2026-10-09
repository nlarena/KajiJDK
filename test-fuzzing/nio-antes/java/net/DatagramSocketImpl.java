package java.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// A `DatagramSocket`'s lower half: what actually talks to the UDP stack.
//
// The split exists so that `DatagramSocket` can be a stable facade and the implementation
// replaceable --by the operating system's, by a tunnel, by a simulation-- without touching the code
// that uses the API.
//
// ===========================================================================================
// IT IS ABSTRACT, AND THAT IS WHY IT GOES IN COMPLETE
// ===========================================================================================
//
// Seventeen of its members are `abstract`: `create`, `bind`, `send`, `receive`, `peek`, `join`,
// `leave` and company. Declaring an abstract method **promises nothing** -- it says "whoever
// implements this has to know how to do it", which is exactly the truth: KajiJDK has nobody to
// implement it, and it says so by having no concrete subclass.
//
// The few that are not abstract belong to this class and touch no network:
//
//  - `connect`/`disconnect`: in the JDK the base **does nothing**, and it is not an oversight -- for
//    UDP, "connecting" is only remembering who one is talking to, and an implementation that does
//    not want to optimize it has nothing to do.
//  - `getLocalPort`/`getFileDescriptor`: they read the two protected fields.
//  - `setOption`/`getOption`/`supportedOptions`: they translate from the new vocabulary
//    (`SocketOption<T>`) to the old one (`SocketOptions`, with integers), which is what the JDK's
//    base does. The translation is a table, and it is complete.
//
// Nothing omitted.
//
// @deprecated The JDK deprecated the whole `DatagramSocketImpl` mechanism.
@Deprecated
public abstract class DatagramSocketImpl implements SocketOptions {

    /** The local port the socket ended up bound to. */
    protected int localPort;

    /** The operating system's descriptor, or null if there is no socket yet. */
    protected FileDescriptor fd;

    public DatagramSocketImpl() {
    }

    /** Creates the socket in the system, without binding it to a port yet. */
    protected abstract void create() throws SocketException;

    /** Binds the socket to {@code laddr}:{@code lport}. */
    protected abstract void bind(int lport, InetAddress laddr) throws SocketException;

    /** Sends the datagram. */
    protected abstract void send(DatagramPacket p) throws IOException;

    /**
     * Remembers who this socket talks to.
     *
     * <p>The base does nothing, and that is right: for UDP there is no handshake, and an
     * implementation that does not filter by origin need not hear about it. One that does want to
     * take advantage of it --so that the system discards datagrams from another origin-- overrides
     * it.
     */
    protected void connect(InetAddress address, int port) throws SocketException {
    }

    /** Stops being "connected". The base does nothing, for the same reason as {@link #connect}. */
    protected void disconnect() {
    }

    /**
     * Peeks at the next datagram's origin without taking it off the queue.
     *
     * @return the port it comes from
     */
    protected abstract int peek(InetAddress i) throws IOException;

    /** Like {@link #peek}, but it also copies the data into {@code p} without consuming it. */
    protected abstract int peekData(DatagramPacket p) throws IOException;

    /** Takes the next datagram and leaves it in {@code p}. */
    protected abstract void receive(DatagramPacket p) throws IOException;

    /**
     * The multicast datagrams' TTL.
     *
     * @deprecated the TTL is an unsigned 8-bit integer and this method uses `byte`, which is signed;
     *     use {@link #setTimeToLive(int)}.
     */
    @Deprecated
    protected abstract void setTTL(byte ttl) throws IOException;

    /**
     * The multicast datagrams' TTL.
     *
     * @deprecated see {@link #setTTL(byte)}.
     */
    @Deprecated
    protected abstract byte getTTL() throws IOException;

    /** How many hops the multicast datagrams going out through here live for. */
    protected abstract void setTimeToLive(int ttl) throws IOException;

    /** The multicast datagrams' TTL. */
    protected abstract int getTimeToLive() throws IOException;

    /**
     * Joins the multicast group {@code inetaddr}.
     *
     * @deprecated it does not let the interface be chosen; use
     *     {@link #joinGroup(SocketAddress, NetworkInterface)}.
     */
    @Deprecated
    protected abstract void join(InetAddress inetaddr) throws IOException;

    /**
     * Leaves the multicast group.
     *
     * @deprecated see {@link #join}.
     */
    @Deprecated
    protected abstract void leave(InetAddress inetaddr) throws IOException;

    /** Joins the group {@code mcastaddr} through the interface {@code netIf}. */
    protected abstract void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf)
            throws IOException;

    /** Leaves the group {@code mcastaddr} on the interface {@code netIf}. */
    protected abstract void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf)
            throws IOException;

    /** Closes the socket. */
    protected abstract void close();

    /** The local port. */
    protected int getLocalPort() {
        return this.localPort;
    }

    /** The system's descriptor, or null. */
    protected FileDescriptor getFileDescriptor() {
        return this.fd;
    }

    /**
     * Sets an option, translating from the typed vocabulary to {@link SocketOptions}' integers.
     *
     * <p>The translation is all the base contributes; the real work is done by the `setOption(int,
     * Object)` the subclass writes. It is done this way --and not the other way round-- because
     * `SocketOptions` is the old interface and it is the one the existing implementations already
     * have written.
     *
     * @throws UnsupportedOperationException if the option has no equivalent
     */
    protected <T> void setOption(SocketOption<T> name, T value) throws IOException {
        int id = optionId(name);
        this.setOption(id, value);
    }

    /**
     * An option's value, with the same translation as {@link #setOption(SocketOption, Object)}.
     *
     * @throws UnsupportedOperationException if the option has no equivalent
     */
    protected <T> T getOption(SocketOption<T> name) throws IOException {
        int id = optionId(name);
        return (T) this.getOption(id);
    }

    // The translation table. It compares by identity because `StandardSocketOptions`' constants are
    // unique, which is precisely what they were made constants for.
    private static int optionId(SocketOption<?> name) {
        if (name == null) {
            throw new NullPointerException();
        }
        if (name == StandardSocketOptions.SO_SNDBUF) {
            return SocketOptions.SO_SNDBUF;
        }
        if (name == StandardSocketOptions.SO_RCVBUF) {
            return SocketOptions.SO_RCVBUF;
        }
        if (name == StandardSocketOptions.SO_REUSEADDR) {
            return SocketOptions.SO_REUSEADDR;
        }
        if (name == StandardSocketOptions.SO_REUSEPORT) {
            return SocketOptions.SO_REUSEPORT;
        }
        if (name == StandardSocketOptions.SO_BROADCAST) {
            return SocketOptions.SO_BROADCAST;
        }
        if (name == StandardSocketOptions.IP_TOS) {
            return SocketOptions.IP_TOS;
        }
        if (name == StandardSocketOptions.IP_MULTICAST_IF) {
            return SocketOptions.IP_MULTICAST_IF2;
        }
        if (name == StandardSocketOptions.IP_MULTICAST_TTL) {
            return SocketOptions.IP_MULTICAST_IF2 + 1;
        }
        if (name == StandardSocketOptions.IP_MULTICAST_LOOP) {
            return SocketOptions.IP_MULTICAST_LOOP;
        }
        throw new UnsupportedOperationException("unsupported option: " + name);
    }

    /** The options this implementation understands. */
    protected Set<SocketOption<?>> supportedOptions() {
        Set<SocketOption<?>> s = new HashSet<SocketOption<?>>();
        s.add(StandardSocketOptions.SO_SNDBUF);
        s.add(StandardSocketOptions.SO_RCVBUF);
        s.add(StandardSocketOptions.SO_REUSEADDR);
        s.add(StandardSocketOptions.IP_TOS);
        return Collections.unmodifiableSet(s);
    }
}
