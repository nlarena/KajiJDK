package javax.naming;

/**
 * Se lanza cuando falta un recurso **del lado del cliente** --memoria, descriptores-- para
 * completar la operacion. No confundir con `LimitExceededException`, que es un limite pactado y
 * no una falta de recursos.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class InsufficientResourcesException extends NamingException {

    private static final long serialVersionUID = 6227672693037844532L;

    public InsufficientResourcesException(String explanation) {
        super(explanation);
    }

    public InsufficientResourcesException() {
        super();
    }
}
