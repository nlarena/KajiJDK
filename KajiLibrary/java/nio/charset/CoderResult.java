package java.nio.charset;

/**
 * The outcome of one call to a coder: how the coding loop stopped.
 *
 * <p>Four states, and telling them apart is the whole point of the class. UNDERFLOW means the
 * input ran out and there may be more; OVERFLOW means the output ran out and the caller should
 * drain it and call again; the two error states carry a length, in input units, of the piece
 * that went wrong.
 *
 * <p>UNDERFLOW and OVERFLOW are single shared instances, so {@code ==} against the constants is
 * the intended test and is what the coding loops themselves use.
 */
public class CoderResult {

    // The four states. Kept as a small int rather than four booleans so the predicates below are
    // one comparison each, and so `equals` is not needed at all -- underflow and overflow are
    // singletons and the error results are compared by their accessors.
    private static final int UNDERFLOW_KIND = 0;
    private static final int OVERFLOW_KIND = 1;
    private static final int MALFORMED_KIND = 2;
    private static final int UNMAPPABLE_KIND = 3;

    private final int kind;
    private final int len;

    private CoderResult(int kind, int len) {
        this.kind = kind;
        this.len = len;
    }

    /** The input ran out mid-way; feed more input, or pass {@code endOfInput} to finish. */
    public static final CoderResult UNDERFLOW = new CoderResult(CoderResult.UNDERFLOW_KIND, 0);

    /** The output buffer filled up; drain it and call the coder again. */
    public static final CoderResult OVERFLOW = new CoderResult(CoderResult.OVERFLOW_KIND, 0);

    // Results for the first few lengths, made once. Real inputs almost never produce an error
    // longer than four units -- the longest UTF-8 sequence is four bytes -- so this covers the
    // traffic without a map, and anything longer simply allocates.
    private static final CoderResult[] MALFORMED_CACHE = CoderResult.cacheFor(
            CoderResult.MALFORMED_KIND);
    private static final CoderResult[] UNMAPPABLE_CACHE = CoderResult.cacheFor(
            CoderResult.UNMAPPABLE_KIND);

    private static CoderResult[] cacheFor(int kind) {
        CoderResult[] out = new CoderResult[5];
        int i = 1;
        while (i < out.length) {
            out[i] = new CoderResult(kind, i);
            i = i + 1;
        }
        return out;
    }

    /**
     * A malformed-input result covering {@code length} input units.
     *
     * @param length how many input units are malformed; must be positive
     * @throws IllegalArgumentException if {@code length} is not positive
     */
    public static CoderResult malformedForLength(int length) {
        return CoderResult.forLength(CoderResult.MALFORMED_CACHE, CoderResult.MALFORMED_KIND,
                length);
    }

    /**
     * An unmappable-character result covering {@code length} input units.
     *
     * @param length how many input units cannot be mapped; must be positive
     * @throws IllegalArgumentException if {@code length} is not positive
     */
    public static CoderResult unmappableForLength(int length) {
        return CoderResult.forLength(CoderResult.UNMAPPABLE_CACHE, CoderResult.UNMAPPABLE_KIND,
                length);
    }

    private static CoderResult forLength(CoderResult[] cache, int kind, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Non-positive length");
        }
        if (length < cache.length) {
            return cache[length];
        }
        return new CoderResult(kind, length);
    }

    /** Whether the input ran out. */
    public boolean isUnderflow() {
        return this.kind == CoderResult.UNDERFLOW_KIND;
    }

    /** Whether the output buffer filled up. */
    public boolean isOverflow() {
        return this.kind == CoderResult.OVERFLOW_KIND;
    }

    /** Whether this is either kind of error — malformed input or an unmappable character. */
    public boolean isError() {
        return this.kind >= CoderResult.MALFORMED_KIND;
    }

    /** Whether the input was malformed. */
    public boolean isMalformed() {
        return this.kind == CoderResult.MALFORMED_KIND;
    }

    /** Whether the input was well-formed but has no representation in the target charset. */
    public boolean isUnmappable() {
        return this.kind == CoderResult.UNMAPPABLE_KIND;
    }

    /**
     * The length of the erroneous input, in input units.
     *
     * @throws UnsupportedOperationException if this is not an error result — underflow and
     *         overflow describe the buffers, not a piece of input, so there is no length to give
     */
    public int length() {
        if (!this.isError()) {
            throw new UnsupportedOperationException();
        }
        return this.len;
    }

    /**
     * Throws the exception this result stands for.
     *
     * <p>The bridge between the two ways of reporting a coding problem: the coder returns a
     * result, and the caller who would rather have an exception asks for one here.
     *
     * @throws CharacterCodingException the {@link MalformedInputException} or {@link
     *         UnmappableCharacterException} matching this result
     * @throws UnsupportedOperationException if this is underflow or overflow, which are not
     *         errors and have no exception to throw
     */
    public void throwException() throws CharacterCodingException {
        if (this.kind == CoderResult.MALFORMED_KIND) {
            throw new MalformedInputException(this.len);
        }
        if (this.kind == CoderResult.UNMAPPABLE_KIND) {
            throw new UnmappableCharacterException(this.len);
        }
        throw new UnsupportedOperationException(this.isUnderflow() ? "UNDERFLOW" : "OVERFLOW");
    }

    /** The state, with the length appended when there is one. */
    public String toString() {
        if (this.kind == CoderResult.UNDERFLOW_KIND) {
            return "UNDERFLOW";
        }
        if (this.kind == CoderResult.OVERFLOW_KIND) {
            return "OVERFLOW";
        }
        String head = this.kind == CoderResult.MALFORMED_KIND ? "MALFORMED" : "UNMAPPABLE";
        return head + "[" + this.len + "]";
    }
}
