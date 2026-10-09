package javax.xml.crypto;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.crypto.MarshalException -- a structure could not be written or read as
 * XML.
 *
 * <p>It comes out when converting between the object model and the document, in either direction.
 * It is different from a signature failure: here the problem is the <b>shape</b> of the XML, not
 * the cryptography. Confusing them sends one looking for a key problem where what there is is a
 * misplaced element.
 *
 * <p>It redefines {@code getCause} and the three {@code printStackTrace}s because in the JDK it
 * keeps its cause in a field of its own, from when {@code Throwable} did not have one yet. Here the
 * cause is {@code Throwable}'s and the redefinitions delegate: same behaviour, without two copies
 * of the datum.
 */
public class MarshalException extends Exception {

    private static final long serialVersionUID = -863185580789085695L;

    /** Without detail. */
    public MarshalException() {
        super();
    }

    /** With a message. */
    public MarshalException(String message) {
        super(message);
    }

    /** With a message and the underlying cause. */
    public MarshalException(String message, Throwable cause) {
        super(message, cause);
    }

    /** With the cause only; the message comes from its {@code toString}. */
    public MarshalException(Throwable cause) {
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
