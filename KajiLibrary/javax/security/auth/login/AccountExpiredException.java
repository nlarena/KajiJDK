package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.AccountExpiredException -- la cuenta vencio.
 *
 * <p>Suele venir de una politica de rotacion. A diferencia de {@link AccountLockedException}, se
 * resuelve renovando y no esperando.
 */
public class AccountExpiredException extends AccountException {

    private static final long serialVersionUID = -6870589190242052883L;

    /** Sin detalle. */
    public AccountExpiredException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public AccountExpiredException(String msg) {
        super(msg);
    }
}
