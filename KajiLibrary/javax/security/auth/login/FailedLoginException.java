package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.FailedLoginException -- la autenticacion no cerro.
 *
 * <p>Es la generica, y a proposito: es la que hay que mostrar hacia afuera. Las de arriba dicen
 * <b>por que</b> fallo, que es informacion que sirve en los registros y que casi nunca conviene
 * mandarle a quien esta intentando entrar.
 */
public class FailedLoginException extends LoginException {

    private static final long serialVersionUID = 802556922354616286L;

    /** Sin detalle. */
    public FailedLoginException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public FailedLoginException(String msg) {
        super(msg);
    }
}
