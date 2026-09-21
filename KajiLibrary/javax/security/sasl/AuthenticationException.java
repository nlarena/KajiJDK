package javax.security.sasl;

/**
 * KajiLibrary's javax.security.sasl.AuthenticationException -- the credentials are no good.
 *
 * <p>It separates "I could not authenticate you" from "something went wrong on the way", which is
 * the only distinction that really matters when a negotiation fails: the first is not fixed by
 * retrying and the second maybe is.
 *
 * <p>The note the specification carries is worth repeating: a server should not send this
 * distinction to the client. Telling whoever tries to get in that the password was wrong --and not
 * that the user does not exist-- confirms that the user exists, which is half the work done for
 * someone probing names. This class is for the server side's log, not for the answer.
 */
public class AuthenticationException extends SaslException {

    private static final long serialVersionUID = -3579708765071815007L;

    /** Without detail. */
    public AuthenticationException() {
        super();
    }

    /** With a message. */
    public AuthenticationException(String detail) {
        super(detail);
    }

    /** With the underlying cause. */
    public AuthenticationException(String detail, Throwable ex) {
        super(detail, ex);
    }
}
