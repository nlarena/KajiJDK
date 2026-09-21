package javax.naming;

/**
 * Abstract root of the three security failures, so they can be caught together.
 *
 * <p>It is `abstract` because "security problem" is not a cause: it is a **category**. Throwing it
 * as is would not tell whoever catches it whether credentials are missing, the mechanism is not
 * supported or they simply lack permission, which are three different reactions.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public abstract class NamingSecurityException extends NamingException {

    private static final long serialVersionUID = 5855287647294685775L;

    public NamingSecurityException(String explanation) {
        super(explanation);
    }

    public NamingSecurityException() {
        super();
    }
}
