package java.nio.charset;

/**
 * The input was well-formed but has no representation in the target charset — the classic case
 * being any character above U+00FF on its way into ISO-8859-1.
 *
 * <p>See {@link MalformedInputException} for the other half of the pair.
 */
public class UnmappableCharacterException extends CharacterCodingException {

    private final int inputLength;

    /**
     * An instance reporting that {@code inputLength} units of input could not be mapped.
     *
     * @param inputLength how many input units the error covers
     */
    public UnmappableCharacterException(int inputLength) {
        this.inputLength = inputLength;
    }

    /** How many input units could not be mapped. */
    public int getInputLength() {
        return this.inputLength;
    }

    /** The detail message, which names the length. */
    public String getMessage() {
        return "Input length = " + this.inputLength;
    }
}
