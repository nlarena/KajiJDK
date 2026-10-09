package java.rmi.server;

/**
 * A remote object that wants to find out when no client references it any more.
 *
 * <h2>Why an explicit notice is needed</h2>
 *
 * <p>Because the local collector does not see remote references: to it, an exported object is alive
 * while the RMI runtime holds it. What keeps count of the clients is the distributed collector, and
 * this method is how it announces that the count reached zero — the moment to let go of whatever
 * the object was holding.
 *
 * <p>It is not a promise of finalisation: the object can be referenced again later if somebody kept
 * its stub, and then {@link #unreferenced} is called again further on.
 */
public interface Unreferenced {

    /** No client with a reference is left. */
    void unreferenced();
}
