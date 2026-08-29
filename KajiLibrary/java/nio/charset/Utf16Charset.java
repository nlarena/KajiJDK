package java.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * The three UTF-16 charsets: big-endian, little-endian, and the one that carries a byte-order
 * mark.
 *
 * <p>Two bytes per character, four for the ones outside the Basic Multilingual Plane, which are
 * written as the surrogate pair Java already keeps them in. That last part is why UTF-16 encoding
 * looks so much like copying: a Java {@code char} <em>is</em> a UTF-16 code unit, so the only
 * real work is choosing which end of it goes first.
 *
 * <p>Since the bytes alone cannot say which end that is, the "UTF-16" charset spends the first
 * two bytes of every stream saying so. The BE and LE charsets do not, because their names
 * already answered the question -- and a byte-order mark inside one of those is not a marker at
 * all but a genuine U+FEFF, which is why they pass it through as a character.
 *
 * <p>Package-private; reached through {@link StandardCharsets}.
 */
final class Utf16Charset extends RankedCharset {

    // Fixed big-endian: no mark is written and none is consumed.
    static final int MODE_BIG = 0;

    // Fixed little-endian, likewise.
    static final int MODE_LITTLE = 1;

    // Byte-order mark: the decoder reads one if it is there and defaults to big-endian if it is
    // not; the encoder always writes one, big-endian.
    static final int MODE_MARK = 2;

    private final int mode;

    Utf16Charset(String canonicalName, String[] aliases, int mode) {
        super(canonicalName, aliases, RankedCharset.RANK_UNICODE);
        this.mode = mode;
    }

    int mode() {
        return this.mode;
    }

    public CharsetDecoder newDecoder() {
        return new Utf16Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Utf16Encoder(this);
    }
}

/** The UTF-16 decoding loop, for all three byte-order modes. */
final class Utf16Decoder extends CharsetDecoder {

    private final int mode;
    private boolean little;
    private boolean orderSettled;

    Utf16Decoder(Utf16Charset cs) {
        super(cs, 0.5f, 1.0f);
        this.mode = cs.mode();
        this.implReset();
    }

    protected void implReset() {
        this.little = this.mode == Utf16Charset.MODE_LITTLE;
        this.orderSettled = this.mode != Utf16Charset.MODE_MARK;
    }

    // Reads the leading byte-order mark, if there is one. Returns false when the input is too
    // short to tell yet, which is not an error -- two bytes may simply not have arrived.
    private boolean settleOrder(ByteBuffer in) {
        if (this.orderSettled) {
            return true;
        }
        if (in.remaining() < 2) {
            return false;
        }
        int start = in.position();
        int first = in.get() & 0xff;
        int second = in.get() & 0xff;
        if (first == 0xfe && second == 0xff) {
            this.little = false;
        } else if (first == 0xff && second == 0xfe) {
            this.little = true;
        } else {
            // No mark: big-endian by definition, and the bytes belong to the text.
            this.little = false;
            in.position(start);
        }
        this.orderSettled = true;
        return true;
    }

    private char unit(int first, int second) {
        // if/else and not a conditional expression: the conditional operator collapses char to
        // int here (finding #260), and this is the one place in the package that would need it.
        if (this.little) {
            return (char) ((second << 8) | first);
        }
        return (char) ((first << 8) | second);
    }

    protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
        if (!this.settleOrder(in)) {
            return CoderResult.UNDERFLOW;
        }
        while (in.remaining() >= 2) {
            int start = in.position();
            char c = this.unit(in.get() & 0xff, in.get() & 0xff);
            if (Character.isHighSurrogate(c)) {
                if (in.remaining() < 2) {
                    in.position(start);
                    return CoderResult.UNDERFLOW;
                }
                char low = this.unit(in.get() & 0xff, in.get() & 0xff);
                if (!Character.isLowSurrogate(low)) {
                    in.position(start);
                    return CoderResult.malformedForLength(2);
                }
                if (out.remaining() < 2) {
                    in.position(start);
                    return CoderResult.OVERFLOW;
                }
                out.put(c);
                out.put(low);
                continue;
            }
            if (Character.isLowSurrogate(c)) {
                // A low surrogate with nothing in front of it: the stream is out of step.
                in.position(start);
                return CoderResult.malformedForLength(2);
            }
            if (!out.hasRemaining()) {
                in.position(start);
                return CoderResult.OVERFLOW;
            }
            out.put(c);
        }
        // A single trailing byte is not malformed yet -- its partner may be in the next buffer.
        // The framework turns it into a malformed result when the caller says the input ended.
        return CoderResult.UNDERFLOW;
    }
}

/** The UTF-16 encoding loop, for all three byte-order modes. */
final class Utf16Encoder extends CharsetEncoder {

    private final int mode;
    private final boolean little;
    private boolean markWritten;

    Utf16Encoder(Utf16Charset cs) {
        // The mark costs two bytes once, and the maximum is per character, so a charset that
        // writes one has to admit to 4 bytes for the first character rather than 2.
        super(cs, 2.0f,
                cs.mode() == Utf16Charset.MODE_MARK ? 4.0f : 2.0f,
                Utf16Encoder.questionMark(cs.mode() == Utf16Charset.MODE_LITTLE));
        this.mode = cs.mode();
        this.little = cs.mode() == Utf16Charset.MODE_LITTLE;
        this.implReset();
    }

    // The replacement has to be a whole code unit, not the single byte the base class would
    // default to -- an odd number of bytes in a UTF-16 stream desynchronises everything after it.
    private static byte[] questionMark(boolean little) {
        byte[] out = new byte[2];
        out[little ? 0 : 1] = (byte) '?';
        return out;
    }

    protected void implReset() {
        this.markWritten = this.mode != Utf16Charset.MODE_MARK;
    }

    private void putUnit(ByteBuffer out, char c) {
        int high = (c >> 8) & 0xff;
        int low = c & 0xff;
        if (this.little) {
            out.put((byte) low);
            out.put((byte) high);
        } else {
            out.put((byte) high);
            out.put((byte) low);
        }
    }

    protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
        if (!this.markWritten) {
            if (out.remaining() < 2) {
                return CoderResult.OVERFLOW;
            }
            out.put((byte) 0xfe);
            out.put((byte) 0xff);
            this.markWritten = true;
        }
        while (in.hasRemaining()) {
            int start = in.position();
            char c = in.get();
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
                if (out.remaining() < 4) {
                    in.position(start);
                    return CoderResult.OVERFLOW;
                }
                this.putUnit(out, c);
                this.putUnit(out, low);
                continue;
            }
            if (Character.isLowSurrogate(c)) {
                in.position(start);
                return CoderResult.malformedForLength(1);
            }
            if (out.remaining() < 2) {
                in.position(start);
                return CoderResult.OVERFLOW;
            }
            this.putUnit(out, c);
        }
        return CoderResult.UNDERFLOW;
    }
}
