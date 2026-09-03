package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.AccountNotFoundException -- no hay tal cuenta.
 *
 * <p>Por lo de arriba, casi nunca conviene mostrarla tal cual: junto con
 * {@link AccountLockedException} deja distinguir cuentas que existen de las que no. Adentro del
 * sistema si vale la pena distinguirla, para los registros.
 */
public class AccountNotFoundException extends AccountException {

    private static final long serialVersionUID = 1498349563916294614L;

    /** Sin detalle. */
    public AccountNotFoundException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public AccountNotFoundException(String msg) {
        super(msg);
    }
}
