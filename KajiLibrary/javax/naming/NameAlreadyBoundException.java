package javax.naming;

/**
 * Thrown when trying to bind a name that is already bound. It is the reason `rebind` exists: `bind`
 * refuses to overwrite, `rebind` overwrites.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class NameAlreadyBoundException extends NamingException {

    private static final long serialVersionUID = -8491441000356780586L;

    public NameAlreadyBoundException(String explanation) {
        super(explanation);
    }

    public NameAlreadyBoundException() {
        super();
    }
}
