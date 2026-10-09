package java.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Turns characters into bytes according to one {@link Charset}.
 *
 * <p>The mirror of {@link CharsetDecoder}, and stateful for the same reason: a character can
 * straddle the end of a buffer -- here a surrogate pair split between two pieces of input -- so
 * the encoder has to remember what it was in the middle of.
 *
 * <p>It carries one concern the decoder does not. Decoding can always produce <em>something</em>,
 * because Unicode has a character for "this was broken". Encoding cannot: a charset with no room
 * for a character has to drop it, substitute it, or refuse, which is what {@link
 * CodingErrorAction} and {@link #replacement} are for.
 *
 * <p>Not thread-safe. {@link Charset#newEncoder} is cheap.
 */
public abstract class CharsetEncoder {

    // The same state machine as the decoder; see CharsetDecoder for what the states mean.
    private static final int ST_RESET = 0;
    private static final int ST_CODING = 1;
    private static final int ST_END = 2;
    private static final int ST_FLUSHED = 3;

    private final Charset cs;
    private final float averageBytesPerChar;
    private final float maxBytesPerChar;
    private byte[] repl;
    private CodingErrorAction malformedAction = CodingErrorAction.REPORT;
    private CodingErrorAction unmappableAction = CodingErrorAction.REPORT;
    private int state = CharsetEncoder.ST_RESET;

    /**
     * Initialises the shared part of an encoder, with an explicit replacement.
     *
     * @param cs the charset that created this encoder
     * @param averageBytesPerChar a positive estimate, used to size output buffers
     * @param maxBytesPerChar a positive upper bound; no single input character may ever produce
     *        more than this many bytes
     * @param replacement the bytes substituted for input this encoder refuses; must be non-empty,
     *        no longer than {@code maxBytesPerChar}, and itself encodable
     * @throws IllegalArgumentException if any of those conditions fails
     */
    protected CharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar,
            byte[] replacement) {
        if (averageBytesPerChar <= 0.0f) {
            throw new IllegalArgumentException("Non-positive averageBytesPerChar");
        }
        if (maxBytesPerChar <= 0.0f) {
            throw new IllegalArgumentException("Non-positive maxBytesPerChar");
        }
        if (averageBytesPerChar > maxBytesPerChar) {
            throw new IllegalArgumentException("averageBytesPerChar exceeds maxBytesPerChar");
        }
        this.cs = cs;
        this.averageBytesPerChar = averageBytesPerChar;
        this.maxBytesPerChar = maxBytesPerChar;
        // Set directly, not through replaceWith: the legality check there asks this charset to
        // decode the replacement, and the charset is not finished being constructed yet.
        this.repl = CharsetEncoder.copyOf(replacement);
    }

    /**
     * Initialises the shared part of an encoder, replacing with a question mark.
     *
     * @param cs the charset that created this encoder
     * @param averageBytesPerChar a positive estimate, used to size output buffers
     * @param maxBytesPerChar a positive upper bound
     */
    protected CharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar) {
        this(cs, averageBytesPerChar, maxBytesPerChar, CharsetEncoder.questionMark());
    }

    private static byte[] questionMark() {
        byte[] one = new byte[1];
        one[0] = (byte) '?';
        return one;
    }

    // Hand-written because System.arraycopy is not usable from this library (finding #258).
    private static byte[] copyOf(byte[] src) {
        byte[] out = new byte[src.length];
        int i = 0;
        while (i < src.length) {
            out[i] = src[i];
            i = i + 1;
        }
        return out;
    }

    /** The charset that created this encoder. */
    public final Charset charset() {
        return this.cs;
    }

    /**
     * The bytes substituted for input this encoder refuses, when the action is REPLACE.
     *
     * <p><strong>Deliberate divergence:</strong> a fresh copy, where the JDK hands back the array
     * it holds. Copying costs an allocation and removes a way to corrupt an encoder from the
     * outside, which is the trade this library makes elsewhere too.
     */
    public final byte[] replacement() {
        return CharsetEncoder.copyOf(this.repl);
    }

    /**
     * Changes the replacement bytes.
     *
     * @param newReplacement the substitute; must be non-empty, no longer than {@link
     *        #maxBytesPerChar}, and legal according to {@link #isLegalReplacement}
     * @return this encoder
     * @throws IllegalArgumentException if the replacement is empty, too long, or not legal
     */
    public final CharsetEncoder replaceWith(byte[] newReplacement) {
        if (newReplacement == null) {
            throw new IllegalArgumentException("Null replacement");
        }
        int len = newReplacement.length;
        if (len == 0) {
            throw new IllegalArgumentException("Empty replacement");
        }
        if ((float) len > this.maxBytesPerChar) {
            throw new IllegalArgumentException("Replacement too long");
        }
        if (!this.isLegalReplacement(newReplacement)) {
            throw new IllegalArgumentException("Illegal replacement");
        }
        this.repl = CharsetEncoder.copyOf(newReplacement);
        this.implReplaceWith(newReplacement);
        return this;
    }

    /**
     * Hook called after the replacement changes; does nothing here.
     *
     * @param newReplacement the replacement just installed
     */
    protected void implReplaceWith(byte[] newReplacement) {
    }

    /**
     * Whether these bytes are a legal replacement for this charset.
     *
     * <p>Legal means "decodes back", and the check matters more than it looks: a replacement that
     * is not valid in the target encoding would inject malformed bytes into the output every time
     * it was used, turning one unencodable character into a corrupt stream.
     *
     * @param replacement the candidate bytes
     */
    public boolean isLegalReplacement(byte[] replacement) {
        CharsetDecoder decoder = this.cs.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer in = ByteBuffer.wrap(replacement);
        int room = (int) ((float) replacement.length * decoder.maxCharsPerByte()) + 1;
        CharBuffer out = CharBuffer.allocate(room);
        CoderResult cr = decoder.decode(in, out, true);
        return !cr.isError();
    }

    /** What this encoder does with malformed input. */
    public CodingErrorAction malformedInputAction() {
        return this.malformedAction;
    }

    /**
     * Sets what to do with malformed input.
     *
     * @param newAction the action to take
     * @return this encoder
     */
    public final CharsetEncoder onMalformedInput(CodingErrorAction newAction) {
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

    /** What this encoder does with characters it cannot represent. */
    public CodingErrorAction unmappableCharacterAction() {
        return this.unmappableAction;
    }

    /**
     * Sets what to do with characters that cannot be represented.
     *
     * @param newAction the action to take
     * @return this encoder
     */
    public final CharsetEncoder onUnmappableCharacter(CodingErrorAction newAction) {
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

    /** The expected number of bytes per input character; an estimate, for sizing buffers. */
    public final float averageBytesPerChar() {
        return this.averageBytesPerChar;
    }

    /** The most bytes a single input character can ever produce; a guarantee, not an estimate. */
    public final float maxBytesPerChar() {
        return this.maxBytesPerChar;
    }

    /**
     * Encodes as much of {@code in} into {@code out} as it can.
     *
     * <p>The mirror of {@link CharsetDecoder#decode(ByteBuffer, CharBuffer, boolean)}, with the
     * same four outcomes and the same contract about {@code endOfInput}.
     *
     * @param in the characters to read; its position advances past what was consumed
     * @param out where the bytes go
     * @param endOfInput true if {@code in} holds the last of the input, which is what lets the
     *        encoder report a lone high surrogate rather than waiting for its partner
     * @return why encoding stopped
     * @throws IllegalStateException if called after a final call without an intervening reset
     * @throws CoderMalfunctionError if {@link #encodeLoop} threw
     */
    public final CoderResult encode(CharBuffer in, ByteBuffer out, boolean endOfInput) {
        int newState = endOfInput ? CharsetEncoder.ST_END : CharsetEncoder.ST_CODING;
        boolean legal = this.state == CharsetEncoder.ST_RESET
                || this.state == CharsetEncoder.ST_CODING
                || (endOfInput && this.state == CharsetEncoder.ST_END);
        if (!legal) {
            throw new IllegalStateException("Current state = " + this.state);
        }
        this.state = newState;
        while (true) {
            CoderResult cr = this.safeEncodeLoop(in, out);
            if (cr.isOverflow()) {
                return cr;
            }
            if (cr.isUnderflow()) {
                if (!endOfInput || !in.hasRemaining()) {
                    return cr;
                }
                // Input ended mid-character -- a high surrogate with no low one behind it.
                cr = CoderResult.malformedForLength(in.remaining());
            }
            CodingErrorAction action = cr.isMalformed()
                    ? this.malformedAction : this.unmappableAction;
            if (action == CodingErrorAction.REPORT) {
                return cr;
            }
            if (action == CodingErrorAction.REPLACE) {
                if (out.remaining() < this.repl.length) {
                    return CoderResult.OVERFLOW;
                }
                out.put(this.repl);
            }
            in.position(in.position() + cr.length());
        }
    }

    // See CharsetDecoder.safeDecodeLoop: a RuntimeException out of a coding loop is a defect in
    // the charset, and CoderMalfunctionError says so.
    private CoderResult safeEncodeLoop(CharBuffer in, ByteBuffer out) {
        try {
            return this.encodeLoop(in, out);
        } catch (RuntimeException problem) {
            throw new CoderMalfunctionError(problem);
        }
    }

    /**
     * Writes whatever trailing output the final input left pending.
     *
     * @param out where any trailing bytes go
     * @return UNDERFLOW when done, OVERFLOW if {@code out} filled first
     * @throws IllegalStateException if the last encode call did not pass {@code endOfInput}
     */
    public final CoderResult flush(ByteBuffer out) {
        if (this.state == CharsetEncoder.ST_END) {
            CoderResult cr = this.implFlush(out);
            if (cr.isUnderflow()) {
                this.state = CharsetEncoder.ST_FLUSHED;
            }
            return cr;
        }
        if (this.state != CharsetEncoder.ST_FLUSHED) {
            throw new IllegalStateException("Current state = " + this.state);
        }
        return CoderResult.UNDERFLOW;
    }

    /**
     * Hook for trailing output; produces none here.
     *
     * @param out where any trailing bytes would go
     * @return UNDERFLOW
     */
    protected CoderResult implFlush(ByteBuffer out) {
        return CoderResult.UNDERFLOW;
    }

    /**
     * Returns this encoder to its initial state, ready for an unrelated input.
     *
     * @return this encoder
     */
    public final CharsetEncoder reset() {
        this.state = CharsetEncoder.ST_RESET;
        this.implReset();
        return this;
    }

    /** Hook for charset-specific state to clear on reset; does nothing here. */
    protected void implReset() {
    }

    /**
     * The encoding loop of the charset itself, and the only method a charset must write.
     *
     * <p>It reads from {@code in} and writes to {@code out}, returning UNDERFLOW when it runs out
     * of input or OVERFLOW when it runs out of room, and leaving {@code in} positioned at the
     * start of whatever it did not consume.
     *
     * @param in the characters to read
     * @param out where the bytes go
     * @return why the loop stopped
     */
    protected abstract CoderResult encodeLoop(CharBuffer in, ByteBuffer out);

    /**
     * Encodes all of {@code in} in one call, growing the output buffer as needed.
     *
     * @param in the characters to encode, all of them
     * @return a buffer of the resulting bytes, flipped and ready to read
     * @throws MalformedInputException if the input is malformed and the action is REPORT
     * @throws UnmappableCharacterException if a character cannot be represented and the action
     *         is REPORT
     */
    public final ByteBuffer encode(CharBuffer in) throws CharacterCodingException {
        int n = (int) ((float) in.remaining() * this.averageBytesPerChar);
        ByteBuffer out = ByteBuffer.allocate(n);
        if (n == 0 && in.remaining() == 0) {
            return out;
        }
        this.reset();
        while (true) {
            CoderResult cr = in.hasRemaining()
                    ? this.encode(in, out, true) : CoderResult.UNDERFLOW;
            if (cr.isUnderflow()) {
                cr = this.flush(out);
            }
            if (cr.isUnderflow()) {
                break;
            }
            if (cr.isOverflow()) {
                n = n * 2 + 1;
                ByteBuffer bigger = ByteBuffer.allocate(n);
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
     * Whether this charset can represent the given character.
     *
     * @param c the character to test; a lone surrogate always answers false, since half a
     *        character is not a character
     */
    public boolean canEncode(char c) {
        CharBuffer one = CharBuffer.allocate(1);
        one.put(c);
        one.flip();
        return this.canEncode(one);
    }

    /**
     * Whether this charset can represent every character in the given sequence.
     *
     * @param csq the text to test
     */
    public boolean canEncode(CharSequence csq) {
        CharBuffer cb = CharBuffer.wrap(csq);
        return this.canEncode(cb);
    }

    // Encodes into nowhere with both actions set to REPORT, and reports whether that succeeded.
    //
    // The actions are saved and put back by hand rather than in a `finally`, because
    // try/catch/finally with a caught exception miscompiles here (finding #257). The one thing
    // lost is restoration when `encode` throws something OTHER than a coding exception -- which
    // would be a CoderMalfunctionError, i.e. an already-broken charset.
    private boolean canEncode(CharBuffer cb) {
        if (this.state == CharsetEncoder.ST_FLUSHED) {
            this.reset();
        } else if (this.state != CharsetEncoder.ST_RESET) {
            throw new IllegalStateException("Current state = " + this.state);
        }
        CodingErrorAction savedMalformed = this.malformedInputAction();
        CodingErrorAction savedUnmappable = this.unmappableCharacterAction();
        this.onMalformedInput(CodingErrorAction.REPORT);
        this.onUnmappableCharacter(CodingErrorAction.REPORT);
        boolean encodable = true;
        try {
            this.encode(cb);
        } catch (CharacterCodingException refused) {
            encodable = false;
        }
        this.onMalformedInput(savedMalformed);
        this.onUnmappableCharacter(savedUnmappable);
        this.reset();
        return encodable;
    }
}
