package javax.security.sasl;

import java.io.IOException;

/**
 * KajiLibrary's javax.security.sasl.SaslException -- the SASL negotiation failed.
 *
 * <p>It extends {@link IOException} and not {@code Exception}, which is the design decision worth
 * looking at: SASL always goes inside a protocol --LDAP, IMAP, SMTP-- and whoever uses it is
 * already catching input/output errors. Hanging it from there saves each call from having two
 * {@code catch}es that do the same thing.
 *
 * <p>{@link #toString} is its own and adds the cause in brackets, instead of letting it appear only
 * in the stack trace. It is useful here: what fails in SASL is almost always the mechanism below,
 * and the message above says nothing on its own.
 */
public class SaslException extends IOException {

    private static final long serialVersionUID = 4579784287983423626L;

    /** Without detail. */
    public SaslException() {
        super();
    }

    /** With a message. */
    public SaslException(String detail) {
        super(detail);
    }

    /**
     * With the underlying cause.
     *
     * @param ex what really failed; null if there is nothing
     */
    public SaslException(String detail, Throwable ex) {
        super(detail);
        if (ex != null) {
            initCause(ex);
        }
    }

    /** The cause, or null. */
    public Throwable getCause() {
        return super.getCause();
    }

    /**
     * Sets the cause.
     *
     * @throws IllegalStateException if it already had one
     */
    public Throwable initCause(Throwable cause) {
        return super.initCause(cause);
    }

    /** With the cause in brackets if there is one; see the class note. */
    public String toString() {
        Throwable cause = getCause();
        String head = super.toString();
        if (cause == null) {
            return head;
        }
        return head + " [Caused by " + cause.toString() + "]";
    }
}
