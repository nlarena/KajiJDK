package java.net;

// A datagram: a block of bytes with a destination, or with a sender.
//
// ===========================================================================================
// IT IS THE ENVELOPE, NOT THE POST
// ===========================================================================================
//
// `DatagramPacket` **neither sends nor receives anything**. It holds a buffer, a stretch inside that
// buffer, and an address with a port. Sending it is `DatagramSocket`'s job. When this note was first
// written there was no `DatagramSocket` in KajiJDK --there were no network natives-- and the point
// was that the envelope existing without the post is not an empty promise: every member here is an
// accessor over a field or a range check, and every one does exactly what it says. There is a
// `DatagramSocket` now, so the datagram can also be delivered.
//
// ===========================================================================================
// THE DETAIL THAT MATTERS: `length` IS BOTH INPUT AND OUTPUT
// ===========================================================================================
//
// On **receiving**, `length` goes in meaning how much room there is and comes out meaning how many
// bytes arrived. That is why reusing a packet for several receptions is a classic mistake: after
// receiving 10 bytes into a 1024-byte buffer, `length` is left at 10, and the next reception
// truncates to 10 bytes without warning. The cure is `setLength(buf.length)` before each reception.
//
// It is documented here because it is the part of the contract the signature does not show.
//
// All eighteen members are here; nothing omitted.
public final class DatagramPacket {

    private byte[] buf;
    private int offset;
    private int length;

    // How much was asked for in the last `setLength`/`setData`. On receiving, `length` is overwritten
    // with what arrived, and without this field there would be no telling how much actually fitted.
    private int bufLength;

    private InetAddress address;

    // It starts at 0 and not at -1. It looks like a detail and it is not: a freshly built packet with
    // no destination reports port 0, which is what JDK 25 does --the old versions put -1-- and it is
    // what makes `getSocketAddress()` of a packet with no destination return the wildcard address
    // with port 0 instead of throwing over an invalid port.
    private int port;

    /**
     * A packet using {@code length} bytes of {@code buf} starting at {@code offset}.
     *
     * <p>With no address: it serves to **receive**, or to be sent after being given one.
     *
     * @throws IllegalArgumentException if the stretch does not fit the buffer
     * @throws NullPointerException     if {@code buf} is null
     */
    public DatagramPacket(byte[] buf, int offset, int length) {
        setData(buf, offset, length);
    }

    /**
     * A packet using the first {@code length} bytes of {@code buf}.
     *
     * @throws IllegalArgumentException if {@code length} does not fit the buffer
     */
    public DatagramPacket(byte[] buf, int length) {
        this(buf, 0, length);
    }

    /**
     * A packet ready to be sent to {@code address}:{@code port}.
     *
     * @throws IllegalArgumentException if the stretch does not fit, or the port is outside 0..65535
     */
    public DatagramPacket(byte[] buf, int offset, int length, InetAddress address, int port) {
        setData(buf, offset, length);
        setAddress(address);
        setPort(port);
    }

    /**
     * A packet ready to be sent to {@code address}, given as a socket address.
     *
     * @throws IllegalArgumentException if {@code address} is not a resolved
     *                                  {@link InetSocketAddress}, or the stretch does not fit
     */
    public DatagramPacket(byte[] buf, int offset, int length, SocketAddress address) {
        setData(buf, offset, length);
        setSocketAddress(address);
    }

    /**
     * A packet ready to be sent, using the first {@code length} bytes.
     *
     * @throws IllegalArgumentException if the stretch does not fit, or the port is out of range
     */
    public DatagramPacket(byte[] buf, int length, InetAddress address, int port) {
        this(buf, 0, length, address, port);
    }

    /**
     * A packet ready to be sent, using the first {@code length} bytes.
     *
     * @throws IllegalArgumentException if {@code address} is not a resolved
     *                                  {@link InetSocketAddress}, or the stretch does not fit
     */
    public DatagramPacket(byte[] buf, int length, SocketAddress address) {
        this(buf, 0, length, address);
    }

    /** Who it goes to, or who it came from; null if none was set. */
    public synchronized InetAddress getAddress() {
        return this.address;
    }

    /** The destination or origin port; 0 if none was set. */
    public synchronized int getPort() {
        return this.port;
    }

    /**
     * The buffer, **uncopied**.
     *
     * <p>That it is not copied comes from the JDK's contract and it is what makes reusing a packet
     * cheap: whoever receives writes straight onto this array. It also means that modifying it
     * changes the packet, which is exactly what it is there for.
     */
    public synchronized byte[] getData() {
        return this.buf;
    }

    /** Where the data starts inside the buffer. */
    public synchronized int getOffset() {
        return this.offset;
    }

    /** How many bytes count. See the header: on receiving, this changes. */
    public synchronized int getLength() {
        return this.length;
    }

    /**
     * Changes the buffer and the stretch.
     *
     * @throws NullPointerException     if {@code buf} is null
     * @throws IllegalArgumentException if the stretch does not fit the buffer
     */
    public synchronized void setData(byte[] buf, int offset, int length) {
        if (buf == null) {
            throw new NullPointerException("null packet buffer");
        }
        // The sum is done and compared this way so that an overflow cannot turn it round: with
        // `offset + length` in int, two enormous values give a negative and would pass the check.
        if (offset < 0 || length < 0 || offset > buf.length - length) {
            throw new IllegalArgumentException("illegal length or offset");
        }
        this.buf = buf;
        this.offset = offset;
        this.length = length;
        this.bufLength = length;
    }

    /** Where to send it; null leaves it with no destination. */
    public synchronized void setAddress(InetAddress iaddr) {
        this.address = iaddr;
    }

    /**
     * Which port to send it to.
     *
     * @throws IllegalArgumentException if it is outside 0..65535
     */
    public synchronized void setPort(int iport) {
        if (iport < 0 || iport > 0xFFFF) {
            throw new IllegalArgumentException("Port out of range:" + iport);
        }
        this.port = iport;
    }

    /**
     * Destination and port in one go.
     *
     * @throws IllegalArgumentException if it is not an {@link InetSocketAddress}, or if it is an
     *                                  unresolved one -- sending to a name nobody resolved is not an
     *                                  operation that can be completed
     */
    public synchronized void setSocketAddress(SocketAddress address) {
        if (address == null || !(address instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("unsupported address type");
        }
        InetSocketAddress addr = (InetSocketAddress) address;
        if (addr.isUnresolved()) {
            throw new IllegalArgumentException("unresolved address");
        }
        setAddress(addr.getAddress());
        setPort(addr.getPort());
    }

    /** Destination and port together. */
    public synchronized SocketAddress getSocketAddress() {
        return new InetSocketAddress(getAddress(), getPort());
    }

    /**
     * Changes the buffer, keeping the stretch if it still fits.
     *
     * @throws NullPointerException     if {@code buf} is null
     * @throws IllegalArgumentException if the stretch it already had does not fit the new buffer
     */
    public synchronized void setData(byte[] buf) {
        if (buf == null) {
            throw new NullPointerException("null packet buffer");
        }
        this.buf = buf;
        this.offset = 0;
        this.length = buf.length;
        this.bufLength = buf.length;
    }

    /**
     * How many bytes count.
     *
     * <p>Before reusing a packet to receive, this method has to be called with the buffer's size; see
     * the header.
     *
     * @throws IllegalArgumentException if it does not fit the buffer from the current offset
     */
    public synchronized void setLength(int length) {
        if (length < 0 || this.offset > this.buf.length - length) {
            throw new IllegalArgumentException("illegal length");
        }
        this.length = length;
        this.bufLength = length;
    }
}
