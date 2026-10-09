package java.net;

// One address of a network interface, with its mask and its broadcast.
//
// There is no way to build one by hand, and that is correct rather than a papered-over gap: in the
// JDK there is none either -- the constructor is package-private, and the only source is
// `NetworkInterface.getInterfaceAddresses()`. `NetworkInterface` was missing from this tree for a
// long time (enumerating the machine's interfaces is an operating-system call the VM does not expose;
// the full why is in `InetAddress`'s header), and while it was, this class was unreachable. The
// earlier note said that the day `NetworkInterface` arrived this class would not change a line: it
// arrived, and it did not.
//
// What is **not** done is giving it a public constructor the JDK does not have so that it can be
// "used". That would be inventing API: it would change the contract to cover up another class's
// absence.
//
// The prefix length is a `short` and not an `int` because it does not go past 128 (IPv6) or 32
// (IPv4); the JDK fixes the type.
public final class InterfaceAddress {

    private final InetAddress address;
    private final Inet4Address broadcast;
    private final short maskLength;

    // Package-private, as in the JDK: the factory is `NetworkInterface`, and there is no other.
    InterfaceAddress(InetAddress address, Inet4Address broadcast, short maskLength) {
        this.address = address;
        this.broadcast = broadcast;
        this.maskLength = maskLength;
    }

    /** This interface's IP address. */
    public InetAddress getAddress() {
        return this.address;
    }

    /**
     * This subnet's broadcast address, or null.
     *
     * <p>Null for IPv6 **always**, and not for want of data: IPv6 has no broadcast, it uses multicast
     * instead. That is why the declared return type is `InetAddress` but the value can only be an
     * `Inet4Address`.
     */
    public InetAddress getBroadcast() {
        return this.broadcast;
    }

    /** How many bits of the address are the network: 24 for a 255.255.255.0 mask. */
    public short getNetworkPrefixLength() {
        return this.maskLength;
    }

    /** Equal if the three parts match. */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof InterfaceAddress)) {
            return false;
        }
        InterfaceAddress other = (InterfaceAddress) obj;
        if (!same(this.address, other.address)) {
            return false;
        }
        if (!same(this.broadcast, other.broadcast)) {
            return false;
        }
        return this.maskLength == other.maskLength;
    }

    private static boolean same(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    @Override
    public int hashCode() {
        int h = this.maskLength;
        if (this.address != null) {
            h = h + this.address.hashCode();
        }
        if (this.broadcast != null) {
            h = h + this.broadcast.hashCode();
        }
        return h;
    }

    /** In the form "address/prefix [broadcast]", which is the JDK's. */
    @Override
    public String toString() {
        return this.address + "/" + this.maskLength + " [" + this.broadcast + "]";
    }
}
