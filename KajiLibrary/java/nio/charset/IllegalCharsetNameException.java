package java.nio.charset;

/**
 * A string was offered as a charset name and is not shaped like one.
 *
 * <p>Unchecked, unlike the coding exceptions: a name that breaks the syntax rules is a constant
 * in the program far more often than it is data, so this is a bug and not bad input.
 */
public class IllegalCharsetNameException extends IllegalArgumentException {

    private final String charsetName;

    /**
     * An instance naming the offending string.
     *
     * @param charsetName the name that was rejected; may be null
     */
    public IllegalCharsetNameException(String charsetName) {
        super(String.valueOf(charsetName));
        this.charsetName = charsetName;
    }

    /** The name that was rejected. */
    public String getCharsetName() {
        return this.charsetName;
    }
}
