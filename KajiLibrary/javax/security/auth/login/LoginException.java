package javax.security.auth.login;

import java.security.GeneralSecurityException;

/**
 * KajiLibrary's javax.security.auth.login.LoginException -- la autenticacion fallo.
 *
 * <p>Es la raiz de una jerarquia deliberadamente <b>chata en lo que informa</b>: sus subclases
 * distinguen la cuenta de la credencial --{@code AccountExpiredException} contra
 * {@code CredentialExpiredException}-- pero ninguna dice "esa cuenta no existe" cuando el modulo
 * eligio no decirlo.
 *
 * <p>Eso no es una carencia del API: un servicio que conteste "usuario incorrecto" y "clave
 * incorrecta" por separado le regala a quien prueba una lista de usuarios validos. Por eso un modulo
 * de login prudente lanza {@code FailedLoginException} a secas en los dos casos, y el API le permite
 * ser tan preciso o tan reservado como quiera.
 *
 * <p>Extiende {@code GeneralSecurityException}, que es la raiz de {@code java.security}: una falla de
 * autenticacion es una falla de seguridad y se puede atrapar junto con las demas.
 */
public class LoginException extends GeneralSecurityException {

    private static final long serialVersionUID = -4679091624035232488L;

    public LoginException() {
        super();
    }

    public LoginException(String msg) {
        super(msg);
    }
}
