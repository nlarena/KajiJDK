package java.net;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

// An IP address.
//
// ===========================================================================================
// WHAT IS HERE AND WHAT IS NOT, AND WHY
// ===========================================================================================
//
// An IP address is two things people confuse all the time: **a number with a format** and **a name
// that has to be resolved**. The first half is byte arithmetic and literal grammars -- RFC 791 and
// RFC 4291 -- and it can be written whole here without touching the network. The second needs a
// resolver, which is to say DNS, which is to say sockets, and KajiJDK has none.
//
// So: **the parsing and the formatting are complete and faithful**; resolution does not exist and
// is not simulated. `getByName` of a literal returns the address; of a name other than "localhost"
// it throws `UnknownHostException`, which is exactly what the JDK does when DNS does not answer.
// That is not a lie: it is the honest result of having no resolver, and the exception's type was
// already in the contract.
//
// ===========================================================================================
// `java.net.IDN`, WHICH USED TO BE MISSING AND WHOSE PLACE THIS IS
// ===========================================================================================
//
// `IDN` turns an internationalized host name into one that can be resolved --which is what this
// class does with the result-- so its absence used to be recorded here.
//
// `toASCII` **is not** Punycode. It is *nameprep* (RFC 3491) and **then** Punycode, and the first
// step is the one that cannot be written: full case folding, NFKC normalization, and the tables of
// forbidden characters. It is not an academic detail, it shows in a two-word example: the JDK turns
// `strasse.de` and `stra{eszett}.de` **into the same string**, because full folding sends the
// eszett to `ss`. A `toLowerCase` does not do that, and the version that used it would give two
// different names for the same domain -- which in a resolver is exactly the error that is not
// forgiven.
//
// It is the same wall that leaves `java.text.Normalizer` without NFKC: Unicode's tables are needed,
// and this tree does not have them. `IDN` is here now, written with the folding and the checks that
// can be done without tables, and it says in its own header which step it is not doing and what
// that changes. Punycode alone, which can be written exactly, is the part that is exact.

// `isReachable(int)` **really probes**: a TCP to port 7 where a refusal counts as an answer,
// because the RST is sent by the host. It is the JDK's own fallback path when it cannot send an
// ICMP, which is the normal case --a raw ping needs permissions an ordinary process does not have.
// It used to answer `false` always, which was legal but useless; it stopped being so when the VM
// learnt TCP.
//
// **The only observable thing separating it from the JDK is the time**, and it is worth knowing
// before choosing a deadline: on Windows the system takes some two seconds to report a TCP refusal,
// so a live host with the port closed needs a deadline of at least that to give `true`. The JDK,
// when it can, sends an ICMP and answers on the spot. The answer is the same; what changes is how
// long it has to be waited for.
//
// The class is concrete and has a package-private constructor, as in the JDK: it is never
// instantiated directly, every instance is an `Inet4Address` or an `Inet6Address`. The methods here
// are the neutral values the subclasses override -- the same structure as the JDK, and for the same
// reason: the common type has to be nameable in the signatures without committing to a family.
public class InetAddress implements Serializable {

    private static final long serialVersionUID = 3286316764910316507L;

    // The address's bytes: four for IPv4, sixteen for IPv6. In the base class it is null, because
    // the base represents no concrete address.
    final byte[] addr;

    // The name it was created with, or null if it is anonymous. **null is not the same as ""**: an
    // address born from a literal has no name, and `toString` prints it as "/1.2.3.4". Confusing
    // the two was the bug in the previous version of this file.
    final String hostName;

    InetAddress(String hostName, byte[] addr) {
        this.hostName = hostName;
        this.addr = addr;
    }

    /** Whether it is a multicast address. */
    public boolean isMulticastAddress() {
        return false;
    }

    /** Whether it is the wildcard address ("any of the local ones"). */
    public boolean isAnyLocalAddress() {
        return false;
    }

    /** Whether it is a loopback address. */
    public boolean isLoopbackAddress() {
        return false;
    }

    /** Whether it is link-local (valid only within the physical link). */
    public boolean isLinkLocalAddress() {
        return false;
    }

    /** Whether it is site-local (the "private" range). */
    public boolean isSiteLocalAddress() {
        return false;
    }

    /** Whether it is multicast of global scope. */
    public boolean isMCGlobal() {
        return false;
    }

    /** Whether it is multicast of node scope. */
    public boolean isMCNodeLocal() {
        return false;
    }

    /** Whether it is multicast of link scope. */
    public boolean isMCLinkLocal() {
        return false;
    }

    /** Whether it is multicast of site scope. */
    public boolean isMCSiteLocal() {
        return false;
    }

    /** Whether it is multicast of organization scope. */
    public boolean isMCOrgLocal() {
        return false;
    }

    /**
     * Whether the host is reachable within {@code timeout} milliseconds.
     *
     * <p>It really probes: see the file's header for what the probe is and how it differs from the
     * JDK's in timing. A `false` says "it did not answer", not "the host does not exist".
     *
     * @throws IllegalArgumentException if {@code timeout} is negative
     */
    public boolean isReachable(int timeout) throws IOException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        if (this.addr == null) {
            // The base class represents no concrete address: there is nobody to ask.
            return false;
        }
        // Zero means "no limit" in the contract. No real wait can be infinite here, so it is
        // translated into a long, explicit deadline instead of hanging the thread.
        return this.probe("", 0, timeout);
    }

    /**
     * Whether this host answers within {@code timeout} milliseconds, probing **through that
     * interface** and with that hop limit.
     *
     * <p>With a null {@code netif} it goes out wherever the system likes and with a {@code ttl} of
     * zero it uses the default limit, which is what the JDK documents; in that case it is identical
     * to {@link #isReachable(int)}.
     *
     * <p>**With an interface or with a TTL the probe asserts less**, and that has to be said: the
     * one-parameter version takes a refusal as an answer --an RST proves the host is alive-- and
     * this one, which has to build the socket by hand in order to choose the interface, does not
     * tell a refusal from silence. A `true` still means "it answered"; a `false` with an interface
     * may be a live host that refused the connection. It is the only difference between the two,
     * and it cannot be avoided without reimplementing each system's non-blocking `connect`.
     *
     * @param netif the interface the probe goes out through, or null
     * @param ttl the hop limit, or zero
     * @param timeout milliseconds; zero is "no limit"
     * @throws IllegalArgumentException if the deadline or the ttl are negative
     */
    public boolean isReachable(NetworkInterface netif, int ttl, int timeout) throws IOException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        if (ttl < 0) {
            throw new IllegalArgumentException("ttl can't be negative");
        }
        if (this.addr == null) {
            return false;
        }
        return this.probe(InetAddress.outboundFrom(netif, this), ttl, timeout);
    }

    // Which local address a probe that has to go through that interface goes out from: the first of
    // the interface's that is of the same family as the destination --binding an IPv4 end to an
    // IPv6 connection is not a request that can be fulfilled. The empty string means "let the
    // system choose".
    private static String outboundFrom(NetworkInterface netif, InetAddress target) {
        if (netif == null) {
            return "";
        }
        boolean isSix = target instanceof Inet6Address;
        java.util.Enumeration<InetAddress> addresses = netif.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress d = addresses.nextElement();
            if (isSix == (d instanceof Inet6Address)) {
                return d.getHostAddress();
            }
        }
        // An interface with no address of the destination's family cannot carry the probe. Letting
        // the system choose is more useful than failing, and it is what the JDK does.
        return "";
    }

    // The common body of the two overloads. The waiting is on this side because the native does not
    // wait: see `jdk.internal.net.Net`'s header.
    private boolean probe(String local, int ttl, int timeout) throws IOException {
        // Zero means "no limit" in the contract. No real wait can be infinite here, so it is
        // translated into a long, explicit deadline instead of hanging the thread.
        long deadline = timeout == 0 ? 30_000L : timeout;
        int probeId = jdk.internal.net.Net.reachableStart(this.getHostAddress(), local, ttl);
        if (probeId < 0) {
            return false;
        }
        try {
            long started = System.currentTimeMillis();
            int r = jdk.internal.net.Net.answerPoll(probeId);
            while (r == -3) {
                if (System.currentTimeMillis() - started >= deadline) {
                    // No answer within the deadline is exactly what this method calls "not
                    // reachable": it does not assert that the host does not exist, it asserts that
                    // it did not answer.
                    return false;
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new java.io.InterruptedIOException("isReachable interrupted");
                }
                r = jdk.internal.net.Net.answerPoll(probeId);
            }
            return r == 1;
        } finally {
            jdk.internal.net.Net.answerFree(probeId);
        }
    }

    /** The name it was created with; failing that, its textual form. */
    public String getHostName() {
        if (this.hostName != null) {
            return this.hostName;
        }
        return this.getHostAddress();
    }

    /** The fully qualified name. With no reverse resolution, it is {@link #getHostName()}. */
    public String getCanonicalHostName() {
        return this.getHostName();
    }

    /** A copy of the raw bytes. */
    public byte[] getAddress() {
        return null;
    }

    /** The address in textual form. */
    public String getHostAddress() {
        return null;
    }

    public int hashCode() {
        return 0;
    }

    public boolean equals(Object obj) {
        return false;
    }

    public String toString() {
        return Objects.toString(this.hostName, "") + "/" + this.getHostAddress();
    }

    // ---- factorias ------------------------------------------------------------------------------

    /**
     * The address for {@code host} and the bytes {@code addr}, asking nobody.
     *
     * <p>Four bytes give an {@link Inet4Address}; sixteen give an {@link Inet6Address}, unless they
     * are the "IPv4-mapped" form (::ffff:a.b.c.d), which collapses to an `Inet4Address` -- that
     * address **is** an IPv4, written with IPv6's syntax, and treating it as v6 would make two
     * objects naming the same host unequal.
     *
     * @throws UnknownHostException if {@code addr} is neither 4 nor 16 long
     */
    public static InetAddress getByAddress(String host, byte[] addr) throws UnknownHostException {
        if (host != null && host.length() > 0 && host.charAt(0) == '[') {
            if (host.charAt(host.length() - 1) == ']') {
                host = host.substring(1, host.length() - 1);
            }
        }
        if (addr == null) {
            throw new UnknownHostException("addr is of illegal length");
        }
        if (addr.length == Inet4Address.INADDRSZ) {
            return new Inet4Address(host, copy(addr));
        }
        if (addr.length == Inet6Address.INADDRSZ) {
            byte[] v4 = Inet6Address.convertFromIPv4MappedAddress(addr);
            if (v4 != null) {
                return new Inet4Address(host, v4);
            }
            return new Inet6Address(host, copy(addr));
        }
        throw new UnknownHostException("addr is of illegal length");
    }

    /** The anonymous address for those bytes. */
    public static InetAddress getByAddress(byte[] addr) throws UnknownHostException {
        return getByAddress(null, addr);
    }

    /**
     * The address of {@code host}.
     *
     * <p>An IPv4 or IPv6 literal is parsed; "localhost" and the empty string give the loopback. Any
     * other name would need a resolver, and there is none: it throws {@link UnknownHostException},
     * which is the same as the JDK returns when DNS does not know.
     */
    public static InetAddress getByName(String host) throws UnknownHostException {
        return getAllByName(host)[0];
    }

    /**
     * Every address of {@code host}. With no resolver there is at most one, so the array always has
     * a single element (or nothing gets returned at all).
     */
    public static InetAddress[] getAllByName(String host) throws UnknownHostException {
        if (host == null || host.length() == 0) {
            return new InetAddress[] {getLoopbackAddress()};
        }
        boolean bracketed = false;
        if (host.charAt(0) == '[') {
            if (host.length() > 2 && host.charAt(host.length() - 1) == ']') {
                host = host.substring(1, host.length() - 1);
                bracketed = true;
            } else {
                throw new UnknownHostException(host + ": invalid IPv6 address literal");
            }
        }
        // It is only tried as a literal if it starts the way one could start. Without this filter,
        // "beef.example" would go into the IPv4 parser and come out the same side, but the filter
        // is also what keeps a name starting with a non-hex letter from even being tried.
        if (host.length() > 0 && (digit(host.charAt(0), 16) != -1 || host.charAt(0) == ':')) {
            InetAddress parsed = null;
            if (!bracketed) {
                byte[] v4 = Inet4Address.textToNumericFormat(host);
                if (v4 != null) {
                    parsed = new Inet4Address(null, v4);
                }
            }
            if (parsed == null) {
                parsed = Inet6Address.parseLiteral(host, false);
            }
            if (parsed != null) {
                return new InetAddress[] {parsed};
            }
        }
        if (host.equalsIgnoreCase("localhost")) {
            return new InetAddress[] {getLoopbackAddress()};
        }
        throw new UnknownHostException(host);
    }

    /** The loopback: 127.0.0.1, named "localhost". */
    public static InetAddress getLoopbackAddress() {
        return new Inet4Address("localhost", new byte[] {127, 0, 0, 1});
    }

    /**
     * The address {@code s} describes, which has to be an IPv4 or IPv6 literal.
     *
     * <p>Unlike {@link #getByName}, this admits no names: if it is not a literal, there is nothing
     * to consult and it fails on the spot.
     *
     * @throws IllegalArgumentException if it is not a valid literal
     */
    public static InetAddress ofLiteral(String s) {
        Objects.requireNonNull(s);
        byte[] v4 = Inet4Address.textToNumericFormat(s);
        if (v4 != null) {
            return new Inet4Address(null, v4);
        }
        InetAddress v6 = Inet6Address.parseLiteral(s, true);
        if (v6 == null) {
            throw invalidLiteral(s);
        }
        return v6;
    }

    /** The local host. KajiJDK has no network identity, so it is the loopback. */
    public static InetAddress getLocalHost() throws UnknownHostException {
        return getLoopbackAddress();
    }

    // ---- utilities shared by the subclasses -----------------------------------------------------

    static IllegalArgumentException invalidLiteral(String s) {
        return new IllegalArgumentException("Invalid IP address literal: " + s);
    }

    static byte[] copy(byte[] src) {
        byte[] out = new byte[src.length];
        int i = 0;
        while (i < src.length) {
            out[i] = src[i];
            i = i + 1;
        }
        return out;
    }

    // `Character.digit` accepts digits from all of Unicode; for an IP literal that would be a hole
    // (Arabic-Indic digits are not digits of an address), so it is restricted to ASCII.
    static int digit(char c, int radix) {
        int v = -1;
        if (c >= '0' && c <= '9') {
            v = c - '0';
        } else if (c >= 'a' && c <= 'z') {
            v = c - 'a' + 10;
        } else if (c >= 'A' && c <= 'Z') {
            v = c - 'A' + 10;
        }
        if (v < 0 || v >= radix) {
            return -1;
        }
        return v;
    }
}
