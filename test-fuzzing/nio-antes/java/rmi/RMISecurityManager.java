package java.rmi;

/**
 * KajiLibrary's java.rmi.RMISecurityManager -- a {@link SecurityManager} with nothing of its own.
 *
 * <p>History: up to 1.1 this class had a policy of its own for code that arrived over RMI. Since
 * 1.2 it adds nothing over {@code SecurityManager}, and since then using one or the other is
 * exactly the same.
 *
 * <p>Marked for removal along with the whole {@code SecurityManager} mechanism, which no longer
 * does anything. It is kept so old code compiles.
 */
@Deprecated(since = "1.8", forRemoval = true)
public class RMISecurityManager extends SecurityManager {

    /** The same as the base class's. */
    public RMISecurityManager() {
    }
}
