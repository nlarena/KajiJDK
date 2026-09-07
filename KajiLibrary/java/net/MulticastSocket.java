package java.net;

import java.io.IOException;

// A `DatagramSocket` with the multicast options on top.
//
// ===========================================================================================
// THE SAME LINE AS `DatagramSocket`, MOVED ALONG ONE PLACE
// ===========================================================================================
//
// Everything this class adds over its parent is **socket options**: the multicast datagrams' TTL,
// which interface they go out through, and whether one's own are received. All three are local
// state, are set before anything is sent, and are complete.
//
// **THE FOUR MEMBERSHIP METHODS NOW WORK.** This header used to say that joining a multicast group
// --which means sending an IGMP to the router and being noted in a table that lives outside this
// process-- could not be fulfilled without a network, and that was true while the VM had no UDP. It
// has it now, and all four really do register: if the system refuses the membership, an
// `IOException` comes out instead of a silence that would leave the program waiting for datagrams
// that are never going to arrive.
//
// The work is in `DatagramSocket`, which is where the socket lives. The two here that take a bare
// `InetAddress` are the same ones with whatever interface this socket has configured -- which is
// exactly what the JDK documents them as doing.
public class MulticastSocket extends DatagramSocket {

    private int timeToLive = 1;
    private InetAddress ifAddress;
    private NetworkInterface netIf;
    private boolean loopbackDisabled = false;

    /**
     * A multicast socket bound to any port.
     *
     * @throws IOException if it could not be bound
     */
    public MulticastSocket() throws IOException {
        super(new InetSocketAddress(0));
    }

    /**
     * A multicast socket bound to {@code port}.
     *
     * @throws IOException if it could not be bound
     */
    public MulticastSocket(int port) throws IOException {
        super(new InetSocketAddress(port));
    }

    /**
     * A multicast socket bound to {@code bindaddr}, or **unbound** if it is null.
     *
     * <p>The null case works in full: it is the socket on which all this class's options can be set
     * and read.
     *
     * @throws IOException if it could not be bound
     */
    public MulticastSocket(SocketAddress bindaddr) throws IOException {
        super(bindaddr);
    }

    /**
     * How many hops the multicast datagrams going out from here live for.
     *
     * @throws IllegalArgumentException if it does not fit 0..255
     */
    public void setTimeToLive(int ttl) throws IOException {
        if (ttl < 0 || ttl > 255) {
            throw new IllegalArgumentException("ttl out of range");
        }
        this.checkOpenIO();
        this.timeToLive = ttl;
    }

    /** The multicast datagrams' TTL. It starts at 1, which is the platform's default. */
    public int getTimeToLive() throws IOException {
        this.checkOpenIO();
        return this.timeToLive;
    }

    /**
     * The TTL, in a byte.
     *
     * @deprecated the TTL runs from 0 to 255 and a Java `byte` is signed, so 128 upwards are written
     *     negative. Use {@link #setTimeToLive(int)}.
     */
    @Deprecated
    public void setTTL(byte ttl) throws IOException {
        this.setTimeToLive(ttl & 0xFF);
    }

    /**
     * The TTL, in a byte.
     *
     * @deprecated see {@link #setTTL(byte)}.
     */
    @Deprecated
    public byte getTTL() throws IOException {
        return (byte) this.getTimeToLive();
    }

    /**
     * Which local address the multicast datagrams go out through.
     *
     * @throws SocketException if the socket is closed
     */
    public void setInterface(InetAddress inf) throws SocketException {
        this.checkOpenSocket();
        if (inf == null) {
            throw new SocketException("Invalid value");
        }
        this.ifAddress = inf;
    }

    /**
     * The local address the multicast datagrams go out through.
     *
     * <p>If none was set, the wildcard address -- which is what the JDK returns when the system has
     * not chosen an interface yet.
     */
    public InetAddress getInterface() throws SocketException {
        this.checkOpenSocket();
        if (this.ifAddress != null) {
            return this.ifAddress;
        }
        try {
            return InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
        } catch (UnknownHostException e) {
            throw new SocketException("no interface");
        }
    }

    /**
     * Which interface the multicast datagrams go out through.
     *
     * <p>It is the good version of {@link #setInterface}: an interface may have several addresses,
     * and in IPv6 the interface is the only way of naming the link.
     *
     * @throws SocketException if the socket is closed
     */
    public void setNetworkInterface(NetworkInterface netIf) throws SocketException {
        this.checkOpenSocket();
        if (netIf == null) {
            throw new SocketException("Invalid value");
        }
        this.netIf = netIf;
    }

    /**
     * The interface set with {@link #setNetworkInterface}.
     *
     * @throws SocketException if none was set. The JDK returns a placeholder interface there
     *     representing "whichever the system chooses"; here there is no system to choose, and
     *     manufacturing a fake interface to return would be inventing a fact. The exception is
     *     checked and the contract already declares it.
     */
    public NetworkInterface getNetworkInterface() throws SocketException {
        this.checkOpenSocket();
        if (this.netIf == null) {
            throw new SocketException("There is no multicast interface set");
        }
        return this.netIf;
    }

    /**
     * Whether local reception of one's own multicast datagrams is DISABLED.
     *
     * <p>Mind the sense, which is inverted and is the JDK's: {@code true} means "do not send them
     * back to me". That inversion is precisely why the method was deprecated.
     *
     * @deprecated use {@code setOption(StandardSocketOptions.IP_MULTICAST_LOOP, ...)}, which reads
     *     the right way round.
     */
    @Deprecated
    public void setLoopbackMode(boolean disable) throws SocketException {
        this.checkOpenSocket();
        this.loopbackDisabled = disable;
    }

    /**
     * Whether local reception is disabled.
     *
     * @deprecated see {@link #setLoopbackMode}.
     */
    @Deprecated
    public boolean getLoopbackMode() throws SocketException {
        this.checkOpenSocket();
        return this.loopbackDisabled;
    }

    private void checkOpenSocket() throws SocketException {
        if (this.isClosed()) {
            throw new SocketException("Socket is closed");
        }
    }

    private void checkOpenIO() throws IOException {
        if (this.isClosed()) {
            throw new SocketException("Socket is closed");
        }
    }

    /**
     * Joins the multicast group {@code mcastaddr} through the interface configured on this socket.
     *
     * <p>It is {@link DatagramSocket#joinGroup(SocketAddress, NetworkInterface)} with whatever
     * interface was set with {@link #setNetworkInterface} --or the one the system chooses if none was
     * set-- which is what the JDK documents.
     *
     * @throws IOException if the group could not be joined
     * @throws IllegalArgumentException if the address is not multicast
     * @deprecated as in the JDK: use {@link DatagramSocket#joinGroup(SocketAddress,
     *     NetworkInterface)}, which says through which interface
     */
    @Deprecated
    public void joinGroup(InetAddress mcastaddr) throws IOException {
        this.joinGroup(new InetSocketAddress(mcastaddr, 0), this.netIf);
    }

    /**
     * Leaves the multicast group {@code mcastaddr}. See {@link #joinGroup(InetAddress)}.
     *
     * @throws IOException if the group could not be left
     * @deprecated as in the JDK
     */
    @Deprecated
    public void leaveGroup(InetAddress mcastaddr) throws IOException {
        this.leaveGroup(new InetSocketAddress(mcastaddr, 0), this.netIf);
    }

    /**
     * Sends that datagram with that TTL, without changing the socket's TTL.
     *
     * <p>The TTL is set before sending and restored afterwards, which is what the JDK does: the
     * contract says the socket's TTL is left as it was.
     *
     * @throws IOException if the datagram could not be sent
     * @deprecated as in the JDK: use {@link #setTimeToLive} and {@link DatagramSocket#send}
     */
    @Deprecated
    public void send(DatagramPacket p, byte ttl) throws IOException {
        this.checkOpenIO();
        int previous = this.timeToLive;
        // The `& 0xFF` is not cosmetic: the parameter is a signed `byte` and a TTL of 200 arrives as
        // -56. The JDK treats it as unsigned, and without this a high TTL would be negative.
        this.setTimeToLive(ttl & 0xFF);
        try {
            this.send(p);
        } finally {
            this.setTimeToLive(previous);
        }
    }
}
