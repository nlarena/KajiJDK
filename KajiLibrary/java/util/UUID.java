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
    /**
     * El UUID de version 3 para un nombre: el MD5 de los bytes, con la version y la variante
     * estampadas encima.
     *
     * <p>Lo que lo hace util es que es **determinista**: el mismo nombre da siempre el mismo id, en
     * cualquier maquina y sin coordinacion. Es lo contrario de `randomUUID()`, y sirve para lo
     * contrario -- darle una identidad estable a algo que ya tiene un nombre unico (una URL, un
     * DN, una ruta) sin tener que guardar la correspondencia en ningun lado.
     *
     * <p>MD5 esta roto para criptografia desde 2004 y aca no importa: no se lo usa para autenticar
     * nada, solo para repartir nombres en el espacio de 128 bits. Es lo que la RFC 4122 fija para
     * la version 3, y cambiarlo cambiaria los ids.
     */
    public static UUID nameUUIDFromBytes(byte[] name) {
        byte[] h = md5(name);
        // Los seis bits que la RFC reserva: cuatro para la version (3) y dos para la variante
        // (IETF). El resto del hash queda intacto.
        h[6] = (byte) ((h[6] & 0x0f) | 0x30);
        h[8] = (byte) ((h[8] & 0x3f) | 0x80);
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (h[i] & 0xffL);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (h[i] & 0xffL);
        }
        return new UUID(msb, lsb);
    }

    // ---- MD5 (RFC 1321) --------------------------------------------------------------------
    //
    // Va escrito aca y no contra `java.security.MessageDigest` porque esa clase no existe en la
    // biblioteca. Es la unica razon; el dia que exista, esto se reemplaza por tres lineas.

    // Los desplazamientos de cada una de las 64 vueltas, cuatro patrones de a dieciseis.
    private static final int[] MD5_S = {
        7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
        5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
        4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
        6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
    };

    // La tabla de constantes: `T[i] = floor(2^32 * abs(sin(i + 1)))`, con el angulo en radianes.
    //
    // Va literal y no calculada porque `Math.sin` no esta en esta biblioteca. Una tabla escrita a
    // mano es justo donde se cuela un digito cambiado, asi que la prueba de comportamiento compara
    // el UUID resultante contra el de `java` real -- un solo bit distinto en cualquiera de las 64
    // constantes cambia los 128 bits de la salida.
    private static final int[] MD5_K = {
        0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee,
        0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
        0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be,
        0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
        0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa,
        0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
        0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed,
        0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
        0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c,
        0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
        0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05,
        0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
        0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039,
        0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
        0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1,
        0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391,
    };

    private static byte[] md5(byte[] msg) {
        // Relleno: un 0x80, ceros hasta dejar 8 bytes libres en el ultimo bloque, y el largo en
        // **bits** como entero de 64, little-endian. Ese largo al final es lo que impide que dos
        // mensajes distintos con el mismo relleno colisionen por construccion.
        long bits = ((long) msg.length) * 8L;
        int total = msg.length + 1;
        while (total % 64 != 56) {
            total = total + 1;
        }
        total = total + 8;
        byte[] m = new byte[total];
        System.arraycopy(msg, 0, m, 0, msg.length);
        m[msg.length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) {
            m[total - 8 + i] = (byte) (bits >>> (8 * i));
        }

        int a0 = 0x67452301;
        int b0 = 0xefcdab89;
        int c0 = 0x98badcfe;
        int d0 = 0x10325476;

        int[] w = new int[16];
        int bloque = 0;
        while (bloque < total) {
            // Las palabras del bloque, little-endian.
            for (int i = 0; i < 16; i++) {
                int o = bloque + i * 4;
                w[i] = (m[o] & 0xff) | ((m[o + 1] & 0xff) << 8)
                        | ((m[o + 2] & 0xff) << 16) | ((m[o + 3] & 0xff) << 24);
            }
            int a = a0;
            int b = b0;
            int c = c0;
            int d = d0;
            for (int i = 0; i < 64; i++) {
                int f;
                int g;
                if (i < 16) {
                    f = (b & c) | (~b & d);
                    g = i;
                } else if (i < 32) {
                    f = (d & b) | (~d & c);
                    g = (5 * i + 1) % 16;
                } else if (i < 48) {
                    f = b ^ c ^ d;
                    g = (3 * i + 5) % 16;
                } else {
                    f = c ^ (b | ~d);
                    g = (7 * i) % 16;
                }
                f = f + a + MD5_K[i] + w[g];
                a = d;
                d = c;
                c = b;
                b = b + Integer.rotateLeft(f, MD5_S[i]);
            }
            a0 = a0 + a;
            b0 = b0 + b;
            c0 = c0 + c;
            d0 = d0 + d;
            bloque = bloque + 64;
        }

        byte[] out = new byte[16];
        escribirLE(out, 0, a0);
        escribirLE(out, 4, b0);
        escribirLE(out, 8, c0);
        escribirLE(out, 12, d0);
        return out;
    }

    private static void escribirLE(byte[] out, int off, int v) {
        for (int i = 0; i < 4; i++) {
            out[off + i] = (byte) (v >>> (8 * i));
        }
    }

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
