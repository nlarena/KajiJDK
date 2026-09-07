package java.net;

// An IP address plus a port -- or, when the name could not be resolved, a name plus a port.
//
// That "or" is the whole class. An `InetSocketAddress` can be in two states and the difference is
// visible in the API (`isUnresolved`, and `getAddress()` returning null) because **it is useful that
// it should be**: a proxy's or a destination's address can be assembled without DNS at hand, passed
// around, and resolved later or on another machine. That is why `createUnresolved` is not a degraded
// case but a first-class factory.
//
// In KajiJDK that stops being a detail: with no resolver, `new InetSocketAddress(name, port)` ends up
// in the unresolved state for anything that is not an IP literal. There is nothing to hide there --
// it is exactly the state the JDK produces when DNS does not answer, with the same observables.
//
// The whole class is pure computation: it stores, validates and formats. Nothing is omitted.
public class InetSocketAddress extends SocketAddress {

    private static final long serialVersionUID = 5076001401234631237L;

    // Exactly one of these two rules: if `addr` is not null, the address is resolved and `hostname`
    // is null; if it is null, it is unresolved and `hostname` holds the name.
    private final String hostname;
    private final InetAddress addr;
    private final int port;

    private InetSocketAddress(String hostname, InetAddress addr, int port) {
        this.hostname = hostname;
        this.addr = addr;
        this.port = port;
    }

    /**
     * The wildcard (0.0.0.0) on that port: "any local address".
     *
     * @throws IllegalArgumentException if the port is outside 0..65535
     */
    public InetSocketAddress(int port) {
        this(checkPort(port), (InetAddress) null);
    }

    /**
     * That address on that port. A null {@code addr} means the wildcard.
     *
     * @throws IllegalArgumentException if the port is outside 0..65535
     */
    public InetSocketAddress(InetAddress addr, int port) {
        this(checkPort(port), addr);
    }

    private InetSocketAddress(int port, InetAddress addr) {
        this.hostname = null;
        this.addr = (addr == null) ? new Inet4Address() : addr;
        this.port = port;
    }

    /**
     * Tries to resolve {@code hostname}; if it cannot, it is left unresolved with that name.
     *
     * <p>That it does not throw when resolution fails comes from the contract: the object is still
     * usable and whoever receives it decides what to do with an unresolved destination.
     *
     * @throws IllegalArgumentException if the port is out of range or {@code hostname} is null
     */
    public InetSocketAddress(String hostname, int port) {
        checkPort(port);
        checkHost(hostname);
        InetAddress a = null;
        String h = null;
        try {
            a = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            h = hostname;
        }
        this.hostname = h;
        this.addr = a;
        this.port = port;
    }

    /** An unresolved address, without even trying to resolve it. */
    public static InetSocketAddress createUnresolved(String host, int port) {
        checkPort(port);
        checkHost(host);
        return new InetSocketAddress(host, null, port);
    }

    private static int checkPort(int port) {
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("port out of range:" + port);
        }
        return port;
    }

    private static String checkHost(String hostname) {
        if (hostname == null) {
            throw new IllegalArgumentException("hostname can't be null");
        }
        return hostname;
    }

    public final int getPort() {
        return this.port;
    }

    /** The address, or null if it is unresolved. */
    public final InetAddress getAddress() {
        return this.addr;
    }

    /** The host's name: the one asked for if it is unresolved, the address's if it is not. */
    public final String getHostName() {
        if (this.hostname != null) {
            return this.hostname;
        }
        if (this.addr != null) {
            return this.addr.getHostName();
        }
        return null;
    }

    /**
     * Like {@link #getHostName()}, but **without** triggering a reverse lookup.
     *
     * <p>The difference only shows in the real JDK, where `getHostName()` may go out to the network;
     * here the two do the same. The method belongs all the same: whoever writes portable code needs
     * to be able to say "the name you already have, do not go looking for another".
     */
    public final String getHostString() {
        if (this.hostname != null) {
            return this.hostname;
        }
        if (this.addr.hostName != null) {
            return this.addr.hostName;
        }
        return this.addr.getHostAddress();
    }

    public final boolean isUnresolved() {
        return this.addr == null;
    }

    public String toString() {
        if (this.isUnresolved()) {
            return this.hostname + "/<unresolved>:" + this.port;
        }
        String s = this.addr.toString();
        // The brackets round an IPv6's numeric part, without which "::1:80" would be ambiguous.
        if (this.addr instanceof Inet6Address) {
            int i = s.lastIndexOf('/');
            s = s.substring(0, i + 1) + "[" + s.substring(i + 1) + "]";
        }
        return s + ":" + this.port;
    }

    // Two unresolved ones are equal if the name (case-insensitively) and the port match; two
    // resolved ones, if the address and the port match. A resolved one is never equal to an
    // unresolved one, even if the name points at that address: it is not known, precisely because it
    // was not resolved.
    public final boolean equals(Object obj) {
        if (!(obj instanceof InetSocketAddress)) {
            return false;
        }
        InetSocketAddress that = (InetSocketAddress) obj;
        boolean sameIP;
        if (this.addr != null) {
            sameIP = this.addr.equals(that.addr);
        } else if (this.hostname != null) {
            sameIP = (that.addr == null) && this.hostname.equalsIgnoreCase(that.hostname);
        } else {
            sameIP = (that.addr == null) && (that.hostname == null);
        }
        return sameIP && (this.port == that.port);
    }

    public final int hashCode() {
        if (this.addr != null) {
            return this.addr.hashCode() + this.port;
        }
        if (this.hostname != null) {
            return this.hostname.hashCode() + this.port;
        }
        return this.port;
    }
}
