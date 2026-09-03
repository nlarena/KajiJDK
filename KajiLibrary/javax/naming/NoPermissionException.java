package javax.naming;

/**
 * Se lanza cuando la identidad es valida y aun asi la operacion no esta permitida. Es la
 * diferencia entre "no se quien sos" y "se quien sos y no podes".
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class NoPermissionException extends NamingSecurityException {

    private static final long serialVersionUID = 8395332708699751775L;

    public NoPermissionException(String explanation) {
        super(explanation);
    }

    public NoPermissionException() {
        super();
    }
}
