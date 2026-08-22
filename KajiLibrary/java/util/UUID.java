package java.util;

import java.io.Serializable;

// KajiLibrary's java.util.UUID — a 128-bit identifier you can mint without asking anyone.
//
// THE POINT is coordination-free uniqueness. A database sequence gives short unique ids but
// needs a central authority to hand them out; a UUID trades 16 bytes for the ability to be
// generated on any machine, offline, in parallel, with a collision probability so small it is
// not worth engineering against. That is the whole trade.
//
// THE LAYOUT is two longs — `mostSigBits` and `leastSigBits` — and the canonical text form
// 8-4-4-4-12 is just those 32 hex digits with four dashes inserted. The dashes are historical
// (they used to separate time-low / time-mid / time-high / clock-seq / node in version 1); for
// a version-4 UUID they carry no meaning at all, which is why `toString`/`fromString` here are
// pure formatting rather than field access.
//
// TWO NIBBLES ARE NOT RANDOM, and this is the part that is easy to get wrong. The high nibble
// of digit 13 is the VERSION (4 = randomly generated), and the top bits of digit 17 are the
// VARIANT (binary 10 = the IETF/RFC 4122 layout). `randomUUID()` overwrites them after
// generating the bits, which is why a version-4 UUID always reads `xxxxxxxx-xxxx-4xxx-yxxx-...`
// with y one of 8, 9, a, b — and why it has 122 random bits, not 128.
//
// RANDOMNESS, HONESTLY. The JDK draws from SecureRandom; KajiLibrary has no CSPRNG, so
// randomUUID() uses java.util.Random seeded from the clock and an allocation-dependent value.
// That is fine for identifiers, and NOT fine for anything that must be unguessable (a session
// token, a password-reset link): our stream is a 48-bit LCG and is invertible from two outputs.
//
// `nameUUIDFromBytes` (the version-3, MD5-based constructor) is omitted: it needs a real MD5,
// and a stand-in that hashed differently would produce ids that disagree with every other
// implementation — worse than not having the method.
public final class UUID implements Comparable<UUID>, Serializable {

    private final long mostSigBits;

    private final long leastSigBits;

    // The two halves, verbatim: no version or variant bits are imposed. Constructing a UUID
    // this way is how you rebuild one from storage; it is the caller's business whether the
    // bits describe a well-formed RFC 4122 identifier.
    public UUID(long mostSigBits, long leastSigBits) {
        this.mostSigBits = mostSigBits;
        this.leastSigBits = leastSigBits;
    }

    // A fresh version-4 UUID: 122 random bits with the version and variant fields stamped in.
    public static UUID randomUUID() {
        // Seeded per call. `currentTimeMillis` alone has millisecond resolution, so two calls
        // inside the same millisecond would return the SAME id — identityHashCode of a
        // freshly allocated object changes per allocation and breaks that tie.
        long seed = System.currentTimeMillis() * 1000003L;
        seed = seed ^ ((long) System.identityHashCode(new Object()));
        seed = seed ^ (((long) System.identityHashCode(new Object())) << 32);
        Random rnd = new Random(seed);
        long msb = rnd.nextLong();
        long lsb = rnd.nextLong();
        // Version nibble (bits 12..15 of the high half) := 4.
        msb = msb & 0xFFFFFFFFFFFF0FFFL;
        msb = msb | 0x0000000000004000L;
        // Variant (top two bits of the low half) := binary 10.
        lsb = lsb & 0x3FFFFFFFFFFFFFFFL;
        lsb = lsb | 0x8000000000000000L;
        return new UUID(msb, lsb);
    }

    // Parse the canonical text form. Like the JDK this is tolerant about field WIDTH — it wants
    // five dash-separated hex fields and takes the low bits of each, so "1-2-3-4-5" parses to
    // 00000001-0002-0003-0004-000000000005 — but it is strict about there being exactly five.
    public static UUID fromString(String name) {
        int len = name.length();
        if (len > 36) {
            throw new IllegalArgumentException("UUID string too large");
        }
        int dash1 = indexOfDash(name, 0);
        int dash2 = indexOfDash(name, dash1 + 1);
        int dash3 = indexOfDash(name, dash2 + 1);
        int dash4 = indexOfDash(name, dash3 + 1);
        int dash5 = indexOfDash(name, dash4 + 1);
        // Checking dash4 and dash5 is enough to reject every wrong shape: a missing earlier dash
        // propagates to dash4 being -1, and a sixth dash shows up as dash5 being non-negative.
        if (dash4 < 0 || dash5 >= 0) {
            throw new IllegalArgumentException("Invalid UUID string");
        }
        long msb = parseHexField(name, 0, dash1) & 0xFFFFFFFFL;
        msb = msb << 16;
        msb = msb | (parseHexField(name, dash1 + 1, dash2) & 0xFFFFL);
        msb = msb << 16;
        msb = msb | (parseHexField(name, dash2 + 1, dash3) & 0xFFFFL);
        long lsb = parseHexField(name, dash3 + 1, dash4) & 0xFFFFL;
        lsb = lsb << 48;
        lsb = lsb | (parseHexField(name, dash4 + 1, len) & 0xFFFFFFFFFFFFL);
        return new UUID(msb, lsb);
    }

    // java.lang.String has no indexOf in KajiLibrary yet, so: first '-' at or after `from`,
    // or -1. A negative `from` (the caller chaining off a previous miss) also yields -1, which
    // is what makes the single dash4/dash5 test above sufficient.
    private static int indexOfDash(String name, int from) {
        int found = -1;
        if (from >= 0) {
            int n = name.length();
            int i = from;
            while (i < n && found < 0) {
                if (name.charAt(i) == '-') {
                    found = i;
                }
                i = i + 1;
            }
        }
        return found;
    }

    // One dash-separated field as an unsigned hex number. Empty is an error, and so is a field
    // wide enough to overflow a long — matching what Long.parseLong(..., 16) does for the JDK.
    private static long parseHexField(String name, int from, int to) {
        if (from >= to) {
            throw new NumberFormatException("empty UUID field");
        }
        long value = 0L;
        for (int i = from; i < to; i++) {
            char c = name.charAt(i);
            int d;
            if (c >= '0' && c <= '9') {
                d = c - '0';
            } else if (c >= 'a' && c <= 'f') {
                d = c - 'a' + 10;
            } else if (c >= 'A' && c <= 'F') {
                d = c - 'A' + 10;
            } else {
                throw new NumberFormatException("not a hexadecimal digit in UUID string");
            }
            if (value > 0x07FFFFFFFFFFFFFFL) {
                throw new NumberFormatException("UUID field too large");
            }
            value = (value << 4) | (long) d;
        }
        return value;
    }

    public long getLeastSignificantBits() {
        return this.leastSigBits;
    }

    public long getMostSignificantBits() {
        return this.mostSigBits;
    }

    // Which generation scheme produced this id: 1 time-based, 2 DCE, 3 name-based (MD5),
    // 4 random, 5 name-based (SHA-1).
    public int version() {
        return (int) ((this.mostSigBits >> 12) & 0x0FL);
    }

    // Which bit LAYOUT the id uses: 0 is the obsolete NCS one, 2 is RFC 4122 (the only one you
    // will meet), 6 is Microsoft's, 7 is reserved. It is a variable-length field — the leading
    // bits say how many of them count — which is why this is a decision tree and not a mask.
    public int variant() {
        int v;
        long top2 = (this.leastSigBits >>> 62) & 0x3L;
        if (top2 < 2L) {
            v = 0;              // 0xx — one bit of tag
        } else if (top2 == 2L) {
            v = 2;              // 10x — two bits
        } else {
            v = (int) ((this.leastSigBits >>> 61) & 0x7L);  // 110 or 111 — three bits: 6 or 7
        }
        return v;
    }

    // The 60-bit timestamp of a version-1 id, in 100-nanosecond units since 1582-10-15. It is
    // stored in three pieces, high part first in the id but last in the number, so reassembling
    // it means moving the fields around rather than a straight read.
    public long timestamp() {
        if (version() != 1) {
            throw new UnsupportedOperationException("Not a time-based UUID");
        }
        long high = (this.mostSigBits & 0x0FFFL) << 48;
        long mid = ((this.mostSigBits >> 16) & 0x0FFFFL) << 32;
        long low = this.mostSigBits >>> 32;
        return high | mid | low;
    }

    // The version-1 clock sequence: a counter bumped whenever the clock jumps backwards, so
    // that a rewound clock still cannot repeat an id.
    public int clockSequence() {
        if (version() != 1) {
            throw new UnsupportedOperationException("Not a time-based UUID");
        }
        return (int) ((this.leastSigBits & 0x3FFF000000000000L) >>> 48);
    }

    // The version-1 node field: originally the machine's 48-bit MAC address, which is why
    // version 1 leaks where an id was made.
    public long node() {
        if (version() != 1) {
            throw new UnsupportedOperationException("Not a time-based UUID");
        }
        return this.leastSigBits & 0x0000FFFFFFFFFFFFL;
    }

    // The canonical 8-4-4-4-12 lowercase form.
    public String toString() {
        char[] buf = new char[36];
        writeHex(buf, 0, this.mostSigBits >>> 32, 8);
        buf[8] = '-';
        writeHex(buf, 9, (this.mostSigBits >>> 16) & 0xFFFFL, 4);
        buf[13] = '-';
        writeHex(buf, 14, this.mostSigBits & 0xFFFFL, 4);
        buf[18] = '-';
        writeHex(buf, 19, (this.leastSigBits >>> 48) & 0xFFFFL, 4);
        buf[23] = '-';
        writeHex(buf, 24, this.leastSigBits & 0x0000FFFFFFFFFFFFL, 12);
        return String.valueOf(buf, 0, 36);
    }

    private static void writeHex(char[] buf, int offset, long value, int digits) {
        for (int i = 0; i < digits; i++) {
            int shift = (digits - 1 - i) * 4;
            int nibble = (int) ((value >>> shift) & 0xFL);
            char c;
            if (nibble < 10) {
                c = (char) ('0' + nibble);
            } else {
                c = (char) ('a' + nibble - 10);
            }
            buf[offset + i] = c;
        }
    }

    // 128 bits folded down to 32: xor the halves together, then xor that long's own halves.
    // Every input bit reaches the result, which is the most a fold this cheap can promise.
    public int hashCode() {
        long hilo = this.mostSigBits ^ this.leastSigBits;
        return ((int) (hilo >> 32)) ^ ((int) hilo);
    }

    public boolean equals(Object obj) {
        boolean eq = false;
        if (obj instanceof UUID) {
            UUID other = (UUID) obj;
            if (this.mostSigBits == other.mostSigBits && this.leastSigBits == other.leastSigBits) {
                eq = true;
            }
        }
        return eq;
    }

    // Ordered by the two halves as SIGNED longs — the JDK's documented behaviour, and worth
    // knowing because it is NOT the same order as comparing the printed strings: a UUID whose
    // first hex digit is 8..f has a negative high half and sorts before one starting 0..7.
    public int compareTo(UUID val) {
        int c;
        if (this.mostSigBits < val.mostSigBits) {
            c = -1;
        } else if (this.mostSigBits > val.mostSigBits) {
            c = 1;
        } else if (this.leastSigBits < val.leastSigBits) {
            c = -1;
        } else if (this.leastSigBits > val.leastSigBits) {
            c = 1;
        } else {
            c = 0;
        }
        return c;
    }
}
