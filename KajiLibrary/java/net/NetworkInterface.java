package java.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;

// A network interface of this machine: its name, its index, and the addresses set on it.
//
// ===========================================================================================
// WHAT THIS CLASS CAN AND CANNOT DO IN KajiJDK, AND WHY IT IS HERE ALL THE SAME
// ===========================================================================================
//
// Enumerating a machine's interfaces is an operating-system call --`getifaddrs` on POSIX,
// `GetAdaptersAddresses` on Windows-- and this VM exposes neither. There is no way to find out which
// interfaces there are, what they are called, or what addresses they have.
//
// So the question is what to do with the four methods that **look interfaces up**
// (`getNetworkInterfaces`, `getByName`, `getByIndex`, `getByInetAddress`). There are three ways out
// and two of them are lies:
//
//  - Returning an **empty** list (or null in the ones that look one up) would be asserting something
//    false: "this machine has no network interfaces". We do not know that, and it is almost certainly
//    untrue.
//  - Inventing a loopback interface --"lo", index 1, 127.0.0.1-- would be worse: plausible and false
//    data, which is the one kind of data nobody is going to go and verify.
//  - **Throwing `SocketException`**, which is what is done.
//
// The third is not a dodge: `SocketException` is a **checked** exception these four methods already
// declare in the JDK, precisely for the "could not find out" case. The compiler **forces** the caller
// to write the `catch`, so there is no way this can surprise anyone in production: it shows at compile
// time, which is when it is best found out. It is the same shape `URL.openStream()` already uses for
// a scheme it does not know how to open.
//
// The same holds for `isUp`, `isLoopback`, `isPointToPoint`, `supportsMulticast`,
// `getHardwareAddress` and `getMTU`: they all declare `SocketException` and they all query the
// operating system interface by interface.
//
// What **is** real and complete are the state accessors --`getName`, `getIndex`, `getDisplayName`,
// `getInetAddresses`, `getInterfaceAddresses`, `getParent`, `isVirtual`, `equals`, `hashCode`,
// `toString`--: they read fields and compare, and they operate exactly as in the JDK over any
// instance that exists.
//
// The practical effect is that the type **can be named**, which is what is needed for the signatures
// mentioning it to compile --`StandardSocketOptions.IP_MULTICAST_IF`,
// `MulticastSocket.setNetworkInterface`, `InetSocketAddress`'s scope id-- without any of them
// promising data that is not there.
public final class NetworkInterface {

    private final String name;
    private final String displayName;
    private final int index;
    private final List<InetAddress> addrs;
    private final List<InterfaceAddress> bindings;
    private final NetworkInterface parent;
    private final boolean virtual;

    // Package-private, as in the JDK: interfaces are manufactured by the platform, not by the user.
    // That being so is what guarantees that a `NetworkInterface` that exists describes an interface
    // that exists.
    NetworkInterface() {
        this("", -1, new InetAddress[0]);
    }

    NetworkInterface(String name, int index, InetAddress[] addrs) {
        this.name = name;
        this.displayName = name;
        this.index = index;
        List<InetAddress> l = new ArrayList<InetAddress>();
        int i = 0;
        while (addrs != null && i < addrs.length) {
            l.add(addrs[i]);
            i = i + 1;
        }
        this.addrs = Collections.unmodifiableList(l);
        this.bindings = Collections.unmodifiableList(new ArrayList<InterfaceAddress>());
        this.parent = null;
        this.virtual = false;
    }

    /** The name the operating system knows it by ("eth0", "lo"). */
    public String getName() {
        return this.name;
    }

    /**
     * The name to show a person.
     *
     * <p>On Windows it is usually a long phrase; on Unix, the same as {@link #getName}.
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * The interface's index, or -1 if the system does not give one.
     *
     * <p>It is the number identifying the interface in the IPv6 APIs --a link-local address's "scope
     * id" is this number-- and that is why it is worth more than the name for those uses.
     */
    public int getIndex() {
        return this.index;
    }

    /** The IP addresses set on this interface. */
    public Enumeration<InetAddress> getInetAddresses() {
        return Collections.enumeration(this.addrs);
    }

    /** The same as {@link #getInetAddresses}, as a stream. */
    public Stream<InetAddress> inetAddresses() {
        return this.addrs.stream();
    }

    /**
     * The addresses **with their mask and their broadcast**, which is more than
     * {@link #getInetAddresses} says.
     */
    public List<InterfaceAddress> getInterfaceAddresses() {
        return this.bindings;
    }

    /** The sub-interfaces (aliases, VLANs) hanging off this one. */
    public Enumeration<NetworkInterface> getSubInterfaces() {
        return Collections.enumeration(new ArrayList<NetworkInterface>());
    }

    /** The same as {@link #getSubInterfaces}, as a stream. */
    public Stream<NetworkInterface> subInterfaces() {
        return new ArrayList<NetworkInterface>().stream();
    }

    /** The interface this one is a sub-interface of, or null if it is a physical one. */
    public NetworkInterface getParent() {
        return this.parent;
    }

    /** Whether it is a sub-interface and not a real interface. */
    public boolean isVirtual() {
        return this.virtual;
    }

    // ---- what needs the operating system ----

    private static SocketException noPlatform() {
        return new SocketException(
                "this VM does not expose the operating system's network interface enumeration");
    }

    /**
     * Every interface on the machine.
     *
     * @throws SocketException always in KajiJDK; see the file's header. Returning an empty list would
     *     be asserting that the machine has no interfaces, which is a false assertion; this is "I
     *     could not find out", which is the truth and the one the contract already allows for.
     */
    public static Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
        throw noPlatform();
    }

    /**
     * The same as {@link #getNetworkInterfaces}, as a stream.
     *
     * @throws SocketException always in KajiJDK
     */
    public static Stream<NetworkInterface> networkInterfaces() throws SocketException {
        throw noPlatform();
    }

    /**
     * The interface called {@code name}.
     *
     * @throws SocketException always in KajiJDK. It does **not** return null: null means "that
     *     interface does not exist", and we do not know that.
     * @throws NullPointerException if {@code name} is null
     */
    public static NetworkInterface getByName(String name) throws SocketException {
        if (name == null) {
            throw new NullPointerException();
        }
        throw noPlatform();
    }

    /**
     * The interface with that index.
     *
     * @throws SocketException always in KajiJDK
     * @throws IllegalArgumentException if the index is negative
     */
    public static NetworkInterface getByIndex(int index) throws SocketException {
        if (index < 0) {
            throw new IllegalArgumentException("Interface index can't be negative");
        }
        throw noPlatform();
    }

    /**
     * The interface that has that address set on it.
     *
     * @throws SocketException always in KajiJDK
     * @throws NullPointerException if {@code addr} is null
     */
    public static NetworkInterface getByInetAddress(InetAddress addr) throws SocketException {
        if (addr == null) {
            throw new NullPointerException();
        }
        throw noPlatform();
    }

    /**
     * Whether the interface is up.
     *
     * @throws SocketException always in KajiJDK: an interface's state is asked of the operating
     *     system every time, it is not a field of the object
     */
    public boolean isUp() throws SocketException {
        throw noPlatform();
    }

    /**
     * Whether it is the loopback interface.
     *
     * @throws SocketException always in KajiJDK
     */
    public boolean isLoopback() throws SocketException {
        throw noPlatform();
    }

    /**
     * Whether it is a point-to-point link (a tunnel, a PPP).
     *
     * @throws SocketException always in KajiJDK
     */
    public boolean isPointToPoint() throws SocketException {
        throw noPlatform();
    }

    /**
     * Whether the interface does multicast.
     *
     * @throws SocketException always in KajiJDK
     */
    public boolean supportsMulticast() throws SocketException {
        throw noPlatform();
    }

    /**
     * The interface's physical (MAC) address.
     *
     * @throws SocketException always in KajiJDK
     */
    public byte[] getHardwareAddress() throws SocketException {
        throw noPlatform();
    }

    /**
     * The interface's maximum packet size.
     *
     * @throws SocketException always in KajiJDK
     */
    public int getMTU() throws SocketException {
        throw noPlatform();
    }

    // ---- identity ----

    /**
     * Same name and same addresses.
     *
     * <p>The index does **not** enter the comparison, and that is the JDK's doing: the index is
     * assigned by the system and may change between boots, so two objects describing the same
     * interface could carry different indices.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof NetworkInterface)) {
            return false;
        }
        NetworkInterface that = (NetworkInterface) obj;
        if (this.name == null) {
            if (that.name != null) {
                return false;
            }
        } else if (!this.name.equals(that.name)) {
            return false;
        }
        return this.addrs.equals(that.addrs);
    }

    @Override
    public int hashCode() {
        return this.name == null ? 0 : this.name.hashCode();
    }

    /** {@code name:displayName (addr1 addr2 ...)}, the same format as the JDK. */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("name:").append(this.name == null ? "null" : this.name);
        if (this.displayName != null) {
            b.append(" (").append(this.displayName).append(')');
        }
        return b.toString();
    }
}
