package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.AccountLockedException -- the account is locked.
 *
 * <p>Almost always because of failed attempts. A detail worth keeping in mind when using it:
 * telling whoever tries to get in that the account is locked confirms that it <b>exists</b>, which
 * is useful information for someone probing names.
 */
public class AccountLockedException extends AccountException {

    private static final long serialVersionUID = 8280345554014066334L;

    /** Without detail. */
    public AccountLockedException() {
        super();
    }

    /** With a message that says what happened. */
    public AccountLockedException(String msg) {
        super(msg);
    }
}
