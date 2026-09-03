package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.CredentialExpiredException -- la credencial vencio.
 *
 * <p>El caso tipico es la contrasena que hay que cambiar. Es <b>recuperable</b>: la aplicacion puede
 * atajarla y ofrecer el cambio en el momento, que es justamente para lo que sirve tenerla aparte de
 * {@link FailedLoginException}.
 */
public class CredentialExpiredException extends CredentialException {

    private static final long serialVersionUID = -5344739593859737937L;

    /** Sin detalle. */
    public CredentialExpiredException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public CredentialExpiredException(String msg) {
        super(msg);
    }
}
