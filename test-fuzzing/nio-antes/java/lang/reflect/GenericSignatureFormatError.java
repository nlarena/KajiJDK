package java.lang.reflect;

/**
 * Thrown when a generic signature in a class file does not parse.
 *
 * <p>It is an {@link Error}, not an exception, and the distinction is deliberate: a syntactically
 * broken {@code Signature} attribute means the class file itself is malformed, which no reasonable
 * caller can recover from. Contrast {@link MalformedParameterizedTypeException}, which is thrown
 * when the signature parses but means something impossible.
 */
// Under ClassFormatError: a malformed generic signature is a malformed class file, and whoever
// catches the second expects to catch the first.
public class GenericSignatureFormatError extends ClassFormatError {

    /**
     * Creates an error with no detail message.
     */
    public GenericSignatureFormatError() {
        super();
    }

    /**
     * Creates an error with the given detail message.
     *
     * @param message the detail message
     */
    public GenericSignatureFormatError(String message) {
        super(message);
    }
}
