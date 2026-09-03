package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.AccountException -- algo pasa con la <b>cuenta</b>, no con lo que se presento.
 *
 * <p>Es la rama que separa "tus datos estan bien pero tu cuenta no sirve" de "tus datos estan mal".
 * La distincion importa al escribir el mensaje que se le muestra a la persona: en el primer caso
 * reintentar la contrasena no arregla nada.
 */
public class AccountException extends LoginException {

    private static final long serialVersionUID = -2112878680733026008L;

    /** Sin detalle. */
    public AccountException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public AccountException(String msg) {
        super(msg);
    }
}
