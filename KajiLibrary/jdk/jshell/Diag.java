package jdk.jshell;

import java.util.Locale;

/**
 * An error or a warning about a snippet.
 *
 * <h2>The positions</h2>
 *
 * <p>They refer to the text the user wrote, not to the code the interpreter builds around it in
 * order to compile it. Translating from one to the other is the interpreter's job, and it is what
 * makes the underline fall where the user expects.
 *
 * <p>{@link #getStartPosition} and {@link #getEndPosition} delimit what has to be underlined;
 * {@link #getPosition} is where to put the caret, which is usually inside but not at the edge. Any
 * of the three may be {@link #NOPOS} when the problem does not belong to a particular place.
 *
 * <h2>{@link #getCode}</h2>
 *
 * <p>It is the message's key, not the message. It is for recognizing a diagnostic without depending
 * on the language: a program may want to treat a type error differently from a syntax error, and
 * comparing the translated text would be fragile.
 *
 * @since 9
 */
public abstract class Diag {

    /** That there is no position. */
    public static final long NOPOS = -1;

    Diag() {
    }

    /**
     * Whether it is an error and not a warning.
     *
     * @return true if it is an error
     */
    public abstract boolean isError();

    /**
     * Where to put the caret.
     *
     * @return the position, or {@link #NOPOS}
     */
    public abstract long getPosition();

    /**
     * Where what has to be underlined starts.
     *
     * @return the position, or {@link #NOPOS}
     */
    public abstract long getStartPosition();

    /**
     * Where what has to be underlined ends.
     *
     * @return the position, or {@link #NOPOS}
     */
    public abstract long getEndPosition();

    /**
     * The message's key, for recognizing it without depending on the language.
     *
     * @return the key
     */
    public abstract String getCode();

    /**
     * The message, to show.
     *
     * @param locale in which language, or {@code null} for the machine's
     * @return the message
     */
    public abstract String getMessage(Locale locale);
}
