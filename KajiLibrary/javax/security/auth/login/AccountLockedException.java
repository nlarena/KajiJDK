package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.AccountLockedException -- la cuenta esta bloqueada.
 *
 * <p>Casi siempre por intentos fallidos. Un detalle que conviene tener presente al usarla: decirle a
 * quien intenta entrar que la cuenta esta bloqueada le confirma que <b>existe</b>, que es
 * informacion util para quien esta probando nombres.
 */
public class AccountLockedException extends AccountException {

    private static final long serialVersionUID = 8280345554014066334L;

    /** Sin detalle. */
    public AccountLockedException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public AccountLockedException(String msg) {
        super(msg);
    }
}
