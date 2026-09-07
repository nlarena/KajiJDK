package java.net;

// An IPv6 address: sixteen bytes, and optionally a scope.
//
// The scope is the surprising part. A link-local address like fe80::1 **does not identify a host**:
// it identifies a host *on a link*, and the same address may exist on two different interfaces of the
// same machine. That is why the literal admits the "%N" suffix, and why the scope is part of the
// object but **not** of `equals`: two objects with the same address and different scopes are equal,
// because the address is the same; what changes is the way out. The JDK does exactly this and it is
// not an oversight.
//
// The scope is kept with a separate flag and not as "zero means none", because zero is a legal scope:
// `getByAddress(host, addr, 0)` produces an address that prints as "...%0", while `ofLiteral("::1")`
// prints nothing. Collapsing them would lose that difference.
//
// On the "IPv4-mapped" form (::ffff:a.b.c.d): that address **is** an IPv4, and both the parsers and
// `InetAddress.getByAddress` collapse it to an `Inet4Address`. It is what the JDK does, and the
// reason is that otherwise the same machine would have two different, unequal objects for the same
// address. The "IPv4-compatible" form (::a.b.c.d, without the ffff) is **not** collapsed: that one is
// a real IPv6, deprecated but distinct.
//
// The scope can be named in both the ways the JDK admits: by number
// (`getByAddress(String, byte[], int)`, `getScopeId()`) and by interface
// (`getByAddress(String, byte[], NetworkInterface)`, `getScopedInterface()`). This class used to say
// the second did not go in because `NetworkInterface` did not exist in this tree; it exists, and it
// does.
//
// The two forms are not interchangeable and that is why both are kept: an interface's index can be
// taken from the interface, but the interface **cannot** be taken from an index without enumerating
// again --and the JDK returns `null` from `getScopedInterface()` when the scope was given as a
// number, not the interface with that index.
public final class Inet6Address extends InetAddress {

    private static final long serialVersionUID = 6880410070516793377L;

    static final int INADDRSZ = 16;

    private final int scopeId;
    private final boolean scopeIdSet;

    // The interface it was created with, if it was created with one. `transient` because this class's
    // serialized form --the JDK's, which this tree respects-- carries the scope as a number and
    // nothing else: an interface cannot be reconstructed on another machine, and storing it would
    // change the format.
    private final transient NetworkInterface scopedInterface;

    Inet6Address(String hostName, byte[] addr) {
        super(hostName, addr);
        this.scopeId = 0;
        this.scopeIdSet = false;
        this.scopedInterface = null;
    }

    Inet6Address(String hostName, byte[] addr, int scopeId) {
        super(hostName, addr);
        // Un scope negativo se ignora en vez de rechazarse: es como el JDK distingue "no me pasaron
        // scope" de "me pasaron el scope cero".
        if (scopeId >= 0) {
            this.scopeId = scopeId;
            this.scopeIdSet = true;
        } else {
            this.scopeId = 0;
            this.scopeIdSet = false;
        }
        // A scope given as a number names no interface: see `getScopedInterface`.
        this.scopedInterface = null;
    }

    private int b(int i) {
        return this.addr[i] & 0xff;
    }

    /** ff00::/8. */
    public boolean isMulticastAddress() {
        return this.b(0) == 0xff;
    }

    /** La direccion sin especificar, "::". */
    public boolean isAnyLocalAddress() {
        int i = 0;
        while (i < INADDRSZ) {
            if (this.addr[i] != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** "::1", and only that one. */
    public boolean isLoopbackAddress() {
        int i = 0;
        while (i < 15) {
            if (this.addr[i] != 0) {
                return false;
            }
            i = i + 1;
        }
        return this.addr[15] == 1;
    }

    /** fe80::/10. */
    public boolean isLinkLocalAddress() {
        return this.b(0) == 0xfe && (this.b(1) & 0xc0) == 0x80;
    }

    /** fec0::/10 (deprecated, but the predicate still means the same). */
    public boolean isSiteLocalAddress() {
        return this.b(0) == 0xfe && (this.b(1) & 0xc0) == 0xc0;
    }

    /** Multicast with scope 0xe (global). */
    public boolean isMCGlobal() {
        return this.b(0) == 0xff && (this.b(1) & 0x0f) == 0x0e;
    }

    /** Multicast with scope 0x1 (interface-local). */
    public boolean isMCNodeLocal() {
        return this.b(0) == 0xff && (this.b(1) & 0x0f) == 0x01;
    }

    /** Multicast with scope 0x2 (link-local). */
    public boolean isMCLinkLocal() {
        return this.b(0) == 0xff && (this.b(1) & 0x0f) == 0x02;
    }

    /** Multicast with scope 0x5 (site-local). */
    public boolean isMCSiteLocal() {
        return this.b(0) == 0xff && (this.b(1) & 0x0f) == 0x05;
    }

    /** Multicast with scope 0x8 (organization-local). */
    public boolean isMCOrgLocal() {
        return this.b(0) == 0xff && (this.b(1) & 0x0f) == 0x08;
    }

    /** Whether the first twelve bytes are zero: RFC 4291's "::a.b.c.d" form, now deprecated. */
    public boolean isIPv4CompatibleAddress() {
        int i = 0;
        while (i < 12) {
            if (this.addr[i] != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public byte[] getAddress() {
        return copy(this.addr);
    }

    /** The numeric scope, or 0 if it has none. */
    public int getScopeId() {
        return this.scopeId;
    }

    public String getHostAddress() {
        String s = numericToTextFormat(this.addr);
        if (this.scopeIdSet) {
            return s + "%" + this.scopeId;
        }
        return s;
    }

    // The sum of the four groups of four bytes, read as signed integers. It is the JDK's algorithm;
    // it is not much as a hash, but changing it would make two JDKs disagree on the iteration order of
    // a HashSet of addresses, and that gets noticed.
    public int hashCode() {
        int hash = 0;
        int i = 0;
        while (i < INADDRSZ) {
            int component = 0;
            int j = 0;
            while (j < 4 && i < INADDRSZ) {
                component = (component << 8) + this.addr[i];
                j = j + 1;
                i = i + 1;
            }
            hash = hash + component;
        }
        return hash;
    }

    // Sin el scope: ver la cabecera.
    public boolean equals(Object obj) {
        if (!(obj instanceof Inet6Address)) {
            return false;
        }
        Inet6Address other = (Inet6Address) obj;
        int i = 0;
        while (i < INADDRSZ) {
            if (this.addr[i] != other.addr[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // ---- factorias ------------------------------------------------------------------------------

    /**
     * The IPv6 address with that name, those bytes and that scope.
     *
     * <p>A negative {@code scopeId} counts as "no scope".
     *
     * @throws UnknownHostException if {@code addr} is not 16 long
     */
    public static Inet6Address getByAddress(String host, byte[] addr, int scopeId)
            throws UnknownHostException {
        if (host != null && host.length() > 0 && host.charAt(0) == '[') {
            if (host.charAt(host.length() - 1) == ']') {
                host = host.substring(1, host.length() - 1);
            }
        }
        if (addr == null || addr.length != INADDRSZ) {
            throw new UnknownHostException("addr is of illegal length");
        }
        return new Inet6Address(host, copy(addr), scopeId);
    }

    /**
     * The address the IPv6 literal {@code s} describes, with or without brackets.
     *
     * <p>It returns an {@link Inet4Address} if the literal is of the IPv4-mapped form, which is why
     * the declared type is {@code InetAddress} and not {@code Inet6Address}.
     *
     * <p>The scope is accepted in numeric form only: a "%eth0" would name an interface, and turning a
     * name into an index means enumerating the machine's interfaces, which this VM cannot do (see
     * `NetworkInterface`'s header).
     *
     * @throws IllegalArgumentException if it is not a valid IPv6 literal
     */
    public static InetAddress ofLiteral(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        InetAddress a = parseLiteral(s, true);
        if (a == null) {
            throw invalidLiteral(s);
        }
        return a;
    }

    static String numericToTextFormat(byte[] src) {
        StringBuilder sb = new StringBuilder(39);
        int i = 0;
        while (i < 8) {
            if (i > 0) {
                sb.append(':');
            }
            int group = ((src[i * 2] & 0xff) << 8) | (src[i * 2 + 1] & 0xff);
            sb.append(Integer.toHexString(group));
            i = i + 1;
        }
        return sb.toString();
    }

    // The first ten bytes zero and the next two 0xff: the mark of an IPv4 written as IPv6. It returns
    // the four real bytes, or null if it is not of that form.
    static byte[] convertFromIPv4MappedAddress(byte[] addr) {
        if (addr.length != INADDRSZ) {
            return null;
        }
        int i = 0;
        while (i < 10) {
            if (addr[i] != 0) {
                return null;
            }
            i = i + 1;
        }
        if ((addr[10] & 0xff) != 0xff || (addr[11] & 0xff) != 0xff) {
            return null;
        }
        return new byte[] {addr[12], addr[13], addr[14], addr[15]};
    }

    // RFC 4291's parser, with "::" and with an IPv4 tail. It returns null --it does not throw-- so
    // that callers can chain attempts: `InetAddress.ofLiteral` tries IPv4 first and IPv6 after.
    static InetAddress parseLiteral(String s, boolean allowBrackets) {
        if (s == null) {
            return null;
        }
        if (allowBrackets && s.length() > 2 && s.charAt(0) == '['
                && s.charAt(s.length() - 1) == ']') {
            s = s.substring(1, s.length() - 1);
        }
        int scope = -1;
        int pct = s.indexOf('%');
        if (pct != -1) {
            if (pct == s.length() - 1) {
                return null;
            }
            String tail = s.substring(pct + 1);
            long v = 0;
            int i = 0;
            while (i < tail.length()) {
                int d = digit(tail.charAt(i), 10);
                // A non-numeric scope would name a network interface; see the header.
                if (d < 0) {
                    return null;
                }
                v = v * 10 + d;
                if (v > 0x7fffffffL) {
                    return null;
                }
                i = i + 1;
            }
            scope = (int) v;
            s = s.substring(0, pct);
        }
        byte[] bytes = textToNumericFormat(s);
        if (bytes == null) {
            return null;
        }
        byte[] v4 = convertFromIPv4MappedAddress(bytes);
        if (v4 != null) {
            return new Inet4Address(null, v4);
        }
        if (scope >= 0) {
            return new Inet6Address(null, bytes, scope);
        }
        return new Inet6Address(null, bytes);
    }

    static byte[] textToNumericFormat(String src) {
        int len = src.length();
        // "::" is the shortest literal there is.
        if (len < 2) {
            return null;
        }
        byte[] dst = new byte[INADDRSZ];
        // Where the "::" was, so as to know how many zeros to insert later.
        int colonp = -1;
        int i = 0;
        int j = 0;
        if (src.charAt(i) == ':') {
            i = i + 1;
            if (src.charAt(i) != ':') {
                return null;
            }
        }
        int curtok = i;
        boolean sawDigit = false;
        int val = 0;
        while (i < len) {
            char ch = src.charAt(i);
            i = i + 1;
            int chval = digit(ch, 16);
            if (chval != -1) {
                val = (val << 4) | chval;
                if (val > 0xffff) {
                    return null;
                }
                sawDigit = true;
                continue;
            }
            if (ch == ':') {
                curtok = i;
                if (!sawDigit) {
                    if (colonp != -1) {
                        return null;
                    }
                    colonp = j;
                    continue;
                }
                if (i == len) {
                    return null;
                }
                if (j + 2 > INADDRSZ) {
                    return null;
                }
                dst[j] = (byte) ((val >> 8) & 0xff);
                dst[j + 1] = (byte) (val & 0xff);
                j = j + 2;
                sawDigit = false;
                val = 0;
                continue;
            }
            if (ch == '.' && (j + 4) <= INADDRSZ) {
                String tail = src.substring(curtok);
                // The tail has to be a complete IPv4: "::1.2.3" is not a literal, even though
                // "1.2.3" on its own is.
                int dots = 0;
                int k = 0;
                while (k < tail.length()) {
                    if (tail.charAt(k) == '.') {
                        dots = dots + 1;
                    }
                    k = k + 1;
                }
                if (dots != 3) {
                    return null;
                }
                byte[] v4 = Inet4Address.textToNumericFormat(tail);
                if (v4 == null) {
                    return null;
                }
                dst[j] = v4[0];
                dst[j + 1] = v4[1];
                dst[j + 2] = v4[2];
                dst[j + 3] = v4[3];
                j = j + 4;
                sawDigit = false;
                break;
            }
            return null;
        }
        if (sawDigit) {
            if (j + 2 > INADDRSZ) {
                return null;
            }
            dst[j] = (byte) ((val >> 8) & 0xff);
            dst[j + 1] = (byte) (val & 0xff);
            j = j + 2;
        }
        if (colonp != -1) {
            // What came after the "::" is shifted right and the gap is filled with zeros.
            if (j == INADDRSZ) {
                return null;
            }
            int n = j - colonp;
            int k = 1;
            while (k <= n) {
                dst[INADDRSZ - k] = dst[colonp + n - k];
                dst[colonp + n - k] = 0;
                k = k + 1;
            }
            j = INADDRSZ;
        }
        if (j != INADDRSZ) {
            return null;
        }
        return dst;
    }

    // The constructor that takes the interface. The numeric scope comes from its index, which is what
    // goes into the textual and the serialized form.
    Inet6Address(String hostName, byte[] addr, NetworkInterface nif) {
        super(hostName, addr);
        if (nif == null) {
            this.scopeId = 0;
            this.scopeIdSet = false;
        } else {
            this.scopeId = nif.getIndex();
            this.scopeIdSet = true;
        }
        this.scopedInterface = nif;
    }

    /**
     * The address of those bytes, with the interface {@code nif}'s scope.
     *
     * <p>The numeric scope that results is the interface's index. With a null {@code nif} the address
     * is left **with no scope**, which is not the same as with scope zero.
     *
     * @throws UnknownHostException if {@code addr} is not sixteen bytes long
     */
    public static Inet6Address getByAddress(String host, byte[] addr, NetworkInterface nif)
            throws UnknownHostException {
        if (host != null && host.length() > 0 && host.charAt(0) == '[') {
            if (host.charAt(host.length() - 1) == ']') {
                host = host.substring(1, host.length() - 1);
            }
        }
        if (addr == null || addr.length != INADDRSZ) {
            throw new UnknownHostException("addr is of illegal length");
        }
        return new Inet6Address(host, copy(addr), nif);
    }

    /**
     * The interface this address was created with, or null.
     *
     * <p>Null too when the scope was given as a number: the interface cannot be taken from an index,
     * and returning whichever one has that index today would be inventing. It is what the JDK does.
     */
    public NetworkInterface getScopedInterface() {
        return this.scopedInterface;
    }
}
