package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.AccountException -- something is wrong with the
 * <b>account</b>, not with what was presented.
 *
 * <p>It is the branch that separates "your data is fine but your account is not usable" from "your
 * data is wrong". The distinction matters when writing the message shown to the person: in the
 * first case retrying the password fixes nothing.
 */
public class AccountException extends LoginException {

    private static final long serialVersionUID = -2112878680733026008L;

    /** Without detail. */
    public AccountException() {
        super();
    }

    /** With a message that says what happened. */
    public AccountException(String msg) {
        super(msg);
    }
}
