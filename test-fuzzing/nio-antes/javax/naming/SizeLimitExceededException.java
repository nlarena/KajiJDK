package javax.naming;

/**
 * Thrown when the result had more entries than a size limit allows --a count limit the caller
 * asked for (such as `SearchControls.setCountLimit`), or the server's own cap. `Context.BATCHSIZE`
 * is not one of them: it is only a hint on how many results to fetch per round trip. (An earlier
 * note named it as a size limit.)
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
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
