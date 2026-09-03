package javax.naming;

/**
 * Se lanza cuando el servicio de nombres rechazo la identidad: credenciales mal, vencidas o
 * ausentes. Reintentar con las mismas no sirve; hay que conseguir otras.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class AuthenticationException extends NamingSecurityException {

    private static final long serialVersionUID = 3678497619904568096L;

    public AuthenticationException(String explanation) {
        super(explanation);
    }

    public AuthenticationException() {
        super();
    }
}
