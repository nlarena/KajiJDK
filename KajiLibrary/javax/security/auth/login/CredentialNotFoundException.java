package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.CredentialNotFoundException -- no se presento ninguna credencial.
 *
 * <p>No es lo mismo que presentar una mal: aca no hubo nada que verificar. Suele significar que el
 * manejador de callbacks no devolvio nada, o que el modulo esperaba algo que la aplicacion nunca
 * junto.
 */
public class CredentialNotFoundException extends CredentialException {

    private static final long serialVersionUID = -7779934467214319475L;

    /** Sin detalle. */
    public CredentialNotFoundException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public CredentialNotFoundException(String msg) {
        super(msg);
    }
}
