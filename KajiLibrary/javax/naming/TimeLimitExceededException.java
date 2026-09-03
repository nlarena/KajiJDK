package javax.naming;

/**
 * Se lanza cuando la operacion tardo mas que el limite de tiempo. Distinta de
 * `InterruptedNamingException`: aca no interrumpio nadie, se cumplio un plazo.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class TimeLimitExceededException extends LimitExceededException {

    private static final long serialVersionUID = -3597009011385034696L;

    public TimeLimitExceededException(String explanation) {
        super(explanation);
    }

    public TimeLimitExceededException() {
        super();
    }
}
