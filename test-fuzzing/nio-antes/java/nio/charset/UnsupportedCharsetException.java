package java.nio.charset;

/**
 * A charset name is well-formed but no charset with that name is available here.
 *
 * <p>The one to catch when a name arrives from outside the program. {@link
 * Charset#isSupported} answers the same question without throwing.
 */
public class UnsupportedCharsetException extends IllegalArgumentException {

    private final String charsetName;

    /**
     * An instance naming the charset that is not available.
     *
     * @param charsetName the name that could not be resolved
     */
    public UnsupportedCharsetException(String charsetName) {
        super(String.valueOf(charsetName));
        this.charsetName = charsetName;
    }

    /** The name that could not be resolved. */
    public String getCharsetName() {
        return this.charsetName;
    }
}
