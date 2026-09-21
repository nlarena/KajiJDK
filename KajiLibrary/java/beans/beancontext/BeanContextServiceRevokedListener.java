package java.beans.beancontext;

import java.util.EventListener;

/**
 * Listens for the revocation of a service.
 *
 * <p>Whoever requests a service with `getService` passes one of these, and through it learns that
 * the service stops being available. It is what keeps a user from holding on to something no longer
 * valid: without this notice, the only way to find out would be for it to fail when used.
 */
public interface BeanContextServiceRevokedListener extends EventListener {

    /** The service the event names was revoked. */
    void serviceRevoked(BeanContextServiceRevokedEvent bcsre);
}
