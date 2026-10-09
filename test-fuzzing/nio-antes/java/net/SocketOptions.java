package java.net;

// The old vocabulary of socket options: an integer per option and `Object` for the value.
//
// ===========================================================================================
// WHY THERE ARE TWO WAYS OF NAMING THE SAME THING
// ===========================================================================================
//
// KajiJDK has `StandardSocketOptions`, where each option is a (name, type) pair and the compiler can
// reject `setOption(SO_KEEPALIVE, 5)`. **This interface is the earlier one**, and its cost shows at
// once: the option is any old `int` and the value any old `Object`, so confusing two options or
// passing the wrong type is not discovered until run time, on the far side of the socket.
//
// It is kept because it is the one `SocketImpl` and `DatagramSocketImpl` implement --the JDK fixes
// their signature-- and removing it would leave those two undeclarable. It is not an alternative: it
// is the lower layer, and `StandardSocketOptions` is the upper one.
//
// ===========================================================================================
// WHAT GOES IN
// ===========================================================================================
//
// The fifteen constants and the two methods, that is, everything. The constants are agreed numbers
// --the contract says which, and these are they-- and the two methods are **abstract**: this
// interface declares that someone will know how to read and write options, it does not do it.
//
// That nobody in this VM implements it with a real socket changes nothing of what is written here.
// An interface with no implementations is still exactly what it promises: a contract.
public interface SocketOptions {

    /** Send the data as soon as it is written, without coalescing it (Nagle's algorithm off). */
    public static final int TCP_NODELAY = 0x0001;

    /** Which local address to bind to. Read-only: it is set when the socket is bound. */
    public static final int SO_BINDADDR = 0x000F;

    /** Reuse an address left in TIME_WAIT. */
    public static final int SO_REUSEADDR = 0x04;

    /** Allow several sockets to bind to the same port. */
    public static final int SO_REUSEPORT = 0x0E;

    /** Allow sending to the broadcast address. */
    public static final int SO_BROADCAST = 0x0020;

    /** Which interface multicasts go out through (the old form, with an address). */
    public static final int IP_MULTICAST_IF = 0x10;

    /** Which interface multicasts go out through (the new form, with an interface). */
    public static final int IP_MULTICAST_IF2 = 0x1f;

    /** Whether the sender of a multicast also receives it. */
    public static final int IP_MULTICAST_LOOP = 0x12;

    /** The IP header's "type of service" field. */
    public static final int IP_TOS = 0x3;

    /** How long to wait on close for the pending data to go out. */
    public static final int SO_LINGER = 0x0080;

    /** How long a read waits before giving up. */
    public static final int SO_TIMEOUT = 0x1006;

    /** The output buffer's size. */
    public static final int SO_SNDBUF = 0x1001;

    /** The input buffer's size. */
    public static final int SO_RCVBUF = 0x1002;

    /** Send periodic probes to detect a dead connection. */
    public static final int SO_KEEPALIVE = 0x0008;

    /** Deliver the urgent data mixed in with the normal data. */
    public static final int SO_OOBINLINE = 0x1003;

    /**
     * Sets an option.
     *
     * <p>For the options that are a switch, {@code val} is a `Boolean`; turning it off is asked for
     * with `Boolean.FALSE`, not with null.
     *
     * @throws SocketException if the option is not known, the value does not match, or the socket
     *                         refuses it
     */
    public void setOption(int optID, Object value) throws SocketException;

    /**
     * An option's value.
     *
     * <p>The options that are a switch return `Boolean.FALSE` when they are off and the value when
     * they have one -- hence the return type being `Object` and not something more precise.
     *
     * @throws SocketException if the option is not known or the socket cannot read it
     */
    public Object getOption(int optID) throws SocketException;
}
