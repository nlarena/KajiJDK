package java.nio.charset;

/**
 * The input was not well-formed for its charset: bytes that no sequence of the encoding could
 * have produced, such as a UTF-8 continuation byte with no leading byte in front of it.
 *
 * <p>Distinct from {@link UnmappableCharacterException}, and the distinction is worth keeping:
 * malformed input means the bytes are broken, unmappable means the bytes are fine and this
 * charset simply has no room for what they say.
 */
public class MalformedInputException extends CharacterCodingException {

    private final int inputLength;

    /**
     * An instance reporting that {@code inputLength} units of input were malformed.
     *
     * @param inputLength how many input units the error covers
     */
    public MalformedInputException(int inputLength) {
        this.inputLength = inputLength;
    }

    /** How many input units were malformed. */
    public int getInputLength() {
        return this.inputLength;
    }

    /** The detail message, which names the length. */
    public String getMessage() {
        return "Input length = " + this.inputLength;
    }
}
