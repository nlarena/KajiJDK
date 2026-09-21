package javax.xml.crypto;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.crypto.KeySelectorException -- the selector could not choose a key.
 *
 * <p>It is checked because not being able to choose a key is an <b>expectable</b> result of
 * validating somebody else's signature: the {@code KeyInfo} names a key that is not known, or names
 * none. Whoever validates has to decide what to do, and the compiler makes them look at it.
 *
 * <p>It redefines {@code getCause} and the three {@code printStackTrace}s because in the JDK it
 * keeps its cause in a field of its own, from when {@code Throwable} did not have one yet. Here the
 * cause is {@code Throwable}'s and the redefinitions delegate: same behaviour, without two copies
 * of the datum.
 */
public class KeySelectorException extends Exception {

    private static final long serialVersionUID = -7155660112864185370L;

    /** Without detail. */
    public KeySelectorException() {
        super();
    }

    /** With a message. */
    public KeySelectorException(String message) {
        super(message);
    }

    /** With a message and the underlying cause. */
    public KeySelectorException(String message, Throwable cause) {
        super(message, cause);
    }

    /** With the cause only; the message comes from its {@code toString}. */
    public KeySelectorException(Throwable cause) {
        super(cause);
    }

    /** The cause, or null. */
    public Throwable getCause() {
        return super.getCause();
    }

    /** To standard error. */
    public void printStackTrace() {
        super.printStackTrace();
    }

    /** To that stream. */
    public void printStackTrace(PrintStream s) {
        super.printStackTrace(s);
    }

    /** To that writer. */
    public void printStackTrace(PrintWriter s) {
        super.printStackTrace(s);
    }
}
