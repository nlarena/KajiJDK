package java.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * UTF-8: every Unicode code point, one to four bytes each, ASCII unchanged.
 *
 * <p>Package-private. The public way to reach it is {@link StandardCharsets#UTF_8}.
 */
final class Utf8Charset extends RankedCharset {

    Utf8Charset() {
        super("UTF-8", Utf8Charset.aliasNames(), RankedCharset.RANK_UNICODE);
    }

    private static String[] aliasNames() {
        String[] out = new String[2];
        out[0] = "UTF8";
        out[1] = "unicode-1-1-utf-8";
        return out;
    }

    public CharsetDecoder newDecoder() {
        return new Utf8Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Utf8Encoder(this);
    }
}

/**
 * The UTF-8 decoding loop.
 *
 * <p>Strict, which is the only safe way to read UTF-8: it rejects overlong forms, encoded
 * surrogates and anything above U+10FFFF. Those are not pedantry -- an overlong encoding of
 * {@code '/'} passes a naive path check and then decodes back into a slash, which is how the
 * strictness rule got written in the first place.
 */
final class Utf8Decoder extends CharsetDecoder {

    Utf8Decoder(Charset cs) {
        super(cs, 1.0f, 1.0f);
    }

    // How many bytes follow this leading byte, or -1 if it cannot lead a sequence. Note that
    // 0xC0 and 0xC1 are excluded: the only things they can encode are overlong forms of ASCII.
    private static int trailerCount(int lead) {
        if (lead >= 0xc2 && lead <= 0xdf) {
            return 1;
        }
        if (lead >= 0xe0 && lead <= 0xef) {
            return 2;
        }
        if (lead >= 0xf0 && lead <= 0xf4) {
            return 3;
        }
        return -1;
    }

    // The legal range of the FIRST trailing byte, narrower than 0x80..0xBF for three of the
    // leading bytes. Each narrowing removes a family of illegal code points at the only place
    // they can be caught cheaply: 0xE0 excludes overlong three-byte forms, 0xF0 excludes overlong
    // four-byte forms, and 0xF4 caps the range at U+10FFFF.
    private static int trailerLow(int lead) {
        if (lead == 0xe0) {
            return 0xa0;
        }
        if (lead == 0xf0) {
            return 0x90;
        }
        return 0x80;
    }

    private static int trailerHigh(int lead) {
        if (lead == 0xf4) {
            return 0x8f;
        }
        // 0xED is NOT narrowed here, though it is the lead byte of the surrogate block. Narrowing
        // it would reject `ED A0 80` after one byte; the reference rejects it after three, because
        // those three bytes ARE a well-formed sequence -- it is the character they spell that is
        // illegal. The check therefore belongs after the code point is assembled, not before.
        return 0xbf;
    }

    private static int leadMask(int trailers) {
        if (trailers == 1) {
            return 0x1f;
        }
        if (trailers == 2) {
            return 0x0f;
        }
        return 0x07;
    }

    protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
        while (in.hasRemaining()) {
            int start = in.position();
            int lead = in.get() & 0xff;
            if (lead < 0x80) {
                if (!out.hasRemaining()) {
                    in.position(start);
                    return CoderResult.OVERFLOW;
                }
                out.put((char) lead);
                continue;
            }
            int trailers = Utf8Decoder.trailerCount(lead);
            if (trailers < 0) {
                in.position(start);
                return CoderResult.malformedForLength(1);
            }
            int cp = lead & Utf8Decoder.leadMask(trailers);
            // How many bytes of this sequence were well-formed, which is what an error result
            // has to report: the caller skips exactly that much and resynchronises there.
            int good = 1;
            int k = 0;
            while (k < trailers) {
                if (!in.hasRemaining()) {
                    in.position(start);
                    return CoderResult.UNDERFLOW;
                }
                int b = in.get() & 0xff;
                int low = k == 0 ? Utf8Decoder.trailerLow(lead) : 0x80;
                int high = k == 0 ? Utf8Decoder.trailerHigh(lead) : 0xbf;
                if (b < low || b > high) {
                    in.position(start);
                    return CoderResult.malformedForLength(good);
                }
                cp = (cp << 6) | (b & 0x3f);
                good = good + 1;
                k = k + 1;
            }
            if (cp >= 0xd800 && cp <= 0xdfff) {
                // A surrogate, spelled out in UTF-8. Well-formed bytes, illegal character: the
                // whole three-byte sequence is the error, so that is the length reported.
                in.position(start);
                return CoderResult.malformedForLength(3);
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

/** The UTF-8 encoding loop. */
final class Utf8Encoder extends CharsetEncoder {

    Utf8Encoder(Charset cs) {
        // 1.1 average is the JDK estimate: mostly-ASCII text with the occasional wider
        // character. It only sizes buffers, so being wrong costs a reallocation, not a result.
        super(cs, 1.1f, 3.0f);
    }

    protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
        while (in.hasRemaining()) {
            int start = in.position();
            char c = in.get();
            if (c < 0x80) {
                if (!out.hasRemaining()) {
                    in.position(start);
                    return CoderResult.OVERFLOW;
                }
                out.put((byte) c);
                continue;
            }
            if (c < 0x800) {
                if (out.remaining() < 2) {
                    in.position(start);
                    return CoderResult.OVERFLOW;
                }
                out.put((byte) (0xc0 | (c >> 6)));
                out.put((byte) (0x80 | (c & 0x3f)));
                continue;
            }
            if (Character.isSurrogate(c)) {
                if (!Character.isHighSurrogate(c)) {
                    // A low surrogate with no high one in front: half a character, and no
                    // amount of further input can repair it.
                    in.position(start);
                    return CoderResult.malformedForLength(1);
                }
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
                int cp = Character.toCodePoint(c, low);
                out.put((byte) (0xf0 | (cp >> 18)));
                out.put((byte) (0x80 | ((cp >> 12) & 0x3f)));
                out.put((byte) (0x80 | ((cp >> 6) & 0x3f)));
                out.put((byte) (0x80 | (cp & 0x3f)));
                continue;
            }
            if (out.remaining() < 3) {
                in.position(start);
                return CoderResult.OVERFLOW;
            }
            out.put((byte) (0xe0 | (c >> 12)));
            out.put((byte) (0x80 | ((c >> 6) & 0x3f)));
            out.put((byte) (0x80 | (c & 0x3f)));
        }
        return CoderResult.UNDERFLOW;
    }
}
