package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.CredentialException -- something is wrong with what was
 *
 * presented.
 *
 * <p>The other branch, mirroring {@link AccountException}: here the account is fine and the problem
 * is the credential --password, certificate, token--.
 */
public class CredentialException extends LoginException {

    private static final long serialVersionUID = -4772893876810601859L;

    /** Without detail. */
    public CredentialException() {
        super();
    }

    /** With a message that says what happened. */
    public CredentialException(String msg) {
        super(msg);
    }
}
