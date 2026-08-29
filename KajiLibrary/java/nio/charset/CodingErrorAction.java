package java.nio.charset;

/**
 * What a coder does when the input is malformed or cannot be mapped: drop it, substitute it,
 * or refuse.
 *
 * <p>A type-safe enumeration of three instances and not an {@code enum}, matching the JDK,
 * where the class predates enums by a release. The three are compared by identity, so a
 * {@code ==} against the constants is the intended test.
 */
public class CodingErrorAction {

    private final String name;

    private CodingErrorAction(String name) {
        this.name = name;
    }

    /** Drop the offending input and carry on, silently. */
    public static final CodingErrorAction IGNORE = new CodingErrorAction("IGNORE");

    /**
     * Replace the offending input with the coder's replacement value and carry on.
     *
     * <p>The forgiving one, and the default for the convenience methods on {@link Charset} and
     * on {@link String} — which is why {@code new String(bytes, UTF_8)} never throws and quietly
     * yields U+FFFD where the bytes were broken.
     */
    public static final CodingErrorAction REPLACE = new CodingErrorAction("REPLACE");

    /**
     * Refuse: return an error {@link CoderResult}, which the convenience methods turn into a
     * {@link CharacterCodingException}.
     */
    public static final CodingErrorAction REPORT = new CodingErrorAction("REPORT");

    /** The name of this action. */
    public String toString() {
        return this.name;
    }
}
