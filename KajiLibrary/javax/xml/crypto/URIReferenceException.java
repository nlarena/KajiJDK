package javax.xml.crypto;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.crypto.URIReferenceException -- a reference could not be resolved.
 *
 * <p>A {@link URIDereferencer} throws it. It is the only exception of the package that carries a
 * datum of its own: the {@link URIReference} that failed.
 *
 * <p>That datum is really needed. A signature has several references and they are resolved in a
 * loop; without knowing which one failed, the message would say "could not resolve" about a
 * signature with ten references and there would be nowhere to start.
 *
 * <p>It redefines {@code getCause} and the {@code printStackTrace}s for the same reason as the
 * other three of the package: in the JDK the cause lives in a field of its own, and here it
 * delegates to {@code Throwable}.
 */
public class URIReferenceException extends Exception {

    private static final long serialVersionUID = 7173469703932561419L;

    /** The one that failed, or null. */
    private URIReference uriReference;

    /** Without detail. */
    public URIReferenceException() {
        super();
    }

    /** With a message. */
    public URIReferenceException(String message) {
        super(message);
    }

    /** With a message and the underlying cause. */
    public URIReferenceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * With the reference that failed.
     *
     * @param uriReference which one it was; see the class note
     */
    public URIReferenceException(String message, Throwable cause, URIReference uriReference) {
        super(message, cause);
        this.uriReference = uriReference;
    }

    /** With the cause only. */
    public URIReferenceException(Throwable cause) {
        super(cause);
    }

    /** The reference that failed, or null if it was not given. */
    public URIReference getURIReference() {
        return this.uriReference;
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
