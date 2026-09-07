package java.net;

// An IPv4 address: four bytes.
//
// Everything here is arithmetic over those four bytes and literal grammar, which is to say it can be
// written complete with no network. The two public parsers --`ofLiteral` and `ofPosixLiteral`-- are
// **different** grammars and the difference matters:
//
//   - `ofLiteral` is the Java platform's form: between one and four fields, **always decimal**.
//     Leading zeros do not mean octal ("010.1.1.1" is 10.1.1.1).
//   - `ofPosixLiteral` is `inet_aton(3)`'s: the same fields, but with C's conventions for the base --
//     "0x" is hexadecimal and a leading zero is octal ("010.1.1.1" is 8.1.1.1).
//
// That both exist is not redundancy: the POSIX one is what `ping`, `curl` and the rest of the system
// use, and reading a literal with the wrong grammar changes the address silently. That is why having
// both under different names is safer than having one "that guesses".
//
// The short form (fewer than four fields) is not a whim either: the last field absorbs all the bytes
// that are missing, so "127.1" is 127.0.0.1 and so is "2130706433".
//
// Nothing is omitted in this class.
public final class Inet4Address extends InetAddress {

    private static final long serialVersionUID = 3286316764910316507L;

    static final int INADDRSZ = 4;

    Inet4Address(String hostName, byte[] addr) {
        super(hostName, addr);
    }

    // The wildcard, 0.0.0.0. It carries "0.0.0.0" as its name and not null, just as in the JDK: it is
    // the only address that prints as "0.0.0.0/0.0.0.0", and `InetSocketAddress(int)` depends on
    // that.
    Inet4Address() {
        super("0.0.0.0", new byte[] {0, 0, 0, 0});
    }

    private int b(int i) {
        return this.addr[i] & 0xff;
    }

    /** 224.0.0.0/4. */
    public boolean isMulticastAddress() {
        return (this.addr[0] & 0xf0) == 0xe0;
    }

    /** 0.0.0.0. */
    public boolean isAnyLocalAddress() {
        return this.b(0) == 0 && this.b(1) == 0 && this.b(2) == 0 && this.b(3) == 0;
    }

    /** 127.0.0.0/8. */
    public boolean isLoopbackAddress() {
        return this.b(0) == 127;
    }

    /** 169.254.0.0/16. */
    public boolean isLinkLocalAddress() {
        return this.b(0) == 169 && this.b(1) == 254;
    }

    /** 10/8, 172.16/12 and 192.168/16: RFC 1918's three private ranges. */
    public boolean isSiteLocalAddress() {
        return this.b(0) == 10
                || (this.b(0) == 172 && this.b(1) >= 16 && this.b(1) <= 31)
                || (this.b(0) == 192 && this.b(1) == 168);
    }

    /** Global multicast: all of 224/4 except the reserved 224.0.0.0/24 block. */
    public boolean isMCGlobal() {
        return this.b(0) >= 224 && this.b(0) <= 238
                && !(this.b(0) == 224 && this.b(1) == 0 && this.b(2) == 0);
    }

    /** IPv4 has no "node" scope, so never. */
    public boolean isMCNodeLocal() {
        return false;
    }

    /** 224.0.0.0/24. */
    public boolean isMCLinkLocal() {
        return this.b(0) == 224 && this.b(1) == 0 && this.b(2) == 0;
    }

    /** 239.255.0.0/16. */
    public boolean isMCSiteLocal() {
        return this.b(0) == 239 && this.b(1) == 255;
    }

    /** 239.192.0.0/14. */
    public boolean isMCOrgLocal() {
        return this.b(0) == 239 && this.b(1) >= 192 && this.b(1) <= 195;
    }

    public byte[] getAddress() {
        return copy(this.addr);
    }

    public String getHostAddress() {
        return numericToTextFormat(this.addr);
    }

    // The four bytes packed into the int, which is an IPv4's natural representation and one with
    // which two different addresses never collide.
    public int hashCode() {
        return (this.b(0) << 24) | (this.b(1) << 16) | (this.b(2) << 8) | this.b(3);
    }

    // An IPv4 is never equal to an IPv6, even if the bytes match: they are addresses from different
    // spaces. (The "IPv4-mapped" form does not break this because it is converted to an Inet4Address
    // on construction, not on comparison.)
    public boolean equals(Object obj) {
        if (!(obj instanceof Inet4Address)) {
            return false;
        }
        Inet4Address other = (Inet4Address) obj;
        int i = 0;
        while (i < INADDRSZ) {
            if (this.addr[i] != other.addr[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // ---- literals -------------------------------------------------------------------------------

    /**
     * The address the decimal literal {@code s} describes (one to four fields).
     *
     * @throws IllegalArgumentException if it is not one
     */
    public static Inet4Address ofLiteral(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        byte[] a = textToNumericFormat(s);
        if (a == null) {
            throw invalidLiteral(s);
        }
        return new Inet4Address(null, a);
    }

    /**
     * The address {@code s} describes under {@code inet_aton(3)}'s rules: "0x" is hex and a leading
     * zero is octal.
     *
     * @throws IllegalArgumentException if it is not a valid POSIX literal
     */
    public static Inet4Address ofPosixLiteral(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        byte[] a = posixToNumericFormat(s);
        if (a == null) {
            throw invalidLiteral(s);
        }
        return new Inet4Address(null, a);
    }

    static String numericToTextFormat(byte[] src) {
        return (src[0] & 0xff) + "." + (src[1] & 0xff) + "." + (src[2] & 0xff) + "." + (src[3] & 0xff);
    }

    // The platform's grammar: decimal fields separated by dots, between one and four. The first
    // fields are worth one byte each; the last is spread over all the bytes that are left, which is
    // where "127.1" and "2130706433" come from.
    //
    // The fifteen-character cap is the JDK's and it is not decorative: without it,
    // "0000000000000000000001" would be a valid address and would also overflow the accumulator.
    static byte[] textToNumericFormat(String src) {
        int len = src.length();
        if (len == 0 || len > 15) {
            return null;
        }
        byte[] res = new byte[INADDRSZ];
        long value = 0;
        int currByte = 0;
        boolean newOctet = true;
        int i = 0;
        while (i < len) {
            char c = src.charAt(i);
            if (c == '.') {
                if (newOctet || value > 0xff || currByte == 3) {
                    return null;
                }
                res[currByte] = (byte) (value & 0xff);
                currByte = currByte + 1;
                value = 0;
                newOctet = true;
            } else {
                int d = digit(c, 10);
                if (d < 0) {
                    return null;
                }
                value = value * 10 + d;
                newOctet = false;
            }
            i = i + 1;
        }
        if (newOctet || value >= (1L << ((4 - currByte) * 8))) {
            return null;
        }
        return spread(res, currByte, value);
    }

    // The same as the previous one, but each field is read with C's bases. It is kept separate rather
    // than adding a flag to the other because the two grammars diverge at the first character ('0')
    // and mixing them lets an error in one leak into the other.
    static byte[] posixToNumericFormat(String src) {
        int len = src.length();
        if (len == 0) {
            return null;
        }
        byte[] res = new byte[INADDRSZ];
        int currByte = 0;
        int start = 0;
        long value = 0;
        int i = 0;
        while (true) {
            if (i == len || src.charAt(i) == '.') {
                if (i == start) {
                    return null;
                }
                Long field = parsePosixField(src.substring(start, i));
                if (field == null) {
                    return null;
                }
                value = field.longValue();
                if (i == len) {
                    break;
                }
                if (currByte == 3 || value > 0xff) {
                    return null;
                }
                res[currByte] = (byte) (value & 0xff);
                currByte = currByte + 1;
                start = i + 1;
            }
            i = i + 1;
        }
        if (value < 0 || value >= (1L << ((4 - currByte) * 8))) {
            return null;
        }
        return spread(res, currByte, value);
    }

    // The last field occupies from `currByte` to the end, in network order.
    private static byte[] spread(byte[] res, int currByte, long value) {
        int b = 3;
        while (b >= currByte) {
            res[b] = (byte) ((value >> (8 * (3 - b))) & 0xff);
            b = b - 1;
        }
        return res;
    }

    private static Long parsePosixField(String f) {
        int radix = 10;
        int from = 0;
        if (f.length() > 1 && f.charAt(0) == '0') {
            if (f.length() > 2 && (f.charAt(1) == 'x' || f.charAt(1) == 'X')) {
                radix = 16;
                from = 2;
            } else {
                radix = 8;
                from = 1;
            }
        }
        if (from >= f.length()) {
            return null;
        }
        long v = 0;
        int i = from;
        while (i < f.length()) {
            int d = digit(f.charAt(i), radix);
            if (d < 0) {
                return null;
            }
            v = v * radix + d;
            if (v > 0xffffffffL) {
                return null;
            }
            i = i + 1;
        }
        return Long.valueOf(v);
    }
}
