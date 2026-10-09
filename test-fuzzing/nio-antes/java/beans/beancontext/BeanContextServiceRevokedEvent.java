package java.beans.beancontext;

/**
 * A service stops being available.
 *
 * <p>{@link #isCurrentServiceInvalidNow} is the part that matters and the one that gets misread: it
 * separates *"do not ask for more"* from *"what you are holding no longer works"*. In the first
 * case the user can keep using its instance until done; in the second it has to let go at once.
 * Treating both alike means either losing half-done work or using a dead object.
 */
public class BeanContextServiceRevokedEvent extends BeanContextEvent {

    /** The class of the revoked service. */
    protected Class serviceClass;

    private final boolean invalidateRefs;

    /** The revocation event for that service class. */
    public BeanContextServiceRevokedEvent(BeanContextServices bcs, Class sc,
            boolean invalidate) {
        super((BeanContext) bcs);
        this.serviceClass = sc;
        this.invalidateRefs = invalidate;
    }

    /** The context revoking it. */
    public BeanContextServices getSourceAsBeanContextServices() {
        return (BeanContextServices) this.getBeanContext();
    }

    /** The service class. */
    public Class getServiceClass() {
        return this.serviceClass;
    }

    /** Whether that class is the revoked service's. */
    public boolean isServiceClass(Class service) {
        return this.serviceClass.equals(service);
    }

    /** Whether the instances already handed out stop being valid right now. */
    public boolean isCurrentServiceInvalidNow() {
        return this.invalidateRefs;
    }
}
