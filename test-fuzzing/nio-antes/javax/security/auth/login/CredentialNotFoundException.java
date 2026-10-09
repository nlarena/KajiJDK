package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.CredentialNotFoundException -- no credential was
 * presented.
 *
 * <p>It is not the same as presenting a wrong one: here there was nothing to verify. It usually
 * means the callback handler returned nothing, or that the module expected something the
 * application never collected.
 */
public class CredentialNotFoundException extends CredentialException {

    private static final long serialVersionUID = -7779934467214319475L;

    /** Without detail. */
    public CredentialNotFoundException() {
        super();
    }

    /** With a message that says what happened. */
    public CredentialNotFoundException(String msg) {
        super(msg);
    }
}
