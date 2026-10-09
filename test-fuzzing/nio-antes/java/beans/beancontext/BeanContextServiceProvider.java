package java.beans.beancontext;

import java.util.Iterator;

/**
 * What makes the instances of a service.
 *
 * <p>The context does not implement services: it registers them and delegates. A provider signs up
 * with `addService(Class, BeanContextServiceProvider)` and from then on serves requests for that
 * class.
 *
 * <p>The `requestor` that `getService` and `releaseService` receive **is not decorative**: the
 * provider can return different instances depending on who asks, and `releaseService` needs to know
 * who was given what. (This note said all three methods receive it; `getCurrentServiceSelectors`
 * does not.) A provider that ignores it cannot tell requestors apart when they release.
 */
public interface BeanContextServiceProvider {

    /**
     * An instance of the service for that requestor.
     *
     * @param serviceSelector a service parameter, or `null` if it takes none
     * @return the instance, or `null` if one cannot be given
     */
    Object getService(BeanContextServices bcs, Object requestor, Class serviceClass,
            Object serviceSelector);

    /** The requestor no longer needs that instance. */
    void releaseService(BeanContextServices bcs, Object requestor, Object service);

    /** The selectors this provider accepts for that class, or `null` if it uses no selectors. */
    Iterator getCurrentServiceSelectors(BeanContextServices bcs, Class serviceClass);
}
