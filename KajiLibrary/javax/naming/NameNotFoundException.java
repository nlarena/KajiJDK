package javax.naming;

/**
 * Thrown when the component being resolved is not bound to anything. The exception's resolved and
 * remaining names say exactly where it stopped.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class NameNotFoundException extends NamingException {

    private static final long serialVersionUID = -8007156725367842053L;

    public NameNotFoundException(String explanation) {
        super(explanation);
    }

    public NameNotFoundException() {
        super();
    }
}
