package javax.xml.crypto.dsig;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.crypto.dsig.XMLSignatureException -- it could not be signed or validated.
 *
 * <p>The operation's generic one. It covers from a key that does not suit the algorithm to a
 * provider error.
 *
 * <p>What it does <b>not</b> mean is that the signature is invalid: {@link XMLSignature#validate}
 * says that by returning false. Confusing the two --treating the exception as a rejection of the
 * signature-- hides configuration errors, and the other way round is worse.
 *
 * <p>It redefines {@code getCause} and the three {@code printStackTrace}s for the same reason as
 * the ones in {@code javax.xml.crypto}: in the JDK the cause lives in a field of its own, from
 * before {@code Throwable} had one, and here it delegates.
 */
public class XMLSignatureException extends Exception {

    private static final long serialVersionUID = -9077726597114319058L;

    /** Without detail. */
    public XMLSignatureException() {
        super();
    }

    /** With a message. */
    public XMLSignatureException(String message) {
        super(message);
    }

    /** With a message and the underlying cause. */
    public XMLSignatureException(String message, Throwable cause) {
        super(message, cause);
    }

    /** With the cause only. */
    public XMLSignatureException(Throwable cause) {
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
