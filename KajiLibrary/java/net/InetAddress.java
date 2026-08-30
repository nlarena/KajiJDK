package java.net;

import java.io.IOException;
import java.io.Serializable;

// KajiLibrary's java.net.InetAddress -- an IP address. KajiJDK has no network stack and no name
// resolver, so this is an IPv4-only, resolution-free model: it holds the four address bytes and an
// optional host name, computes every address-category predicate from the bytes, and the factories
// accept literals / raw bytes but never perform DNS (getByName of a non-literal raises
// UnknownHostException; getLocalHost / getLoopbackAddress answer the loopback address).
//
// A KajiLibrary subset: isReachable(NetworkInterface, int, int) is OMITTED, since KajiJDK does not
// model java.net.NetworkInterface. (There is no network to reach in any case -- isReachable(int)
// answers false.)
public class InetAddress implements Serializable {

    // The four bytes of the IPv4 address, most-significant first.
    private final byte[] addr;

    // The host name this address was created with, or null if it is anonymous.
    private final String hostName;

    // Package-private: instances come from the factories, never a public constructor (as in the JDK,
    // whose instances are Inet4Address / Inet6Address).
    InetAddress(String hostName, byte[] addr) {
        this.hostName = hostName;
        this.addr = addr;
    }

    private int b(int i) {
        return this.addr[i] & 0xff;
    }

    /** Whether this is a multicast address (224.0.0.0 through 239.255.255.255). */
    public boolean isMulticastAddress() {
        return (this.addr[0] & 0xf0) == 0xe0;
    }

    /** Whether this is the wildcard address (0.0.0.0). */
    public boolean isAnyLocalAddress() {
        return this.b(0) == 0 && this.b(1) == 0 && this.b(2) == 0 && this.b(3) == 0;
    }

    /** Whether this is a loopback address (127.0.0.0/8). */
    public boolean isLoopbackAddress() {
        return this.b(0) == 127;
    }

    /** Whether this is a link-local address (169.254.0.0/16). */
    public boolean isLinkLocalAddress() {
        return this.b(0) == 169 && this.b(1) == 254;
    }

    /** Whether this is a site-local address (10/8, 172.16/12, 192.168/16). */
    public boolean isSiteLocalAddress() {
        return this.b(0) == 10
                || (this.b(0) == 172 && this.b(1) >= 16 && this.b(1) <= 31)
                || (this.b(0) == 192 && this.b(1) == 168);
    }

    /** Whether this is a globally-scoped multicast address. */
    public boolean isMCGlobal() {
        return this.b(0) >= 224 && this.b(0) <= 238
                && !(this.b(0) == 224 && this.b(1) == 0 && this.b(2) == 0);
    }

    /** Whether this is a node-local multicast address (never, for IPv4). */
    public boolean isMCNodeLocal() {
        return false;
    }

    /** Whether this is a link-local multicast address (224.0.0.0/24). */
    public boolean isMCLinkLocal() {
        return this.b(0) == 224 && this.b(1) == 0 && this.b(2) == 0;
    }

    /** Whether this is a site-local multicast address (239.255.0.0/16). */
    public boolean isMCSiteLocal() {
        return this.b(0) == 239 && this.b(1) == 255;
    }

    /** Whether this is an organization-local multicast address (239.192.0.0/14). */
    public boolean isMCOrgLocal() {
        return this.b(0) == 239 && this.b(1) >= 192 && this.b(1) <= 195;
    }

    /** Whether the address is reachable. Always false: KajiJDK has no network. */
    public boolean isReachable(int timeout) throws IOException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        return false;
    }

    /** The host name for this address, or its textual form when none was given. */
    public String getHostName() {
        if (this.hostName != null) {
            return this.hostName;
        }
        return this.getHostAddress();
    }

    /** The fully-qualified host name. KajiJDK does no reverse lookup, so this is {@link #getHostName()}. */
    public String getCanonicalHostName() {
        return this.getHostName();
    }

    /** A copy of the raw address bytes. */
    public byte[] getAddress() {
        byte[] copy = new byte[this.addr.length];
        int i = 0;
        while (i < this.addr.length) {
            copy[i] = this.addr[i];
            i = i + 1;
        }
        return copy;
    }

    /** The address in dotted-decimal form. */
    public String getHostAddress() {
        return this.b(0) + "." + this.b(1) + "." + this.b(2) + "." + this.b(3);
    }

    public int hashCode() {
        return (this.b(0) << 24) | (this.b(1) << 16) | (this.b(2) << 8) | this.b(3);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof InetAddress)) {
            return false;
        }
        InetAddress other = (InetAddress) obj;
        if (other.addr.length != this.addr.length) {
            return false;
        }
        int i = 0;
        while (i < this.addr.length) {
            if (this.addr[i] != other.addr[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public String toString() {
        String host = (this.hostName != null) ? this.hostName : "";
        return host + "/" + this.getHostAddress();
    }

    // ---- factories ----

    /** An {@code InetAddress} for the given host name and raw address, without any lookup. */
    public static InetAddress getByAddress(String host, byte[] addr) throws UnknownHostException {
        if (addr == null || addr.length != 4) {
            throw new UnknownHostException("addr is of illegal length (KajiJDK models IPv4 only)");
        }
        byte[] copy = new byte[4];
        int i = 0;
        while (i < 4) {
            copy[i] = addr[i];
            i = i + 1;
        }
        return new InetAddress(host, copy);
    }

    /** An anonymous {@code InetAddress} for the given raw address. */
    public static InetAddress getByAddress(byte[] addr) throws UnknownHostException {
        return getByAddress(null, addr);
    }

    /** The address for {@code host}: a literal is parsed; anything else has no resolver here. */
    public static InetAddress getByName(String host) throws UnknownHostException {
        if (host == null || host.length() == 0) {
            return getLoopbackAddress();
        }
        if (host.equals("localhost")) {
            return new InetAddress("localhost", new byte[] {127, 0, 0, 1});
        }
        byte[] parsed = parseV4(host);
        if (parsed != null) {
            return new InetAddress(host, parsed);
        }
        throw new UnknownHostException(host);
    }

    /** All addresses for {@code host}. KajiJDK returns at most one (no resolver). */
    public static InetAddress[] getAllByName(String host) throws UnknownHostException {
        return new InetAddress[] {getByName(host)};
    }

    /** The loopback address (127.0.0.1). */
    public static InetAddress getLoopbackAddress() {
        return new InetAddress("localhost", new byte[] {127, 0, 0, 1});
    }

    /** Parses an IP address literal, throwing {@link IllegalArgumentException} if it is not one. */
    public static InetAddress ofLiteral(String s) {
        byte[] parsed = parseV4(s);
        if (parsed == null) {
            throw new IllegalArgumentException("Not an IP address literal: " + s);
        }
        return new InetAddress(null, parsed);
    }

    /** The local host. KajiJDK has no host identity, so this is the loopback address. */
    public static InetAddress getLocalHost() throws UnknownHostException {
        return getLoopbackAddress();
    }

    // Parse a dotted-decimal IPv4 literal into four bytes, or null if it is not one.
    private static byte[] parseV4(String s) {
        if (s == null) {
            return null;
        }
        byte[] out = new byte[4];
        int part = 0;
        int val = 0;
        int digits = 0;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '.') {
                if (digits == 0 || part == 3) {
                    return null;
                }
                out[part] = (byte) val;
                part = part + 1;
                val = 0;
                digits = 0;
            } else if (c >= '0' && c <= '9') {
                val = val * 10 + (c - '0');
                digits = digits + 1;
                if (val > 255 || digits > 3) {
                    return null;
                }
            } else {
                return null;
            }
            i = i + 1;
        }
        if (part != 3 || digits == 0) {
            return null;
        }
        out[3] = (byte) val;
        return out;
    }
}
