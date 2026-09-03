package javax.naming;

/**
 * Se lanza cuando el proveedor no implementa la operacion pedida. `Context` es una interfaz
 * grande y hay servicios que son de solo lectura o que no tienen subcontextos; esta es la manera
 * pactada de decirlo.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class OperationNotSupportedException extends NamingException {

    private static final long serialVersionUID = 5493232822427682064L;

    public OperationNotSupportedException(String explanation) {
        super(explanation);
    }

    public OperationNotSupportedException() {
        super();
    }
}
