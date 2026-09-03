package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.CredentialException -- algo pasa con lo que se presento.
 *
 * <p>La otra rama, en espejo con {@link AccountException}: aca la cuenta esta bien y el problema es la
 * credencial --contrasena, certificado, token--.
 */
public class CredentialException extends LoginException {

    private static final long serialVersionUID = -4772893876810601859L;

    /** Sin detalle. */
    public CredentialException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public CredentialException(String msg) {
        super(msg);
    }
}
