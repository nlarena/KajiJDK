package javax.naming;

/**
 * Se lanza cuando el resultado tenia mas entradas de las que el limite de tamano permite --el
 * `BATCHSIZE` del cliente, o el tope del servidor--.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class SizeLimitExceededException extends LimitExceededException {

    private static final long serialVersionUID = 7129289564879168579L;

    public SizeLimitExceededException(String explanation) {
        super(explanation);
    }

    public SizeLimitExceededException() {
        super();
    }
}
