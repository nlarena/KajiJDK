package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.AccountExpiredException -- the account expired.
 *
 * <p>It usually comes from a rotation policy. Unlike {@link AccountLockedException}, it is solved
 * by renewing and not by waiting.
 */
public class AccountExpiredException extends AccountException {

    private static final long serialVersionUID = -6870589190242052883L;

    /** Without detail. */
    public AccountExpiredException() {
        super();
    }

    /** With a message that says what happened. */
    public AccountExpiredException(String msg) {
        super(msg);
    }
}
