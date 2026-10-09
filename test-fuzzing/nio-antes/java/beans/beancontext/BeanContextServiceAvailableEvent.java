package java.beans.beancontext;

import java.util.Iterator;

/** A new service is available in a {@link BeanContextServices}. */
public class BeanContextServiceAvailableEvent extends BeanContextEvent {

    /** The class of the service that appeared. */
    protected Class serviceClass;

    /** The event for the service of that class. */
    public BeanContextServiceAvailableEvent(BeanContextServices bcs, Class sc) {
        super((BeanContext) bcs);
        this.serviceClass = sc;
    }

    /** The context announcing it. */
    public BeanContextServices getSourceAsBeanContextServices() {
        return (BeanContextServices) this.getBeanContext();
    }

    /** The service class. */
    public Class getServiceClass() {
        return this.serviceClass;
    }

    /**
     * The selectors the service accepts, or `null` if it uses no selectors.
     *
     * <p>The context is asked at the moment of the query and nothing is stored in the event:
     * between the service being announced and someone looking at the event, the provider may have
     * changed what it accepts, and a stale copy would be worse than none.
     */
    public Iterator getCurrentServiceSelectors() {
        return this.getSourceAsBeanContextServices().getCurrentServiceSelectors(this.serviceClass);
    }
}
