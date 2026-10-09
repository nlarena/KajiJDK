package java.beans.beancontext;

/**
 * Listens for new services appearing, as well as for their revocation.
 *
 * <p>It extends {@link BeanContextServiceRevokedListener} instead of repeating it: whoever wants to
 * hear about additions almost always wants to hear about removals too, and keeping them apart would
 * force registering two listeners to follow one service.
 */
public interface BeanContextServicesListener extends BeanContextServiceRevokedListener {

    /** A new service is available. */
    void serviceAvailable(BeanContextServiceAvailableEvent bcsae);
}
