package java.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * The three UTF-32 charsets: big-endian, little-endian, and the one that reads a byte-order mark.
 *
 * <p>Four bytes per code point, always, with no surrogates and no variable length. It is the
 * encoding that trades space for the property every other one gives up: the n-th code point is
 * at offset {@code 4n}.
 *
 * <p>One asymmetry worth naming, because it is not a mistake: the "UTF-32" charset <em>reads</em>
 * a byte-order mark but does not <em>write</em> one, where "UTF-16" writes one. That is the JDK
 * behaviour and it is observable -- encoding to UTF-32 and to UTF-32BE gives identical bytes.
 *
 * <p>Package-private; reached through {@link StandardCharsets}.
 */
final class Utf32Charset extends RankedCharset {

    /** Fixed big-endian. */
    static final int MODE_BIG = 0;

    /** Fixed little-endian. */
    static final int MODE_LITTLE = 1;

    /** Reads a leading byte-order mark if present, defaults to big-endian, never writes one. */
    static final int MODE_MARK = 2;

    private final int mode;

    Utf32Charset(String canonicalName, String[] aliases, int mode) {
        super(canonicalName, aliases, RankedCharset.RANK_UNICODE);
        this.mode = mode;
    }

    int mode() {
        return this.mode;
    }

    public CharsetDecoder newDecoder() {
        return new Utf32Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Utf32Encoder(this);
    }
}

/** The UTF-32 decoding loop, for all three byte-order modes. */
final class Utf32Decoder extends CharsetDecoder {

    private final int mode;
    private boolean little;
    private boolean orderSettled;

    Utf32Decoder(Utf32Charset cs) {
        super(cs, 0.25f, 1.0f);
        this.mode = cs.mode();
        this.implReset();
    }

    protected void implReset() {
        this.little = this.mode == Utf32Charset.MODE_LITTLE;
        this.orderSettled = this.mode != Utf32Charset.MODE_MARK;
    }

    private boolean settleOrder(ByteBuffer in) {
        if (this.orderSettled) {
            return true;
        }
        if (in.remaining() < 4) {
            return false;
        }
        int start = in.position();
        int b0 = in.get() & 0xff;
        int b1 = in.get() & 0xff;
        int b2 = in.get() & 0xff;
        int b3 = in.get() & 0xff;
        boolean bigMark = b0 == 0x00 && b1 == 0x00 && b2 == 0xfe && b3 == 0xff;
        boolean littleMark = b0 == 0xff && b1 == 0xfe && b2 == 0x00 && b3 == 0x00;
        if (bigMark) {
            this.little = false;
        } else if (littleMark) {
            this.little = true;
        } else {
            this.little = false;
            in.position(start);
        }
        this.orderSettled = true;
        return true;
    }

    protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
        if (!this.settleOrder(in)) {
            return CoderResult.UNDERFLOW;
        }
        while (in.remaining() >= 4) {
            int start = in.position();
            int b0 = in.get() & 0xff;
            int b1 = in.get() & 0xff;
            int b2 = in.get() & 0xff;
            int b3 = in.get() & 0xff;
            int cp = this.little
                    ? (b3 << 24) | (b2 << 16) | (b1 << 8) | b0
                    : (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
            // Only the range is checked. Surrogate code points pass, which is worth stating
            // because it is the opposite of what UTF-8 does two files over: there, a surrogate is
            // malformed. The reference draws the line in exactly that place, and this was
            // verified against it rather than assumed -- a UTF-32 unit of 0x0000D800 decodes to
            // the char U+D800 and no error.
            boolean valid = cp >= 0 && cp <= 0x10ffff;
            if (!valid) {
                in.position(start);
                return CoderResult.malformedForLength(4);
            }
            int width = cp > 0xffff ? 2 : 1;
            if (out.remaining() < width) {
                in.position(start);
                return CoderResult.OVERFLOW;
            }
            if (width == 1) {
                out.put((char) cp);
            } else {
                out.put(Character.highSurrogate(cp));
                out.put(Character.lowSurrogate(cp));
            }
        }
        return CoderResult.UNDERFLOW;
    }
}

/** The UTF-32 encoding loop, for all three byte-order modes. */
final class Utf32Encoder extends CharsetEncoder {

    private final boolean little;

    Utf32Encoder(Utf32Charset cs) {
        super(cs, 4.0f, 4.0f, Utf32Encoder.questionMark(cs.mode() == Utf32Charset.MODE_LITTLE));
        this.little = cs.mode() == Utf32Charset.MODE_LITTLE;
    }

    // A whole four-byte unit, for the same reason UTF-16 needs a two-byte one.
    private static byte[] questionMark(boolean little) {
        byte[] out = new byte[4];
        out[little ? 0 : 3] = (byte) '?';
        return out;
    }

    private void putCodePoint(ByteBuffer out, int cp) {
        int b0 = (cp >> 24) & 0xff;
        int b1 = (cp >> 16) & 0xff;
        int b2 = (cp >> 8) & 0xff;
        int b3 = cp & 0xff;
        if (this.little) {
            out.put((byte) b3);
            out.put((byte) b2);
            out.put((byte) b1);
            out.put((byte) b0);
        } else {
            out.put((byte) b0);
            out.put((byte) b1);
            out.put((byte) b2);
            out.put((byte) b3);
        }
    }

    protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
        while (in.hasRemaining()) {
            int start = in.position();
            char c = in.get();
            int cp = c;
            if (Character.isHighSurrogate(c)) {
                if (!in.hasRemaining()) {
                    in.position(start);
                    return CoderResult.UNDERFLOW;
                }
                char low = in.get();
                if (!Character.isLowSurrogate(low)) {
                    in.position(start);
                    return CoderResult.malformedForLength(1);
                }
                cp = Character.toCodePoint(c, low);
            } else if (Character.isLowSurrogate(c)) {
                in.position(start);
                return CoderResult.malformedForLength(1);
            }
            if (out.remaining() < 4) {
                in.position(start);
                return CoderResult.OVERFLOW;
            }
            this.putCodePoint(out, cp);
        }
        return CoderResult.UNDERFLOW;
    }
}
