package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.CredentialExpiredException -- the credential expired.
 *
 * <p>The typical case is the password that has to be changed. It is <b>recoverable</b>: the
 * application can catch it and offer the change right then, which is precisely what having it apart
 * from {@link FailedLoginException} is for.
 */
public class CredentialExpiredException extends CredentialException {

    private static final long serialVersionUID = -5344739593859737937L;

    /** Without detail. */
    public CredentialExpiredException() {
        super();
    }

    /** With a message that says what happened. */
    public CredentialExpiredException(String msg) {
        super(msg);
    }
}
