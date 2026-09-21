package java.nio.channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This library's {@link DatagramChannel}, over the UDP seam of the VM.
 *
 * <h2>Receiving without waiting</h2>
 *
 * <p>{@link #receive} returns `null` in non-blocking mode when nothing arrived, which is the
 * contract, and it is exactly what the seam answers already --a -3--. In blocking mode it insists
 * with a short `Thread.sleep`: sleeping releases the interpreter, so the thread that waits for a
 * packet does not stop the one that is going to send it from advancing.
 *
 * <h2>The sender and the datagram are a single datum</h2>
 *
 * <p>The seam hands them over in three calls --receive, and then ask who it came from-- because a
 * native that returns an integer cannot return both. That is why {@link #receive} is
 * `synchronized`: without the lock, two threads receiving over the same channel could take each
 * other's sender. It is the same reason and the same lock as in `java.net.DatagramSocket`.
 *
 * <h2>Connecting, which in UDP sends nothing</h2>
 *
 * <p>{@link #connect} fixes who is talked to so that the rest is filtered. It is a local decision:
 * there is no handshake. Here the filter is applied by this class --whatever comes from another is
 * discarded-- because the seam has no way of asking the system for it, and the observable result is
 * the one the contract promises.
 */
final class KajiDatagramChannel extends DatagramChannel {

    /** The socket of the VM, or -1 if it has not been tied yet. */
    private int handle = -1;

    private InetSocketAddress peer = null;

    private int ttl = 1;

    private final List<KajiMembershipKey> memberships = new ArrayList<KajiMembershipKey>();

    private static final Set<SocketOption<?>> OPTIONS;

    static {
        Set<SocketOption<?>> s = new HashSet<SocketOption<?>>();
        s.add(StandardSocketOptions.IP_MULTICAST_TTL);
        OPTIONS = Collections.unmodifiableSet(s);
    }

    KajiDatagramChannel(SelectorProvider provider) {
        super(provider);
    }

    private void requireOpen() throws ClosedChannelException {
        if (!this.isOpen()) {
            throw new ClosedChannelException();
        }
    }

    // Tying without anybody asking for it: sending or receiving over an untied channel ties it,
    // which is what the JDK does. Without this, a channel that only sends could never send.
    private void ensureBound() throws IOException {
        if (this.handle < 0) {
            this.bind(null);
        }
    }

    // ---- binding --------------------------------------------------------------------------------

    public DatagramChannel bind(SocketAddress local) throws IOException {
        this.requireOpen();
        if (this.handle >= 0) {
            throw new AlreadyBoundException();
        }
        String host = "0.0.0.0";
        int port = 0;
        if (local != null) {
            if (!(local instanceof InetSocketAddress)) {
                throw new UnsupportedAddressTypeException();
            }
            InetSocketAddress d = (InetSocketAddress) local;
            if (d.isUnresolved()) {
                throw new UnresolvedAddressException();
            }
            if (d.getAddress() != null && !d.getAddress().isAnyLocalAddress()) {
                host = d.getAddress().getHostAddress();
            }
            port = d.getPort();
        }
        int h = jdk.internal.net.Net.udpBind(host, port);
        if (h < 0) {
            throw new java.net.BindException("Cannot bind: " + host + ":" + port);
        }
        this.handle = h;
        return this;
    }

    public SocketAddress getLocalAddress() throws IOException {
        this.requireOpen();
        if (this.handle < 0) {
            return null;
        }
        String d = jdk.internal.net.Net.localAddress(this.handle);
        int p = jdk.internal.net.Net.localPort(this.handle);
        if (d == null || p < 0) {
            return null;
        }
        return new InetSocketAddress(InetAddress.getByName(d), p);
    }

    public java.net.DatagramSocket socket() {
        return (java.net.DatagramSocket) jdk.internal.net.Adoption.datagram(this.handle);
    }

    // ---- connecting ----------------------------------------------------------------------------

    public boolean isConnected() {
        return this.peer != null;
    }

    public DatagramChannel connect(SocketAddress remote) throws IOException {
        this.requireOpen();
        if (remote == null) {
            throw new IllegalArgumentException("address is null");
        }
        if (!(remote instanceof InetSocketAddress)) {
            throw new UnsupportedAddressTypeException();
        }
        InetSocketAddress d = (InetSocketAddress) remote;
        if (d.isUnresolved()) {
            throw new UnresolvedAddressException();
        }
        this.ensureBound();
        this.peer = d;
        return this;
    }

    public DatagramChannel disconnect() throws IOException {
        this.requireOpen();
        this.peer = null;
        return this;
    }

    public SocketAddress getRemoteAddress() throws IOException {
        this.requireOpen();
        return this.peer;
    }

    // ---- moving datagrams --------------------------------------------------------------------

    public synchronized SocketAddress receive(ByteBuffer dst) throws IOException {
        this.requireOpen();
        if (dst == null) {
            throw new NullPointerException("dst");
        }
        this.ensureBound();
        int count = dst.remaining();
        byte[] buf = new byte[count];
        while (true) {
            int n = jdk.internal.net.Net.udpReceive(this.handle, buf, 0, count);
            while (n == -3 && this.isBlocking()) {
                if (!this.isOpen()) {
                    throw new AsynchronousCloseException();
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new java.io.InterruptedIOException("receive interrupted");
                }
                n = jdk.internal.net.Net.udpReceive(this.handle, buf, 0, count);
            }
            if (n == -3) {
                return null;
            }
            if (n < 0) {
                throw new IOException("receive failed");
            }
            String dir = jdk.internal.net.Net.udpSenderAddress(this.handle);
            int port = jdk.internal.net.Net.udpSenderPort(this.handle);
            InetSocketAddress src_ = dir == null
                    ? null : new InetSocketAddress(InetAddress.getByName(dir), port);
            if (this.peer != null && !this.peer.equals(src_)) {
                // Connected channel: whatever does not come from the peer is discarded **without handing it
                // over**. In non-blocking mode an answer has to come now --it cannot go round and round-- and
                // that is why the `return null`: nothing arrived that this channel should see.
                if (!this.isBlocking()) {
                    return null;
                }
                continue;
            }
            dst.put(buf, 0, n);
            return src_;
        }
    }

    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        this.requireOpen();
        if (src == null) {
            throw new NullPointerException("src");
        }
        if (!(target instanceof InetSocketAddress)) {
            throw new UnsupportedAddressTypeException();
        }
        InetSocketAddress d = (InetSocketAddress) target;
        if (d.isUnresolved()) {
            throw new UnresolvedAddressException();
        }
        if (this.peer != null && !this.peer.equals(d)) {
            throw new IllegalArgumentException("Connected address not equal to target address");
        }
        this.ensureBound();
        int count = src.remaining();
        byte[] buf = new byte[count];
        src.get(buf, 0, count);
        if (!jdk.internal.net.Net.udpSend(this.handle, d.getAddress().getHostAddress(), d.getPort(),
                buf, 0, count)) {
            throw new IOException("send failed");
        }
        return count;
    }

    // ---- the stream forms, which demand being connected --------------------------------------

    public int read(ByteBuffer dst) throws IOException {
        if (this.peer == null) {
            throw new NotYetConnectedException();
        }
        SocketAddress src_ = this.receive(dst);
        return src_ == null ? 0 : dst.position();
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        KajiSocketChannel.requireRange(dsts, offset, length);
        if (this.peer == null) {
            throw new NotYetConnectedException();
        }
        // A datagram goes into **one** buffer: it is not split among several, because the limit of
        // the message is part of the datum. The first one with room takes it whole.
        for (int i = 0; i < length; i++) {
            if (dsts[offset + i].remaining() > 0) {
                return this.read(dsts[offset + i]);
            }
        }
        return 0;
    }

    public int write(ByteBuffer src) throws IOException {
        if (this.peer == null) {
            throw new NotYetConnectedException();
        }
        return this.send(src, this.peer);
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        KajiSocketChannel.requireRange(srcs, offset, length);
        if (this.peer == null) {
            throw new NotYetConnectedException();
        }
        // The buffers are gathered into **a single** datagram: writing one per buffer would send
        // several messages where the caller asked for one, and on the other side that shows.
        int total = 0;
        for (int i = 0; i < length; i++) {
            total += srcs[offset + i].remaining();
        }
        byte[] buf = new byte[total];
        int pos = 0;
        for (int i = 0; i < length; i++) {
            ByteBuffer b = srcs[offset + i];
            int n = b.remaining();
            b.get(buf, pos, n);
            pos += n;
        }
        this.ensureBound();
        if (!jdk.internal.net.Net.udpSend(this.handle, this.peer.getAddress().getHostAddress(),
                this.peer.getPort(), buf, 0, total)) {
            throw new IOException("send failed");
        }
        return total;
    }

    // ---- multidifusion -----------------------------------------------------------------------

    public MembershipKey join(InetAddress group, NetworkInterface interf) throws IOException {
        return this.joinGroup(group, interf, null);
    }

    public MembershipKey join(InetAddress group, NetworkInterface interf, InetAddress source)
            throws IOException {
        if (source == null) {
            throw new NullPointerException("source");
        }
        // A membership by sender has to be sustained by the system, and the seam of this VM does not
        // ask for it. Saying so is what the contract foresees for this case, and it is useful
        // information: it means "this stack does not filter by sender", which is not the same as "it
        // could not be done".
        throw new UnsupportedOperationException("source-specific multicast not supported");
    }

    private MembershipKey joinGroup(InetAddress group, NetworkInterface interf, InetAddress source)
            throws IOException {
        this.requireOpen();
        if (group == null) {
            throw new NullPointerException("group");
        }
        if (!group.isMulticastAddress()) {
            throw new IllegalArgumentException("Group not a multicast address");
        }
        this.ensureBound();
        for (KajiMembershipKey k : this.memberships) {
            if (k.isValid() && k.group().equals(group) && k.sameCard(interf)) {
                throw new IllegalStateException("Already a member of the group");
            }
        }
        String card = KajiDatagramChannel.nameCard(group, interf);
        if (!jdk.internal.net.Net.udpJoin(this.handle, group.getHostAddress(), card)) {
            throw new IOException("join group failed: " + group);
        }
        KajiMembershipKey k = new KajiMembershipKey(this, group, interf, source, card);
        this.memberships.add(k);
        return k;
    }

    // It drops the membership. It is called by the key, which is the one that has the contract of
    // `drop()`.
    void release(KajiMembershipKey k) {
        if (this.handle >= 0) {
            jdk.internal.net.Net.udpLeave(this.handle, k.group().getHostAddress(), k.card());
        }
    }

    // In IPv4 the card is named by address and in IPv6 by index: they are two different strings.
    // The empty one means "the one the system chooses".
    static String nameCard(InetAddress group, NetworkInterface interf) {
        if (interf == null) {
            return "";
        }
        if (group instanceof java.net.Inet6Address) {
            return Integer.toString(interf.getIndex());
        }
        java.util.Enumeration<InetAddress> dirs = interf.getInetAddresses();
        while (dirs.hasMoreElements()) {
            InetAddress d = dirs.nextElement();
            if (d instanceof java.net.Inet4Address) {
                return d.getHostAddress();
            }
        }
        return "";
    }

    // ---- options ----------------------------------------------------------------------------

    public <T> DatagramChannel setOption(SocketOption<T> name, T value) throws IOException {
        this.requireOpen();
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!StandardSocketOptions.IP_MULTICAST_TTL.equals(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        int v = ((Integer) value).intValue();
        if (v < 0 || v > 255) {
            throw new IllegalArgumentException("Invalid TTL: " + v);
        }
        this.ensureBound();
        this.ttl = v;
        jdk.internal.net.Net.udpSetTtl(this.handle, v);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOption(SocketOption<T> name) throws IOException {
        this.requireOpen();
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!StandardSocketOptions.IP_MULTICAST_TTL.equals(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        return (T) Integer.valueOf(this.ttl);
    }

    public Set<SocketOption<?>> supportedOptions() {
        return OPTIONS;
    }

    // ---- closing ------------------------------------------------------------------------------

    protected void implCloseSelectableChannel() throws IOException {
        // Closing drops every membership, which is what `MulticastChannel.close()` promises.
        for (KajiMembershipKey k : this.memberships) {
            k.invalidateKey();
        }
        this.memberships.clear();
        if (this.handle >= 0) {
            jdk.internal.net.Net.close(this.handle);
            this.handle = -1;
        }
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        // Nothing to say to the system; see the note of `KajiSocketChannel`.
    }

    /**
     * The VM handle of the datagram socket underneath, or -1 when there is none open.
     *
     * <p>Package-private and only for the selector: it is the one thing `poll` needs and the one
     * thing no public method of a channel hands out.
     *
     * @return the handle
     */
    int pollHandle() {
        return this.handle;
    }
}
