package java.beans.beancontext;

import java.util.Iterator;
import java.util.TooManyListenersException;

/**
 * A {@link BeanContext} that also hands out **services**.
 *
 * <p>A service is an object identified by its class that the context obtains for whoever asks. The
 * difference from simply instantiating it is the lookup: if this context does not have it, the
 * request **climbs the hierarchy**, so a deeply nested child can use something the root registered
 * without knowing where it is.
 *
 * <p>It extends {@link BeanContextServicesListener} because a nested context is at the same time a
 * listener of its parent: that is how it learns of the services that appear further up.
 */
public interface BeanContextServices extends BeanContext, BeanContextServicesListener {

    /** Registers a provider for that service class. `false` if there already was one. */
    boolean addService(Class serviceClass, BeanContextServiceProvider serviceProvider);

    /**
     * Revokes that service.
     *
     * @param revokeCurrentServicesNow whether the instances already handed out must be invalidated
     *     too
     */
    void revokeService(Class serviceClass, BeanContextServiceProvider serviceProvider,
            boolean revokeCurrentServicesNow);

    /** Whether the service is available here or further up. */
    boolean hasService(Class serviceClass);

    /**
     * An instance of the service for that child.
     *
     * @throws TooManyListenersException if the revocation listener could not be registered
     */
    Object getService(BeanContextChild child, Object requestor, Class serviceClass,
            Object serviceSelector, BeanContextServiceRevokedListener bcsrl)
            throws TooManyListenersException;

    /** The child no longer needs that instance. */
    void releaseService(BeanContextChild child, Object requestor, Object service);

    /** The available service classes. */
    Iterator getCurrentServiceClasses();

    /** The selectors that service accepts, or `null` if it uses no selectors. */
    Iterator getCurrentServiceSelectors(Class serviceClass);

    /** Registers a listener for services being added and revoked. */
    void addBeanContextServicesListener(BeanContextServicesListener bcsl);

    /** Removes it. */
    void removeBeanContextServicesListener(BeanContextServicesListener bcsl);
}
