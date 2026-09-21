package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.AccountNotFoundException -- there is no such account.
 *
 * <p>Because of the above, it is almost never wise to show it as is: together with
 * {@link AccountLockedException} it lets accounts that exist be told from those that do not. Inside
 * the system it is worth telling apart, for the logs.
 */
public class AccountNotFoundException extends AccountException {

    private static final long serialVersionUID = 1498349563916294614L;

    /** Without detail. */
    public AccountNotFoundException() {
        super();
    }

    /** With a message that says what happened. */
    public AccountNotFoundException(String msg) {
        super(msg);
    }
}
