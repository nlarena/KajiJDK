package javax.naming;

/**
 * Se lanza cuando el hilo fue interrumpido mientras esperaba a que la operacion terminara. La
 * operacion queda en estado indefinido: puede haberse hecho o no.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class InterruptedNamingException extends NamingException {

    private static final long serialVersionUID = 6404516648893194728L;

    public InterruptedNamingException(String explanation) {
        super(explanation);
    }

    public InterruptedNamingException() {
        super();
    }
}
