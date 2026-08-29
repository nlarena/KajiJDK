package java.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Turns bytes into characters according to one {@link Charset}.
 *
 * <p>A decoder is a small state machine, and the reason it exists at all -- rather than a single
 * "byte array in, String out" function -- is that a character can straddle the end of a buffer.
 * The caller feeds input in pieces, says whether each piece is the last, and the decoder
 * remembers what it was in the middle of.
 *
 * <p>The intended sequence is: {@link #reset}, then {@link #decode(ByteBuffer, CharBuffer,
 * boolean)} once per piece with {@code endOfInput} false, then one final call with it true, then
 * {@link #flush}. The one-argument {@link #decode(ByteBuffer)} does all of that for a caller who
 * has the whole input already.
 *
 * <p>Not thread-safe, and deliberately so: the state that makes partial input work belongs to
 * one decoder, so a decoder belongs to one thread at a time. {@link Charset#newDecoder} is cheap.
 */
public abstract class CharsetDecoder {

    // The state machine the JDK specifies, as a small int. RESET and CODING accept input; END is
    // "the last piece has been given"; FLUSHED is "the trailing output has been produced". The
    // transitions are checked because using a decoder out of order is a bug that otherwise shows
    // up much later, as silently wrong text.
    private static final int ST_RESET = 0;
    private static final int ST_CODING = 1;
    private static final int ST_END = 2;
    private static final int ST_FLUSHED = 3;

    // U+FFFD REPLACEMENT CHARACTER, built rather than written as a literal so that this source
    // stays pure ASCII and survives every tool that touches it.
    private static final String DEFAULT_REPLACEMENT = CharsetDecoder.replacementString();

    private static String replacementString() {
        char[] one = new char[1];
        one[0] = (char) 0xfffd;
        return String.valueOf(one, 0, 1);
    }

    private final Charset cs;
    private final float averageCharsPerByte;
    private final float maxCharsPerByte;
    private String repl = CharsetDecoder.DEFAULT_REPLACEMENT;
    private CodingErrorAction malformedAction = CodingErrorAction.REPORT;
    private CodingErrorAction unmappableAction = CodingErrorAction.REPORT;
    private int state = CharsetDecoder.ST_RESET;

    /**
     * Initialises the shared part of a decoder.
     *
     * @param cs the charset that created this decoder
     * @param averageCharsPerByte a positive estimate, used to size output buffers
     * @param maxCharsPerByte a positive upper bound; no single input byte may ever produce more
     *        than this many characters
     * @throws IllegalArgumentException if either ratio is not positive, or the average exceeds
     *         the maximum
     */
    protected CharsetDecoder(Charset cs, float averageCharsPerByte, float maxCharsPerByte) {
        if (averageCharsPerByte <= 0.0f) {
            throw new IllegalArgumentException("Non-positive averageCharsPerByte");
        }
        if (maxCharsPerByte <= 0.0f) {
            throw new IllegalArgumentException("Non-positive maxCharsPerByte");
        }
        if (averageCharsPerByte > maxCharsPerByte) {
            throw new IllegalArgumentException("averageCharsPerByte exceeds maxCharsPerByte");
        }
        this.cs = cs;
        this.averageCharsPerByte = averageCharsPerByte;
        this.maxCharsPerByte = maxCharsPerByte;
    }

    /** The charset that created this decoder. */
    public final Charset charset() {
        return this.cs;
    }

    /** The string substituted for input this decoder refuses, when the action is REPLACE. */
    public final String replacement() {
        return this.repl;
    }

    /**
     * Changes the replacement string.
     *
     * @param newReplacement the substitute; must be non-empty and no longer than {@link
     *        #maxCharsPerByte}, so that replacing can never overflow an output buffer that was
     *        sized from that bound
     * @return this decoder
     * @throws IllegalArgumentException if the replacement is empty or too long
     */
    public final CharsetDecoder replaceWith(String newReplacement) {
        if (newReplacement == null) {
            throw new IllegalArgumentException("Null replacement");
        }
        int len = newReplacement.length();
        if (len == 0) {
            throw new IllegalArgumentException("Empty replacement");
        }
        if ((float) len > this.maxCharsPerByte) {
            throw new IllegalArgumentException("Replacement too long");
        }
        this.repl = newReplacement;
        this.implReplaceWith(newReplacement);
        return this;
    }

    /**
     * Hook called after the replacement changes; does nothing here.
     *
     * @param newReplacement the replacement just installed
     */
    protected void implReplaceWith(String newReplacement) {
    }

    /** What this decoder does with malformed input. */
    public CodingErrorAction malformedInputAction() {
        return this.malformedAction;
    }

    /**
     * Sets what to do with malformed input.
     *
     * @param newAction the action to take
     * @return this decoder
     */
    public final CharsetDecoder onMalformedInput(CodingErrorAction newAction) {
        if (newAction == null) {
            throw new IllegalArgumentException("Null action");
        }
        this.malformedAction = newAction;
        this.implOnMalformedInput(newAction);
        return this;
    }

    /**
     * Hook called after the malformed-input action changes; does nothing here.
     *
     * @param newAction the action just installed
     */
    protected void implOnMalformedInput(CodingErrorAction newAction) {
    }

    /** What this decoder does with input it cannot map. */
    public CodingErrorAction unmappableCharacterAction() {
        return this.unmappableAction;
    }

    /**
     * Sets what to do with input that cannot be mapped.
     *
     * @param newAction the action to take
     * @return this decoder
     */
    public final CharsetDecoder onUnmappableCharacter(CodingErrorAction newAction) {
        if (newAction == null) {
            throw new IllegalArgumentException("Null action");
        }
        this.unmappableAction = newAction;
        this.implOnUnmappableCharacter(newAction);
        return this;
    }

    /**
     * Hook called after the unmappable-character action changes; does nothing here.
     *
     * @param newAction the action just installed
     */
    protected void implOnUnmappableCharacter(CodingErrorAction newAction) {
    }

    /** The expected number of characters per input byte; an estimate, for sizing buffers. */
    public final float averageCharsPerByte() {
        return this.averageCharsPerByte;
    }

    /** The most characters a single input byte can ever produce; a guarantee, not an estimate. */
    public final float maxCharsPerByte() {
        return this.maxCharsPerByte;
    }

    /**
     * Decodes as much of {@code in} into {@code out} as it can.
     *
     * <p>Returns for one of four reasons, and the caller is expected to look at which: UNDERFLOW
     * means the input is exhausted (feed more, or this was the end); OVERFLOW means {@code out}
     * is full (drain it and call again); the error results appear only when the matching action
     * is REPORT, since IGNORE and REPLACE are handled here and never surface.
     *
     * @param in the bytes to read; its position advances past what was consumed
     * @param out where the characters go
     * @param endOfInput true if {@code in} holds the last of the input, which is what lets the
     *        decoder report a truncated character rather than waiting forever for its tail
     * @return why decoding stopped
     * @throws IllegalStateException if called after a final call without an intervening reset
     * @throws CoderMalfunctionError if {@link #decodeLoop} threw
     */
    public final CoderResult decode(ByteBuffer in, CharBuffer out, boolean endOfInput) {
        int newState = endOfInput ? CharsetDecoder.ST_END : CharsetDecoder.ST_CODING;
        boolean legal = this.state == CharsetDecoder.ST_RESET
                || this.state == CharsetDecoder.ST_CODING
                || (endOfInput && this.state == CharsetDecoder.ST_END);
        if (!legal) {
            throw new IllegalStateException("Current state = " + this.state);
        }
        this.state = newState;
        while (true) {
            CoderResult cr = this.safeDecodeLoop(in, out);
            if (cr.isOverflow()) {
                return cr;
            }
            if (cr.isUnderflow()) {
                if (!endOfInput || !in.hasRemaining()) {
                    return cr;
                }
                // The input ended in the middle of a character: nothing more is coming, so what
                // is left can only be malformed.
                cr = CoderResult.malformedForLength(in.remaining());
            }
            CodingErrorAction action = cr.isMalformed()
                    ? this.malformedAction : this.unmappableAction;
            if (action == CodingErrorAction.REPORT) {
                return cr;
            }
            if (action == CodingErrorAction.REPLACE) {
                if (out.remaining() < this.repl.length()) {
                    return CoderResult.OVERFLOW;
                }
                out.put(this.repl);
            }
            in.position(in.position() + cr.length());
        }
    }

    // The decoding loop is written by the charset, so it is the one piece here that can be wrong
    // in an unbounded way. A RuntimeException out of it is a defect in the charset, not in the
    // data, and CoderMalfunctionError says exactly that.
    private CoderResult safeDecodeLoop(ByteBuffer in, CharBuffer out) {
        try {
            return this.decodeLoop(in, out);
        } catch (RuntimeException problem) {
            throw new CoderMalfunctionError(problem);
        }
    }

    /**
     * Writes whatever trailing output the final input left pending.
     *
     * <p>Most charsets have none -- the ones that do are the stateful encodings, where finishing
     * means emitting a shift back to the initial state.
     *
     * @param out where any trailing characters go
     * @return UNDERFLOW when done, OVERFLOW if {@code out} filled first
     * @throws IllegalStateException if the last decode call did not pass {@code endOfInput}
     */
    public final CoderResult flush(CharBuffer out) {
        if (this.state == CharsetDecoder.ST_END) {
            CoderResult cr = this.implFlush(out);
            if (cr.isUnderflow()) {
                this.state = CharsetDecoder.ST_FLUSHED;
            }
            return cr;
        }
        if (this.state != CharsetDecoder.ST_FLUSHED) {
            throw new IllegalStateException("Current state = " + this.state);
        }
        return CoderResult.UNDERFLOW;
    }

    /**
     * Hook for trailing output; produces none here.
     *
     * @param out where any trailing characters would go
     * @return UNDERFLOW
     */
    protected CoderResult implFlush(CharBuffer out) {
        return CoderResult.UNDERFLOW;
    }

    /**
     * Returns this decoder to its initial state, ready for an unrelated input.
     *
     * @return this decoder
     */
    public final CharsetDecoder reset() {
        this.state = CharsetDecoder.ST_RESET;
        this.implReset();
        return this;
    }

    /** Hook for charset-specific state to clear on reset; does nothing here. */
    protected void implReset() {
    }

    /**
     * The decoding loop of the charset itself, and the only method a charset must write.
     *
     * <p>It reads from {@code in} and writes to {@code out}, and returns UNDERFLOW when it runs
     * out of input or OVERFLOW when it runs out of room. It must leave {@code in} positioned at
     * the start of whatever it did not consume -- that is what makes resuming across buffers
     * work, and it is also how the error results address the offending bytes.
     *
     * @param in the bytes to read
     * @param out where the characters go
     * @return why the loop stopped
     */
    protected abstract CoderResult decodeLoop(ByteBuffer in, CharBuffer out);

    /**
     * Decodes all of {@code in} in one call, growing the output buffer as needed.
     *
     * <p>Resets this decoder first and drives the whole reset/decode/flush sequence, so it is
     * the method to use when the entire input is already in hand. Errors are thrown rather than
     * returned, which is why this one is declared to throw.
     *
     * @param in the bytes to decode, all of them
     * @return a buffer of the resulting characters, flipped and ready to read
     * @throws MalformedInputException if the input is malformed and the action is REPORT
     * @throws UnmappableCharacterException if input cannot be mapped and the action is REPORT
     */
    public final CharBuffer decode(ByteBuffer in) throws CharacterCodingException {
        int n = (int) ((float) in.remaining() * this.averageCharsPerByte);
        CharBuffer out = CharBuffer.allocate(n);
        if (n == 0 && in.remaining() == 0) {
            return out;
        }
        this.reset();
        while (true) {
            CoderResult cr = in.hasRemaining()
                    ? this.decode(in, out, true) : CoderResult.UNDERFLOW;
            if (cr.isUnderflow()) {
                cr = this.flush(out);
            }
            if (cr.isUnderflow()) {
                break;
            }
            if (cr.isOverflow()) {
                n = n * 2 + 1;
                CharBuffer bigger = CharBuffer.allocate(n);
                out.flip();
                bigger.put(out);
                out = bigger;
                continue;
            }
            cr.throwException();
        }
        out.flip();
        return out;
    }

    /**
     * Whether this decoder works out the charset from the input itself.
     *
     * <p>False here and for every charset in this library. The decoders behind names like
     * "UTF-16" do read a byte-order mark, but that settles the byte order and not the charset,
     * so they are not auto-detecting in this sense either.
     */
    public boolean isAutoDetecting() {
        return false;
    }

    /**
     * Whether an auto-detecting decoder has settled on a charset yet.
     *
     * @throws UnsupportedOperationException always, since this decoder does not auto-detect
     */
    public boolean isCharsetDetected() {
        throw new UnsupportedOperationException();
    }

    /**
     * The charset an auto-detecting decoder settled on.
     *
     * @throws UnsupportedOperationException always, since this decoder does not auto-detect
     */
    public Charset detectedCharset() {
        throw new UnsupportedOperationException();
    }
}
