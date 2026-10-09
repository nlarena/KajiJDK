package java.net;

// The names of the socket options the platform defines.
//
// Each constant is a (name, value type) pair and nothing more: it describes **what can be asked
// for**, it does not ask for it. That is why the class goes into KajiJDK complete even though there
// are no sockets -- it is a vocabulary, and a vocabulary does not promise there is anyone to speak it
// with. The day a socket exists, these same constants are the ones it will accept.
//
// That the type travels in the constant (`SocketOption<Boolean>` and not `SocketOption`) is what
// makes `setOption(SO_KEEPALIVE, 5)` fail to compile. Without it `Object` would have to be passed and
// the error discovered at run time, on the far side of the socket.
//
// All twelve are here. `IP_MULTICAST_IF` --whose type is `SocketOption<NetworkInterface>`-- was out
// for a while because `NetworkInterface` did not exist in this tree; it exists now, with its lookups
// throwing `SocketException` instead of inventing interfaces (see that class's header), and that is
// more than enough to name a constant's type. The constant promises nothing by itself: it says that
// a socket option whose value is an interface **exists**, and that is true.
public final class StandardSocketOptions {

    private StandardSocketOptions() {
    }

    /** Allow sending datagrams to the broadcast address. */
    public static final SocketOption<Boolean> SO_BROADCAST =
            new StdSocketOption<Boolean>("SO_BROADCAST", Boolean.class);

    /** Send periodic probes to detect dead connections. */
    public static final SocketOption<Boolean> SO_KEEPALIVE =
            new StdSocketOption<Boolean>("SO_KEEPALIVE", Boolean.class);

    /** The output buffer's size, in bytes. */
    public static final SocketOption<Integer> SO_SNDBUF =
            new StdSocketOption<Integer>("SO_SNDBUF", Integer.class);

    /** The input buffer's size, in bytes. */
    public static final SocketOption<Integer> SO_RCVBUF =
            new StdSocketOption<Integer>("SO_RCVBUF", Integer.class);

    /** Reuse an address left in TIME_WAIT. */
    public static final SocketOption<Boolean> SO_REUSEADDR =
            new StdSocketOption<Boolean>("SO_REUSEADDR", Boolean.class);

    /** Allow several sockets to listen on the same port and share out the connections. */
    public static final SocketOption<Boolean> SO_REUSEPORT =
            new StdSocketOption<Boolean>("SO_REUSEPORT", Boolean.class);

    /** Seconds `close` waits for the output buffer to empty; negative switches it off. */
    public static final SocketOption<Integer> SO_LINGER =
            new StdSocketOption<Integer>("SO_LINGER", Integer.class);

    /** The IP header's "type of service" field. */
    public static final SocketOption<Integer> IP_TOS =
            new StdSocketOption<Integer>("IP_TOS", Integer.class);

    /** Which interface multicast datagrams go out through. */
    public static final SocketOption<NetworkInterface> IP_MULTICAST_IF =
            new StdSocketOption<NetworkInterface>("IP_MULTICAST_IF", NetworkInterface.class);

    /** How many hops a multicast datagram lives for. */
    public static final SocketOption<Integer> IP_MULTICAST_TTL =
            new StdSocketOption<Integer>("IP_MULTICAST_TTL", Integer.class);

    /** Whether the multicast datagrams sent are also received locally. */
    public static final SocketOption<Boolean> IP_MULTICAST_LOOP =
            new StdSocketOption<Boolean>("IP_MULTICAST_LOOP", Boolean.class);

    /** Send the data as soon as it is written, without waiting to fill a packet (Nagle's algorithm). */
    public static final SocketOption<Boolean> TCP_NODELAY =
            new StdSocketOption<Boolean>("TCP_NODELAY", Boolean.class);

    // An immutable (name, type) pair. It does not define `equals`: options are compared by identity,
    // which is what suits constants, and it is what the JDK does.
    private static class StdSocketOption<T> implements SocketOption<T> {

        private final String name;
        private final Class<T> type;

        StdSocketOption(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }

        public String name() {
            return this.name;
        }

        public Class<T> type() {
            return this.type;
        }

        public String toString() {
            return this.name;
        }
    }
}
