package java.util;

// KajiLibrary's java.util.Base64 — RFC 4648 base64, the standard way to carry arbitrary bytes
// through a channel that only tolerates text (a URL, an HTTP header, a JSON string, an email
// body).
//
// THE IDEA IS PURE ARITHMETIC. Three bytes are 24 bits; 24 bits split evenly into four 6-bit
// groups; each 6-bit group (0..63) names one character of a 64-character alphabet. So every
// 3 input bytes become exactly 4 output characters, and the encoding costs a flat 33% in size.
// The whole implementation is that sentence plus the handling of what happens when the input
// length is not a multiple of 3.
//
// THE TAIL is the only subtle part. A 1-byte remainder is 8 bits, which fills one 6-bit group
// and leaves 2 bits over; those 2 bits are shifted up to become the top of a second group, and
// the bottom 4 bits of that group are zero. A 2-byte remainder is 16 bits: two full groups and
// a 4-bit leftover padded to a third. So a remainder emits 2 or 3 characters, never 1 — and to
// keep the output a multiple of 4 (so a decoder can consume it in fixed 4-character atoms) the
// missing characters are written as '='. Padding carries no data: it only records how many
// bytes the last atom stands for. `withoutPadding()` drops it, which is legal when the length
// is known some other way.
//
// TWO ALPHABETS. The first 62 characters are A-Z, a-z, 0-9 in that order — that part is
// universal. The last two differ: '+' and '/' for the basic variant, '-' and '_' for the
// URL-and-filename-safe one, because '+' and '/' are meaningful inside a URL path or query.
// A decoder built for one alphabet REJECTS the other's two characters; it does not accept both.
//
// KajiLibrary implements the basic and URL-safe variants. The MIME variant (getMimeEncoder /
// getMimeDecoder: 76-character lines, CRLF separators, and a decoder that silently skips
// anything outside the alphabet) is omitted — it is the same core with a line-wrapping shell.
//
// Encoder and Decoder are *static nested* classes here exactly as in the JDK: they carry the
// per-variant tables, so `Base64` itself is a pure factory and is never instantiated.
public class Base64 {

    // Non-instantiable: every entry point is static (matches the JDK, which hides the ctor).
    private Base64() {
    }

    // The basic RFC 4648 encoder: '+' and '/', with '=' padding.
    public static Encoder getEncoder() {
        return new Encoder(false, true);
    }

    // The URL-and-filename-safe encoder: '-' and '_', with '=' padding.
    public static Encoder getUrlEncoder() {
        return new Encoder(true, true);
    }

    // The basic RFC 4648 decoder. Rejects '-' and '_'.
    public static Decoder getDecoder() {
        return new Decoder(false);
    }

    // The URL-and-filename-safe decoder. Rejects '+' and '/'.
    /**
     * The **MIME** encoder (RFC 2045): the basic alphabet, cut into lines of 76 characters separated
     * by CRLF.
     *
     * <p>The cut is not decorative. MIME was born for the body of an email, and the transports of the
     * day did not guarantee long lines: a line of 10,000 characters could arrive cut wherever the
     * server felt like, and that breaks the base64. Cutting it oneself every 76 is making sure nobody
     * else cuts it.
     */
    public static Encoder getMimeEncoder() {
        return new Encoder(false, true, 76, new byte[] {(byte) '\r', (byte) '\n'});
    }

    /**
     * A MIME encoder with the line length and separator it is given.
     *
     * <p>`lineLength` is **rounded down to a multiple of 4**, and if it comes out at zero or less
     * nothing is cut. Both rules are the JDK's and both have a reason: four characters are one group
     * of three bytes, so cutting anywhere else would split a group down the middle.
     *
     * @throws IllegalArgumentException if `lineSeparator` contains a base64 alphabet character
     *         --it would be indistinguishable from the data when decoding--
     */
    public static Encoder getMimeEncoder(int lineLength, byte[] lineSeparator) {
        if (lineSeparator == null) {
            throw new NullPointerException();
        }
        byte[] alphabet = new byte[64];
        char[] base = Encoder.buildAlphabet(false);
        int i = 0;
        while (i < 64) {
            alphabet[i] = (byte) base[i];
            i = i + 1;
        }
        int j = 0;
        while (j < lineSeparator.length) {
            int k = 0;
            while (k < 64) {
                if (lineSeparator[j] == alphabet[k]) {
                    throw new IllegalArgumentException(
                            "Illegal base64 line separator character 0x"
                                    + Integer.toString(lineSeparator[j], 16));
                }
                k = k + 1;
            }
            j = j + 1;
        }
        int len = lineLength / 4 * 4;
        if (len <= 0) {
            return new Encoder(false, true, 0, new byte[0]);
        }
        byte[] copied = new byte[lineSeparator.length];
        System.arraycopy(lineSeparator, 0, copied, 0, lineSeparator.length);
        return new Encoder(false, true, len, copied);
    }

    /**
     * The MIME decoder: the basic alphabet, and it **discards** every character not in it.
     *
     * <p>That tolerance is the difference from `getDecoder()`, which rejects what it does not
     * recognise. Here it is the right thing: the text comes from an email, with line breaks, spaces
     * and whatever the transport put in between.
     */
    public static Decoder getMimeDecoder() {
        return new Decoder(false, true);
    }

    public static Decoder getUrlDecoder() {
        return new Decoder(true);
    }

    // --- Base64.Encoder -------------------------------------------------------------------

    // Bytes in, base64 text out. Stateless and immutable, so one instance is safe to share
    // between threads; that is why the factories above can hand out fresh ones freely.
    public static class Encoder {

        // 6-bit group value -> character. A `final char[]` instance field rather than a
        // `static final` one: reference constants are fine, but keeping it per-instance means
        // the two alphabets are just two objects instead of two static tables (finding #110
        // makes cross-class static field reads a trap, so we avoid static state entirely).
        private final char[] toBase64;

        // Whether the tail is padded out to a multiple of 4 with '='.
        private final boolean doPadding;

        // Every how many characters the line is cut (0 = it is not cut), and with what separator.
        private final int lineLength;
        private final byte[] lineSeparator;

        // Package-private, like the JDK's private one: instances come from the factories.
        Encoder(boolean url, boolean doPadding) {
            this(url, doPadding, 0, new byte[0]);
        }

        Encoder(boolean url, boolean doPadding, int lineLength, byte[] lineSeparator) {
            this.toBase64 = buildAlphabet(url);
            this.doPadding = doPadding;
            this.lineLength = lineLength;
            this.lineSeparator = lineSeparator;
        }

        // A-Z, a-z, 0-9, then the two variant characters. Computed rather than spelled out:
        // the alphabet is contiguous ASCII runs, so arithmetic is both shorter and harder to
        // typo than a 64-element literal.
        static char[] buildAlphabet(boolean url) {
            char[] t = new char[64];
            for (int i = 0; i < 26; i++) {
                t[i] = (char) ('A' + i);
            }
            for (int i = 0; i < 26; i++) {
                t[26 + i] = (char) ('a' + i);
            }
            for (int i = 0; i < 10; i++) {
                t[52 + i] = (char) ('0' + i);
            }
            if (url) {
                t[62] = '-';
                t[63] = '_';
            } else {
                t[62] = '+';
                t[63] = '/';
            }
            return t;
        }

        // How many characters `srclen` bytes encode to: 4 per whole 3-byte group, then either
        // a full padded atom or just the 2-or-3 characters the remainder actually needs.
        private int outLength(int srclen) {
            int n = srclen / 3 * 4;
            int rem = srclen % 3;
            if (rem != 0) {
                if (this.doPadding) {
                    n = n + 4;
                } else {
                    n = n + rem + 1;
                }
            }
            // The MIME variant's line separators. They go **between** lines and not at the end, so
            // they are `(n-1)/lineLength` and not `n/lineLength`: an output of exactly 76 characters
            // carries zero separators, not one.
            if (this.lineLength > 0 && n > 0) {
                n = n + ((n - 1) / this.lineLength) * this.lineSeparator.length;
            }
            return n;
        }

        // The encoder proper. Everything else is a wrapper that decides where the characters go.
        private char[] encodeChars(byte[] src) {
            char[] raw = encodeUnwrapped(src);
            if (this.lineLength <= 0 || raw.length == 0) {
                return raw;
            }
            // It is laid out in lines by inserting the separator every `lineLength` characters.
            char[] dst = new char[outLength(src.length)];
            int readCount = 0;
            int written = 0;
            while (readCount < raw.length) {
                if (readCount > 0 && readCount % this.lineLength == 0) {
                    int k = 0;
                    while (k < this.lineSeparator.length) {
                        dst[written] = (char) (this.lineSeparator[k] & 0xff);
                        written = written + 1;
                        k = k + 1;
                    }
                }
                dst[written] = raw[readCount];
                written = written + 1;
                readCount = readCount + 1;
            }
            return dst;
        }

        // The encoding without cutting into lines, which is the one that does the inner work.
        private char[] encodeUnwrapped(byte[] src) {
            int slen = src.length;
            int noSeparator = slen / 3 * 4;
            int rest = slen % 3;
            if (rest != 0) {
                noSeparator = noSeparator + (this.doPadding ? 4 : rest + 1);
            }
            char[] dst = new char[noSeparator];
            int full = slen / 3 * 3;   // the part that splits evenly into 3-byte groups
            int sp = 0;
            int dp = 0;
            while (sp < full) {
                // Three bytes packed big-endian into the low 24 bits, then read back 6 at a
                // time from the top. The `& 0xff` matters: Java's byte is signed, so a byte
                // >= 0x80 would sign-extend into the groups above it without it.
                int bits = ((src[sp] & 0xff) << 16) | ((src[sp + 1] & 0xff) << 8) | (src[sp + 2] & 0xff);
                sp = sp + 3;
                dst[dp] = this.toBase64[(bits >>> 18) & 0x3f];
                dst[dp + 1] = this.toBase64[(bits >>> 12) & 0x3f];
                dst[dp + 2] = this.toBase64[(bits >>> 6) & 0x3f];
                dst[dp + 3] = this.toBase64[bits & 0x3f];
                dp = dp + 4;
            }
            int rem = slen - full;
            if (rem == 1) {
                int b0 = src[sp] & 0xff;
                dst[dp] = this.toBase64[b0 >>> 2];             // top 6 bits
                dst[dp + 1] = this.toBase64[(b0 << 4) & 0x3f]; // last 2 bits, zero-extended
                dp = dp + 2;
                if (this.doPadding) {
                    dst[dp] = '=';
                    dst[dp + 1] = '=';
                    dp = dp + 2;
                }
            } else if (rem == 2) {
                int b0 = src[sp] & 0xff;
                int b1 = src[sp + 1] & 0xff;
                dst[dp] = this.toBase64[b0 >>> 2];
                dst[dp + 1] = this.toBase64[((b0 << 4) & 0x3f) | (b1 >>> 4)];
                dst[dp + 2] = this.toBase64[(b1 << 2) & 0x3f]; // last 4 bits, zero-extended
                dp = dp + 3;
                if (this.doPadding) {
                    dst[dp] = '=';
                    dp = dp + 1;
                }
            }
            return dst;
        }

        // Encode into a fresh byte[] of base64 characters (all of them are ASCII, so the
        // char->byte narrowing is lossless).
        public byte[] encode(byte[] src) {
            char[] chars = encodeChars(src);
            byte[] dst = new byte[chars.length];
            for (int i = 0; i < chars.length; i++) {
                dst[i] = (byte) chars[i];
            }
            return dst;
        }

        // Encode into a caller-supplied buffer; returns how many bytes were written.
        public int encode(byte[] src, byte[] dst) {
            char[] chars = encodeChars(src);
            if (dst.length < chars.length) {
                throw new IllegalArgumentException("Output byte array is too small for encoding all input bytes");
            }
            for (int i = 0; i < chars.length; i++) {
                dst[i] = (byte) chars[i];
            }
            return chars.length;
        }

        // The common case: bytes in, String out.
        public String encodeToString(byte[] src) {
            char[] chars = encodeChars(src);
            return String.valueOf(chars, 0, chars.length);
        }

        // The same alphabet, but the tail is left unpadded. Returns a new Encoder because an
        // Encoder is immutable — `Base64.getEncoder()` keeps its padding.
        public Encoder withoutPadding() {
            Encoder result;
            if (this.doPadding) {
                result = new Encoder(this.toBase64[62] == '-', false);
            } else {
                result = this;
            }
            return result;
        }
    }

    // --- Base64.Decoder -------------------------------------------------------------------

    // Base64 text in, bytes out. Strict: any character outside this variant's alphabet, and
    // any malformed final atom, raises IllegalArgumentException rather than being skipped.
    public static class Decoder {

        // character code (0..255) -> 6-bit value, -1 for "not in this alphabet", -2 for '='.
        // The two sentinels are what makes the decode loop a single table lookup: a negative
        // result means "not data", and which negative says whether it is padding or an error.
        private final int[] fromBase64;

        // Whether it is the MIME variant: it discards what it does not recognise instead of
        // rejecting it.
        private final boolean mime;

        Decoder(boolean url) {
            this(url, false);
        }

        Decoder(boolean url, boolean mime) {
            this.fromBase64 = buildDecodeTable(url);
            this.mime = mime;
        }

        private static int[] buildDecodeTable(boolean url) {
            int[] t = new int[256];
            for (int i = 0; i < 256; i++) {
                t[i] = -1;
            }
            for (int i = 0; i < 26; i++) {
                t['A' + i] = i;
            }
            for (int i = 0; i < 26; i++) {
                t['a' + i] = 26 + i;
            }
            for (int i = 0; i < 10; i++) {
                t['0' + i] = 52 + i;
            }
            if (url) {
                t['-'] = 62;
                t['_'] = 63;
            } else {
                t['+'] = 62;
                t['/'] = 63;
            }
            t['='] = -2;
            return t;
        }

        // How many bytes the input decodes to. Padding is what tells us: two '=' means the last
        // atom carries 1 byte, one '=' means 2. When padding is absent the same information is
        // in the length modulo 4, so we reconstruct the padding count from it.
        private int outLength(byte[] src) {
            int len = src.length;
            if (this.mime) {
                // Only the alphabet's characters are counted: the others contribute no bits, and
                // counting them would oversize the array (and with it, what `decode` returns).
                int usefulCount = 0;
                int i = 0;
                while (i < len) {
                    int c = src[i] & 0xff;
                    if (this.fromBase64[c] != -1) {
                        usefulCount = usefulCount + 1;
                    }
                    i = i + 1;
                }
                if (usefulCount == 0) {
                    return 0;
                }
                byte[] cleaned = new byte[usefulCount];
                int j = 0;
                i = 0;
                while (i < len) {
                    int c = src[i] & 0xff;
                    if (this.fromBase64[c] != -1) {
                        cleaned[j] = src[i];
                        j = j + 1;
                    }
                    i = i + 1;
                }
                src = cleaned;
                len = usefulCount;
            }
            int result;
            if (len == 0) {
                result = 0;
            } else if (len < 2) {
                // One character is never a valid encoding: the smallest atom is 2 characters.
                throw new IllegalArgumentException("Input byte[] should at least have 2 bytes for base64 bytes");
            } else {
                int paddings = 0;
                if (src[len - 1] == '=') {
                    paddings = 1;
                    if (src[len - 2] == '=') {
                        paddings = 2;
                    }
                }
                if (paddings == 0 && (len & 0x3) != 0) {
                    paddings = 4 - (len & 0x3);
                }
                result = 3 * ((len + 3) / 4) - paddings;
            }
            return result;
        }

        // The decoder proper: fill a 24-bit accumulator 6 bits at a time and flush 3 bytes
        // whenever it is full. `shiftto` is where the next 6-bit group lands — 18, 12, 6, 0 —
        // so it doubles as a record of how far into the current atom we are, which is exactly
        // what the end-of-input and padding checks need.
        private int decode0(byte[] src, byte[] dst) {
            int sl = src.length;
            int sp = 0;
            int dp = 0;
            int bits = 0;
            int shiftto = 18;
            boolean done = false;
            while (sp < sl && !done) {
                int c = src[sp] & 0xff;
                sp = sp + 1;
                int b = this.fromBase64[c];
                if (b == -2) {
                    // '='. Legal only where the atom is genuinely short: after 2 characters
                    // (shiftto == 6, and then it must be the very last character or be followed
                    // by a second '='), or after 3 (shiftto == 0). A leading '=' (shiftto == 18)
                    // or one after a single character is malformed.
                    boolean bad = false;
                    if (shiftto == 18) {
                        bad = true;
                    } else if (shiftto == 12) {
                        bad = true;
                    } else if (shiftto == 6) {
                        if (sp == sl) {
                            bad = true;
                        } else {
                            int c2 = src[sp] & 0xff;
                            sp = sp + 1;
                            if (c2 != '=') {
                                bad = true;
                            }
                        }
                    }
                    if (bad) {
                        throw new IllegalArgumentException("Input byte array has wrong 4-byte ending unit");
                    }
                    done = true;
                } else if (b < 0) {
                    // The MIME variant **discards** what it does not recognise --line breaks,
                    // spaces, whatever the transport put in-- instead of rejecting it. It is the whole
                    // difference from `getDecoder()`, and it is what makes a base64 pasted out of an
                    // email decode.
                    if (!this.mime) {
                        throw new IllegalArgumentException("Illegal base64 character");
                    }
                } else {
                    bits = bits | (b << shiftto);
                    shiftto = shiftto - 6;
                    if (shiftto < 0) {
                        dst[dp] = (byte) (bits >> 16);
                        dst[dp + 1] = (byte) (bits >> 8);
                        dst[dp + 2] = (byte) bits;
                        dp = dp + 3;
                        shiftto = 18;
                        bits = 0;
                    }
                }
            }
            // Input (or the data part of it) ended mid-atom: 6 bits pending means 1 whole byte
            // was assembled, 0 pending means 2. 12 pending means a lone dangling character,
            // which carries only 6 bits — not enough for even one byte.
            if (shiftto == 6) {
                dst[dp] = (byte) (bits >> 16);
                dp = dp + 1;
            } else if (shiftto == 0) {
                dst[dp] = (byte) (bits >> 16);
                dst[dp + 1] = (byte) (bits >> 8);
                dp = dp + 2;
            } else if (shiftto == 12) {
                throw new IllegalArgumentException("Last unit does not have enough valid bits");
            }
            // Anything after the padding is not ours to ignore.
            if (sp < sl) {
                throw new IllegalArgumentException("Input byte array has incorrect ending byte");
            }
            return dp;
        }

        public byte[] decode(byte[] src) {
            byte[] dst = new byte[outLength(src)];
            int n = decode0(src, dst);
            byte[] result;
            if (n == dst.length) {
                result = dst;
            } else {
                // Unpadded input can over-estimate by a byte; hand back an exact-length array.
                result = new byte[n];
                for (int i = 0; i < n; i++) {
                    result[i] = dst[i];
                }
            }
            return result;
        }

        // Decode text. Base64 is ASCII, so the String is narrowed to bytes latin-1 style; a
        // character above U+00FF is unmappable and becomes '?', which is not in the alphabet
        // and so fails the same way the JDK's ISO-8859-1 conversion does.
        public byte[] decode(String src) {
            int n = src.length();
            byte[] bytes = new byte[n];
            for (int i = 0; i < n; i++) {
                char c = src.charAt(i);
                if (c > 255) {
                    bytes[i] = (byte) '?';
                } else {
                    bytes[i] = (byte) c;
                }
            }
            return decode(bytes);
        }

        // Decode into a caller-supplied buffer; returns how many bytes were written.
        public int decode(byte[] src, byte[] dst) {
            int need = outLength(src);
            if (dst.length < need) {
                throw new IllegalArgumentException("Output byte array is too small for decoding all input bytes");
            }
            return decode0(src, dst);
        }
    }
}
