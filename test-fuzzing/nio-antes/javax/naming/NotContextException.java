package javax.naming;

/**
 * Thrown when resolution reached an object that is not a context while there was still name left
 * to resolve --or when a context operation was requested on something that is not one. Resolving
 * `a/b/c` where `a/b` is a file and not a directory gives this.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class NotContextException extends NamingException {

    private static final long serialVersionUID = 849752551644540417L;

    public NotContextException(String explanation) {
        super(explanation);
    }

    public NotContextException() {
        super();
    }
}
