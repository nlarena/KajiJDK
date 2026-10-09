package java.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * The two eight-bit charsets whose byte values <em>are</em> their code points: US-ASCII and
 * ISO-8859-1.
 *
 * <p>One class for both, because they differ in exactly one number -- the highest code point
 * they can hold, 0x7F or 0xFF -- and in nothing else. Both map byte {@code n} to character
 * {@code U+00n} and back, which is not a coincidence: Unicode adopted the Latin-1 block
 * unchanged precisely so that this conversion would stay trivial.
 *
 * <p>Package-private; reached through {@link StandardCharsets#US_ASCII} and {@link
 * StandardCharsets#ISO_8859_1}.
 */
final class SingleByteCharset extends RankedCharset {

    // The highest code point this charset can hold; also the highest byte value it accepts,
    // since the two are the same number here.
    private final int ceiling;

    SingleByteCharset(String canonicalName, String[] aliases, int rank, int ceiling) {
        super(canonicalName, aliases, rank);
        this.ceiling = ceiling;
    }

    int ceiling() {
        return this.ceiling;
    }

    public CharsetDecoder newDecoder() {
        return new SingleByteDecoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new SingleByteEncoder(this);
    }
}

/** Decodes one byte to one character, refusing bytes above the ceiling. */
final class SingleByteDecoder extends CharsetDecoder {

    private final int ceiling;

    SingleByteDecoder(SingleByteCharset cs) {
        super(cs, 1.0f, 1.0f);
        this.ceiling = cs.ceiling();
    }

    protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
        while (in.hasRemaining()) {
            if (!out.hasRemaining()) {
                return CoderResult.OVERFLOW;
            }
            int start = in.position();
            int b = in.get() & 0xff;
            if (b > this.ceiling) {
                // Malformed and not unmappable: the byte is not a character this charset failed
                // to place, it is a byte the charset says cannot occur at all. Only US-ASCII
                // ever gets here; ISO-8859-1 has no unused byte values.
                in.position(start);
                return CoderResult.malformedForLength(1);
            }
            out.put((char) b);
        }
        return CoderResult.UNDERFLOW;
    }
}

/** Encodes one character to one byte, refusing anything above the ceiling. */
final class SingleByteEncoder extends CharsetEncoder {

    private final int ceiling;

    SingleByteEncoder(SingleByteCharset cs) {
        super(cs, 1.0f, 1.0f);
        this.ceiling = cs.ceiling();
    }

    protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
        while (in.hasRemaining()) {
            int start = in.position();
            char c = in.get();
            if (Character.isSurrogate(c)) {
                // A surrogate pair is ONE character, so it has to be consumed and refused as a
                // unit -- otherwise a single unrepresentable emoji comes out as two question
                // marks instead of one.
                if (!Character.isHighSurrogate(c)) {
                    in.position(start);
                    return CoderResult.malformedForLength(1);
                }
                if (!in.hasRemaining()) {
                    in.position(start);
                    return CoderResult.UNDERFLOW;
                }
                char low = in.get();
                in.position(start);
                if (!Character.isLowSurrogate(low)) {
                    return CoderResult.malformedForLength(1);
                }
                return CoderResult.unmappableForLength(2);
            }
            if (c > this.ceiling) {
                in.position(start);
                return CoderResult.unmappableForLength(1);
            }
            if (!out.hasRemaining()) {
                in.position(start);
                return CoderResult.OVERFLOW;
            }
            out.put((byte) c);
        }
        return CoderResult.UNDERFLOW;
    }
}
