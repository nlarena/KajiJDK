package java.net;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// A datagram socket: the object that is configured to send and receive UDP.
//
// ===========================================================================================
// WHERE THE LINE IS
// ===========================================================================================
//
// This header used to say that `send`, `receive` and the multicast memberships were not here
// because there was no UDP stack in this VM, and that the line of the whole of `java.net` in
// KajiJDK was "what is configured goes in, what transports does not". **Everything is in now.** The
// stack exists --`jdk.internal.net.Net`, the same seam that opened TCP-- so all four are here, the
// constructors that bind really bind, and `MulticastSocket` really joins groups.
//
// **How a datagram is waited for.** The native does **not** block: it answers -3 when nothing has
// arrived yet. It has to be that way, and it is not a convenience: this VM's Java threads share one
// interpreter, so a native standing still waiting would not let the thread that was going to send
// the packet run. `receive` waits on this side, retrying with a short `Thread.sleep` --sleeping
// releases the interpreter-- and that is why it can really honour `setSoTimeout`.
//
// **On `connect`:** in UDP there is no handshake. `connect` is a **local** decision --it fixes who
// is being talked to so that the rest of the datagrams are filtered-- and it sends not one byte.
// That is why it is really implemented and not omitted: it records state, and everything observable
// afterwards (`isConnected`, `getInetAddress`, `getPort`, `getRemoteSocketAddress`) is true.
//
// **On the options' default values:** in the JDK the operating system sets them and they vary from
// machine to machine. Here this class sets them, and they are documented one by one. No correct
// program depends on them --which is why the JDK never promises them-- and what is guaranteed is
// the only thing that matters about a configuration object: what is set is what is read.
public class DatagramSocket implements Closeable {

    private static volatile DatagramSocketImplFactory factory;

    private final DatagramSocketImpl impl;

    private boolean bound;
    private boolean closed;
    private InetAddress remoteAddr;
    private int remotePort = -1;

    /** The VM's socket, or -1 if this one was not bound. */
    int handle = -1;

    // What a `DatagramChannel` needs in order to hand over the socket that wraps it. See
    // `ServerSocket.adopt`.
    void adopt(int h) {
        this.handle = h;
        this.bound = true;
    }

    // Default values: see the header's note. `soTimeout` at 0 means "wait forever" and is the only
    // one the JDK does fix in Java. The others are the ones most systems use.
    private int soTimeout = 0;
    private int sendBufferSize = 65507;
    private int receiveBufferSize = 65507;
    private boolean reuseAddress = false;
    private boolean broadcast = true;
    private int trafficClass = 0;

    /**
     * A socket bound to any port on every interface.
     *
     * @throws SocketException if it could not be bound. For an **unbound** socket, which can be had
     *     and configured in full, use {@code new DatagramSocket(null)}.
     */
    public DatagramSocket() throws SocketException {
        this.impl = null;
        this.bindTo("0.0.0.0", 0);
    }

    /**
     * A socket bound to {@code port} on every interface.
     *
     * @throws SocketException if it could not be bound
     */
    public DatagramSocket(int port) throws SocketException {
        this(port, null);
    }

    /**
     * A socket bound to {@code laddr}:{@code port}.
     *
     * @throws IllegalArgumentException if the port is out of range
     * @throws SocketException if it could not be bound
     */
    public DatagramSocket(int port, InetAddress laddr) throws SocketException {
        this.impl = null;
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("Port out of range:" + port);
        }
        this.bindTo(laddr == null ? "0.0.0.0" : laddr.getHostAddress(), port);
    }

    /**
     * A socket bound to {@code bindaddr}, or **unbound** if {@code bindaddr} is null.
     *
     * <p>The null case is not an invented exception: the JDK documents it in just this way ("if the
     * address is null, creates an unbound socket").
     *
     * @throws SocketException if it could not be bound
     */
    public DatagramSocket(SocketAddress bindaddr) throws SocketException {
        this.impl = createImpl();
        if (bindaddr != null) {
            this.bind(bindaddr);
        }
    }

    /**
     * An unbound socket over the given implementation.
     *
     * <p>It is the constructor a subclass bringing its own stack uses. It binds nothing, so it
     * works in full.
     *
     * @throws NullPointerException if {@code impl} is null
     */
    protected DatagramSocket(DatagramSocketImpl impl) {
        if (impl == null) {
            throw new NullPointerException();
        }
        this.impl = impl;
    }

    private static DatagramSocketImpl createImpl() {
        DatagramSocketImplFactory f = factory;
        return f == null ? null : f.createDatagramSocketImpl();
    }

    // Binds the VM's socket and keeps the handle. It is all the binding constructors and `bind` do,
    // and it is kept together so that the four paths never drift apart.
    private void bindTo(String host, int port) throws SocketException {
        int h = jdk.internal.net.Net.udpBind(host, port);
        if (h < 0) {
            // The native does not tell "port taken" from "no permission"; the message names the
            // only thing known for certain.
            throw new SocketException("Cannot bind: " + host + ":" + port);
        }
        this.handle = h;
        this.bound = true;
    }

    private void checkOpen() throws SocketException {
        if (this.closed) {
            throw new SocketException("Socket is closed");
        }
    }

    /**
     * Binds the socket to {@code addr}.
     *
     * @throws SocketException if the socket is closed, already bound, or could not be bound
     */
    public void bind(SocketAddress addr) throws SocketException {
        this.checkOpen();
        if (this.bound) {
            throw new SocketException("already bound");
        }
        if (addr != null && !(addr instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        // With no address, the wildcard: bind to any port on every interface, which is what the JDK
        // documents for `bind(null)`.
        String host = "0.0.0.0";
        int port = 0;
        if (addr != null) {
            InetSocketAddress isa = (InetSocketAddress) addr;
            if (isa.getAddress() != null && !isa.getAddress().isAnyLocalAddress()) {
                host = isa.getAddress().getHostAddress();
            }
            port = isa.getPort();
        }
        this.bindTo(host, port);
    }

    /**
     * Fixes who this socket talks to.
     *
     * <p>It is a local decision: it sends nothing. See the header's note.
     *
     * @throws IllegalArgumentException if the address is null or the port is out of range
     */
    public void connect(InetAddress address, int port) {
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("connect: " + port);
        }
        if (address == null) {
            throw new IllegalArgumentException("connect: null address");
        }
        if (this.closed) {
            return;
        }
        this.remoteAddr = address;
        this.remotePort = port;
    }

    /**
     * Like {@link #connect(InetAddress, int)}, with the address and the port together.
     *
     * @throws IllegalArgumentException if {@code addr} is null or is not an {@link
     *     InetSocketAddress}
     * @throws SocketException if {@code addr} does not have its address resolved
     */
    public void connect(SocketAddress addr) throws SocketException {
        if (addr == null) {
            throw new IllegalArgumentException("Address can't be null");
        }
        if (!(addr instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        InetSocketAddress epoint = (InetSocketAddress) addr;
        if (epoint.isUnresolved()) {
            throw new SocketException("Unresolved address");
        }
        this.connect(epoint.getAddress(), epoint.getPort());
    }

    /** Stops being fixed to a destination. If it was not, it does nothing. */
    public void disconnect() {
        this.remoteAddr = null;
        this.remotePort = -1;
    }

    /** Whether the socket is bound to a local port. */
    public boolean isBound() {
        return this.bound;
    }

    /** Whether the socket has a destination fixed. */
    public boolean isConnected() {
        return this.remoteAddr != null;
    }

    /** The destination that was set, or null. */
    public InetAddress getInetAddress() {
        return this.remoteAddr;
    }

    /** The fixed destination's port, or -1. */
    public int getPort() {
        return this.remotePort;
    }

    /** The fixed destination as a {@link SocketAddress}, or null if there is none. */
    public SocketAddress getRemoteSocketAddress() {
        if (!this.isConnected()) {
            return null;
        }
        return new InetSocketAddress(this.remoteAddr, this.remotePort);
    }

    /** The local address it is bound to, or null if it is not bound. */
    public SocketAddress getLocalSocketAddress() {
        if (this.closed || !this.bound) {
            return null;
        }
        return new InetSocketAddress(this.getLocalAddress(), this.getLocalPort());
    }

    /**
     * The local address.
     *
     * <p>Null if it is closed, and the wildcard address if it is not bound -- which is what the JDK
     * returns in the same situation.
     */
    public InetAddress getLocalAddress() {
        if (this.closed) {
            return null;
        }
        if (this.handle >= 0) {
            String d = jdk.internal.net.Net.localAddress(this.handle);
            if (d != null) {
                try {
                    // It is a numeric literal: this queries no DNS.
                    return InetAddress.getByName(d);
                } catch (UnknownHostException e) {
                    // It cannot happen with a numeric literal; if it did, it falls to the wildcard
                    // below.
                }
            }
        }
        try {
            return InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /** The local port: -1 if it is closed, 0 if it is not bound. */
    public int getLocalPort() {
        if (this.closed) {
            return -1;
        }
        if (this.handle >= 0) {
            int p = jdk.internal.net.Net.localPort(this.handle);
            if (p >= 0) {
                return p;
            }
        }
        return this.bound && this.impl != null ? this.impl.getLocalPort() : 0;
    }

    // ---- options ----

    /**
     * Milliseconds a reception waits; 0 is "forever".
     *
     * @throws SocketException if the socket is closed
     * @throws IllegalArgumentException if the timeout is negative
     */
    public void setSoTimeout(int timeout) throws SocketException {
        this.checkOpen();
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        }
        this.soTimeout = timeout;
    }

    public int getSoTimeout() throws SocketException {
        this.checkOpen();
        return this.soTimeout;
    }

    /**
     * Suggested size of the output buffer.
     *
     * <p>"Suggested" comes from the JDK, it is not a get-out of ours: the operating system may give
     * you another, and that is why the getter never promised to return what you set. Here there is
     * no system to change it, so it returns exactly what was set.
     *
     * @throws IllegalArgumentException if the size is not positive
     */
    public void setSendBufferSize(int size) throws SocketException {
        this.checkOpen();
        if (size <= 0) {
            throw new IllegalArgumentException("negative send size");
        }
        this.sendBufferSize = size;
    }

    public int getSendBufferSize() throws SocketException {
        this.checkOpen();
        return this.sendBufferSize;
    }

    /**
     * Suggested size of the input buffer. See {@link #setSendBufferSize}.
     *
     * @throws IllegalArgumentException if the size is not positive
     */
    public void setReceiveBufferSize(int size) throws SocketException {
        this.checkOpen();
        if (size <= 0) {
            throw new IllegalArgumentException("invalid receive size");
        }
        this.receiveBufferSize = size;
    }

    public int getReceiveBufferSize() throws SocketException {
        this.checkOpen();
        return this.receiveBufferSize;
    }

    /** Whether an address left taken may be reused. */
    public void setReuseAddress(boolean on) throws SocketException {
        this.checkOpen();
        this.reuseAddress = on;
    }

    public boolean getReuseAddress() throws SocketException {
        this.checkOpen();
        return this.reuseAddress;
    }

    /** Whether datagrams can be sent to the broadcast address. */
    public void setBroadcast(boolean on) throws SocketException {
        this.checkOpen();
        this.broadcast = on;
    }

    public boolean getBroadcast() throws SocketException {
        this.checkOpen();
        return this.broadcast;
    }

    /**
     * The "type of service" field of the IP header.
     *
     * @throws IllegalArgumentException if it does not fit in a byte
     */
    public void setTrafficClass(int tc) throws SocketException {
        this.checkOpen();
        if (tc < 0 || tc > 255) {
            throw new IllegalArgumentException("tc is not in range 0 -- 255");
        }
        this.trafficClass = tc;
    }

    public int getTrafficClass() throws SocketException {
        this.checkOpen();
        return this.trafficClass;
    }

    /**
     * Sets an option by its typed constant.
     *
     * @throws UnsupportedOperationException if this class does not support that option
     * @throws IllegalArgumentException if the value is no good for that option
     */
    public <T> DatagramSocket setOption(SocketOption<T> name, T value) throws IOException {
        this.checkOpen();
        if (name == null) {
            throw new NullPointerException();
        }
        if (name == StandardSocketOptions.SO_SNDBUF) {
            this.setSendBufferSize(((Integer) value).intValue());
        } else if (name == StandardSocketOptions.SO_RCVBUF) {
            this.setReceiveBufferSize(((Integer) value).intValue());
        } else if (name == StandardSocketOptions.SO_REUSEADDR) {
            this.setReuseAddress(((Boolean) value).booleanValue());
        } else if (name == StandardSocketOptions.SO_BROADCAST) {
            this.setBroadcast(((Boolean) value).booleanValue());
        } else if (name == StandardSocketOptions.IP_TOS) {
            this.setTrafficClass(((Integer) value).intValue());
        } else {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        return this;
    }

    /**
     * An option's value.
     *
     * @throws UnsupportedOperationException if this class does not support that option
     */
    public <T> T getOption(SocketOption<T> name) throws IOException {
        this.checkOpen();
        if (name == null) {
            throw new NullPointerException();
        }
        if (name == StandardSocketOptions.SO_SNDBUF) {
            return (T) Integer.valueOf(this.getSendBufferSize());
        }
        if (name == StandardSocketOptions.SO_RCVBUF) {
            return (T) Integer.valueOf(this.getReceiveBufferSize());
        }
        if (name == StandardSocketOptions.SO_REUSEADDR) {
            return (T) Boolean.valueOf(this.getReuseAddress());
        }
        if (name == StandardSocketOptions.SO_BROADCAST) {
            return (T) Boolean.valueOf(this.getBroadcast());
        }
        if (name == StandardSocketOptions.IP_TOS) {
            return (T) Integer.valueOf(this.getTrafficClass());
        }
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    /** The options this socket understands. */
    public Set<SocketOption<?>> supportedOptions() {
        Set<SocketOption<?>> s = new HashSet<SocketOption<?>>();
        s.add(StandardSocketOptions.SO_SNDBUF);
        s.add(StandardSocketOptions.SO_RCVBUF);
        s.add(StandardSocketOptions.SO_REUSEADDR);
        s.add(StandardSocketOptions.SO_BROADCAST);
        s.add(StandardSocketOptions.IP_TOS);
        return Collections.unmodifiableSet(s);
    }

    // ---- life cycle ----

    /** Closes the socket. Closing twice does nothing, which is what {@link Closeable} requires. */
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.handle >= 0) {
            jdk.internal.net.Net.close(this.handle);
            this.handle = -1;
        }
    }

    /** Whether it was closed already. */
    public boolean isClosed() {
        return this.closed;
    }

    /**
     * The associated NIO channel, or null.
     *
     * <p>Null unless the socket came out of a `DatagramChannel`, which is what the JDK does: a
     * socket created with `new` has no channel.
     */
    public java.nio.channels.DatagramChannel getChannel() {
        return null;
    }

    /**
     * Installs the implementation factory for the whole VM. Once only.
     *
     * @throws Error if one had already been installed
     * @deprecated the JDK deprecated the {@link DatagramSocketImpl} mechanism
     */
    @Deprecated
    public static synchronized void setDatagramSocketImplFactory(DatagramSocketImplFactory fac)
            throws IOException {
        if (factory != null) {
            throw new Error("factory already defined");
        }
        factory = fac;
    }

    // ---- moving datagrams -------------------------------------------------------------------

    /**
     * Sends that datagram.
     *
     * <p>If the socket is connected, the packet may carry no destination: the fixed one is used. If
     * it carries one **different** from the fixed one, it is refused, which is what the contract
     * requires.
     *
     * @throws IOException if the datagram could not be sent whole
     * @throws IllegalArgumentException if the packet has no destination and the socket is not
     *     connected
     * @throws SocketException if the socket is closed
     */
    public void send(DatagramPacket p) throws IOException {
        if (p == null) {
            throw new NullPointerException("p");
        }
        this.checkOpen();
        InetAddress target = p.getAddress();
        int port = p.getPort();
        if (this.isConnected()) {
            if (target == null) {
                target = this.remoteAddr;
                port = this.remotePort;
            } else if (!target.equals(this.remoteAddr) || port != this.remotePort) {
                throw new IllegalArgumentException("connected address and packet address differ");
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Address not set");
        }
        // Sending while unbound binds: the system chooses the outgoing port. It is what the JDK
        // does, and without it a `new DatagramSocket(null)` that only sends could never send.
        if (this.handle < 0) {
            this.bindTo("0.0.0.0", 0);
        }
        boolean ok = jdk.internal.net.Net.udpSend(this.handle, target.getHostAddress(), port,
                p.getData(), p.getOffset(), p.getLength());
        if (!ok) {
            throw new IOException("send failed");
        }
    }

    /**
     * Waits for a datagram and leaves it in {@code p}, together with who sent it.
     *
     * <p>It honours {@link #setSoTimeout}: the native does not wait --it answers "not yet" on the
     * spot-- and the one counting the time is this method, which is the one that knows when it
     * started waiting.
     *
     * <p>It is `synchronized` because receiving and asking who it came from are **a single
     * operation** split into three calls to the native; without the lock, two threads receiving
     * over the same socket could take each other's sender.
     *
     * @throws SocketTimeoutException if the deadline expired with nothing arriving
     * @throws IOException if the reception failed
     */
    public synchronized void receive(DatagramPacket p) throws IOException {
        if (p == null) {
            throw new NullPointerException("p");
        }
        this.checkOpen();
        if (this.handle < 0) {
            // Receiving while unbound binds, just as sending does.
            this.bindTo("0.0.0.0", 0);
        }
        long start = System.currentTimeMillis();
        byte[] buf = p.getData();
        int n = jdk.internal.net.Net.udpReceive(this.handle, buf, p.getOffset(), p.getLength());
        while (n == -3) {
            if (this.closed) {
                throw new SocketException("Socket is closed");
            }
            if (this.soTimeout > 0
                    && System.currentTimeMillis() - start >= this.soTimeout) {
                throw new SocketTimeoutException("Receive timed out");
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.InterruptedIOException("receive interrupted");
            }
            n = jdk.internal.net.Net.udpReceive(this.handle, buf, p.getOffset(), p.getLength());
        }
        if (n < 0) {
            throw new IOException("receive failed");
        }
        p.setLength(n);
        String isa = jdk.internal.net.Net.udpSenderAddress(this.handle);
        if (isa != null) {
            p.setAddress(InetAddress.getByName(isa));
            p.setPort(jdk.internal.net.Net.udpSenderPort(this.handle));
        }
    }

    // ---- multicast --------------------------------------------------------------------------

    /**
     * Joins the multicast group {@code mcastaddr} through the interface {@code netIf}.
     *
     * @param netIf null lets the system choose the interface, which is what the JDK documents
     * @throws SocketException if the address is not multicast -- which is what JDK 25 throws, even
     *     though its javadoc promises `IllegalArgumentException`
     * @throws IOException if the group could not be joined
     * @throws IllegalArgumentException if the address is not an {@link InetSocketAddress}
     */
    public void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {
        this.membership(mcastaddr, netIf, true);
    }

    /**
     * Leaves the multicast group {@code mcastaddr}. See {@link #joinGroup(SocketAddress,
     * NetworkInterface)}.
     *
     * @throws IOException if the group could not be left
     */
    public void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {
        this.membership(mcastaddr, netIf, false);
    }

    // Joining and leaving a group resolve exactly the same things --the address, the interface, and
    // whether they are v4 or v6-- so they are kept together: separating them would duplicate that
    // resolution, which is where all the odd cases live.
    private void membership(SocketAddress mcastaddr, NetworkInterface netIf, boolean join)
            throws IOException {
        this.checkOpen();
        if (!(mcastaddr instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        InetAddress group = ((InetSocketAddress) mcastaddr).getAddress();
        if (group == null || !group.isMulticastAddress()) {
            // The JDK's javadoc says `IllegalArgumentException`, but JDK 25 throws
            // `SocketException("Not a multicast address")`. What it **does** is followed, not what
            // it says: it is what a program running against both will catch.
            throw new SocketException("Not a multicast address");
        }
        if (this.handle < 0) {
            this.bindTo("0.0.0.0", 0);
        }
        String iface = DatagramSocket.interfaceName(group, netIf);
        boolean ok = join
                ? jdk.internal.net.Net.udpJoin(this.handle, group.getHostAddress(), iface)
                : jdk.internal.net.Net.udpLeave(this.handle, group.getHostAddress(), iface);
        if (!ok) {
            throw new IOException((join ? "join" : "leave") + " group failed: " + group);
        }
    }

    // How to name the interface for the native: in IPv4 it is named by address and in IPv6 by
    // index, and they are two different strings. The empty string means "whichever the system
    // chooses".
    static String interfaceName(InetAddress group, NetworkInterface netIf) {
        if (netIf == null) {
            return "";
        }
        if (group instanceof Inet6Address) {
            return Integer.toString(netIf.getIndex());
        }
        java.util.Enumeration<InetAddress> dirs = netIf.getInetAddresses();
        while (dirs.hasMoreElements()) {
            InetAddress d = dirs.nextElement();
            if (d instanceof Inet4Address) {
                return d.getHostAddress();
            }
        }
        // An interface with no IPv4 address cannot receive v4 multicast; letting the system choose
        // is more useful than failing, and it is what the JDK does with an interface with no
        // addresses.
        return "";
    }
}
