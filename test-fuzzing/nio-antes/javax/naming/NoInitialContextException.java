package javax.naming;

/**
 * Thrown when there is no initial provider that can serve the operation.
 *
 * <p>In this library it is the most likely exception of the whole package: **every** operation of
 * `InitialContext` ends here. In the JDK that happens only when no
 * `javax.naming.spi.InitialContextFactory` is configured; here it happens even when one is, because
 * `InitialContext` does not call `javax.naming.spi.NamingManager.getInitialContext`. (An earlier
 * note said this was the JDK's own behaviour under the same conditions; that only holds with no
 * factory configured.) See the `InitialContext` class header.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class NoInitialContextException extends NamingException {

    private static final long serialVersionUID = -3413733186901258623L;

    public NoInitialContextException(String explanation) {
        super(explanation);
    }

    public NoInitialContextException() {
        super();
    }
}
