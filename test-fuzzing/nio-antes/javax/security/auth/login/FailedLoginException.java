package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.FailedLoginException -- the authentication did not go
 *
 * through.
 *
 * <p>It is the generic one, and on purpose: it is the one to show to the outside. The ones above
 * say <b>why</b> it failed, which is information that serves in the logs and that is almost never
 * wise to send to whoever is trying to get in.
 */
public class FailedLoginException extends LoginException {

    private static final long serialVersionUID = 802556922354616286L;

    /** Without detail. */
    public FailedLoginException() {
        super();
    }

    /** With a message that says what happened. */
    public FailedLoginException(String msg) {
        super(msg);
    }
}
