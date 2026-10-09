package javax.xml.xpath;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.xpath.XPathException -- the root of the XPath errors.
 *
 * <p>Two constructors and neither accepts null: one takes a message and the other a cause, and the
 * one that receives null throws {@link NullPointerException} right away. It is stricter than usual,
 * and rightly so -- an exception with neither message nor cause is of no use to anyone, and the
 * cost of finding that out is a stack trace that says nothing.
 *
 * <p>The cause constructor leaves the message as the cause's {@code toString}. That is why there is
 * no constructor taking both: if you have the cause, the message comes from there, and if you want
 * your own, the way is the message one plus {@code initCause}.
 *
 * <p>It redefines {@code getCause} and the three {@code printStackTrace} methods to match the JDK's
 * declarations. The cause is {@code Throwable}'s, as it is in the JDK 25 sources too (the only
 * {@code cause} of its own there is a serial field). An earlier note said the redefinitions only
 * delegate and the observable behaviour is the same; the JDK's {@code printStackTrace(PrintStream)}
 * prints the cause's trace first, then a {@code "--------------- linked to ------------------"}
 * line, then its own trace (which again ends with the cause). Here all three just call
 * {@code super}, so the output is the ordinary {@code Throwable} trace with a {@code Caused by:}
 * section.
 */
public class XPathException extends Exception {

    private static final long serialVersionUID = -1837080260374986980L;

    /**
     * With a message.
     *
     * @throws NullPointerException if it is null; see the class note
     */
    public XPathException(String message) {
        super(message);
        if (message == null) {
            throw new NullPointerException("message can't be null");
        }
    }

    /**
     * With a cause; the message comes from its {@code toString}.
     *
     * @throws NullPointerException if it is null
     */
    public XPathException(Throwable cause) {
        super(cause);
        if (cause == null) {
            throw new NullPointerException("cause can't be null");
        }
    }

    /** The cause, or null if it was built with a message and nobody set one. */
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
