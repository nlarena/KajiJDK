package javax.naming;

/**
 * Se lanza cuando el mecanismo de autenticacion pedido no lo soporta el servicio --o no lo
 * soporta para **esta** operacion--. A diferencia de `AuthenticationException`, las credenciales
 * pueden estar perfectas: lo que no se acepta es la forma de presentarlas.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class AuthenticationNotSupportedException extends NamingSecurityException {

    private static final long serialVersionUID = -7149033933259492300L;

    public AuthenticationNotSupportedException(String explanation) {
        super(explanation);
    }

    public AuthenticationNotSupportedException() {
        super();
    }
}
