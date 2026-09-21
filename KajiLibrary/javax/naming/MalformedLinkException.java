package javax.naming;

/**
 * Thrown when what is in the link is not a valid name. Among others, `LinkRef.getLinkName()`
 * throws it when the reference lacks the `LinkAddress` address it should have.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class MalformedLinkException extends LinkException {

    private static final long serialVersionUID = -3066740437737830242L;

    public MalformedLinkException(String explanation) {
        super(explanation);
    }

    public MalformedLinkException() {
        super();
    }
}
