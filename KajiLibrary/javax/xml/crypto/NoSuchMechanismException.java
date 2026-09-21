package javax.xml.crypto;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.crypto.NoSuchMechanismException -- there is no implementation for that
 * mechanism.
 *
 * <p>The only <b>unchecked</b> one of the package, and rightly so: asking for a mechanism the
 * platform does not have is a deployment error, not a condition the program can handle. It is the
 * same decision {@code NoSuchAlgorithmException} does not make --that one is checked-- and the
 * difference shows when writing a {@code getInstance}: here there is nothing to catch.
 *
 * <p>It redefines {@code getCause} and the three {@code printStackTrace}s because in the JDK it
 * keeps its cause in a field of its own, from when {@code Throwable} did not have one yet. Here the
 * cause is {@code Throwable}'s and the redefinitions delegate: same behaviour, without two copies
 * of the datum.
 */
public class NoSuchMechanismException extends RuntimeException {

    private static final long serialVersionUID = 4189669069570660166L;

    /** Without detail. */
    public NoSuchMechanismException() {
        super();
    }

    /** With a message. */
    public NoSuchMechanismException(String message) {
        super(message);
    }

    /** With a message and the underlying cause. */
    public NoSuchMechanismException(String message, Throwable cause) {
        super(message, cause);
    }

    /** With the cause only; the message comes from its {@code toString}. */
    public NoSuchMechanismException(Throwable cause) {
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
