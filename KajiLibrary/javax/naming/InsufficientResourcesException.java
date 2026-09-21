package javax.naming;

/**
 * Thrown when a resource **on the client side** --memory, descriptors-- is missing to complete
 * the operation. Not to be confused with `LimitExceededException`, which is an agreed limit and
 * not a lack of resources.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
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
