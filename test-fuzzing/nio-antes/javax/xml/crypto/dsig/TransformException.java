package javax.xml.crypto.dsig;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.crypto.dsig.TransformException -- a transform failed.
 *
 * <p>It comes out of {@link Transform#transform}. What failed is the <b>path</b> between the datum
 * and its digest: a badly written XPath expression, a stylesheet that throws, data of a type the
 * transform does not accept.
 *
 * <p>It is not a cryptographic failure. Telling it apart from {@link XMLSignatureException} matters
 * when diagnosing: here the signature was not even compared.
 *
 * <p>It redefines {@code getCause} and the three {@code printStackTrace}s for the same reason as
 * the ones in {@code javax.xml.crypto}: in the JDK the cause lives in a field of its own, from
 * before {@code Throwable} had one, and here it delegates.
 */
public class TransformException extends Exception {

    private static final long serialVersionUID = 526117000366604532L;

    /** Without detail. */
    public TransformException() {
        super();
    }

    /** With a message. */
    public TransformException(String message) {
        super(message);
    }

    /** With a message and the underlying cause. */
    public TransformException(String message, Throwable cause) {
        super(message, cause);
    }

    /** With the cause only. */
    public TransformException(Throwable cause) {
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
