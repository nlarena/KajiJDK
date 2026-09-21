package javax.naming;

/**
 * Thrown when the operation took longer than the time limit. Different from
 * `InterruptedNamingException`: here nobody interrupted, a deadline was met.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
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
