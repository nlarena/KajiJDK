package java.beans.beancontext;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.Serializable;

/**
 * The reusable implementation of {@link BeanContextChild}.
 *
 * <p>It serves two ways, which is why it has two constructors: a class **extends** it, or a bean
 * that already extends something else uses it by **delegation**, passing itself to the constructor.
 * In the second case {@link #isDelegated} is `true` and every event goes out in the name of the
 * delegating bean, not of this support — which is what a listener expects to see.
 *
 * <h2>The veto on moving, and `rejectedSetBCOnce`</h2>
 *
 * <p>Changing context is a **vetoable** property: before moving, the question is asked, and if
 * anyone objects the move does not happen. The part that reads badly is the `rejectedSetBCOnce`
 * flag, and it deserves the paragraph:
 *
 * <p>A context that expels a child calls `setBeanContext(null)`. A child that could veto that every
 * time could never be removed. So the veto is honoured **once**: the first refusal stands, and the
 * next call goes through without asking. This note said that only "the same change" attempted again
 * goes through. The flag does not remember which change was refused, so the next `setBeanContext`
 * —whatever context it names— skips both {@link #validatePendingSetBeanContext} and the veto
 * listeners. The flag is cleared as soon as a move completes.
 */
public class BeanContextChildSupport implements BeanContextChild, BeanContextServicesListener,
        Serializable {

    /** The bean events go out in the name of: this one, or the one that delegated to it. */
    public BeanContextChild beanContextChildPeer;

    /** The property change listeners. */
    protected PropertyChangeSupport pcSupport;

    /** The veto listeners. */
    protected VetoableChangeSupport vcSupport;

    /** The current context, or `null`. */
    protected transient BeanContext beanContext;

    /** Whether a context change has already been refused once. See the class note. */
    protected transient boolean rejectedSetBCOnce;

    // The context being moved to while the veto is asked. Not the same as `beanContext`: during the
    // query the child is still in the old one, and if anyone vetoes it stays there. Nothing reads
    // this field; it is only set before the veto query and cleared after it.
    private transient BeanContext pendingContext;

    /** A support that acts in its own name. */
    public BeanContextChildSupport() {
        this.beanContextChildPeer = this;
        this.pcSupport = new PropertyChangeSupport(this.beanContextChildPeer);
        this.vcSupport = new VetoableChangeSupport(this.beanContextChildPeer);
    }

    /** A support that acts in the name of `bcc`. `null` means in its own name. */
    public BeanContextChildSupport(BeanContextChild bcc) {
        this.beanContextChildPeer = bcc == null ? this : bcc;
        this.pcSupport = new PropertyChangeSupport(this.beanContextChildPeer);
        this.vcSupport = new VetoableChangeSupport(this.beanContextChildPeer);
    }

    /** The bean it acts in the name of. */
    public BeanContextChild getBeanContextChildPeer() {
        return this.beanContextChildPeer;
    }

    /** Whether it acts in the name of another bean instead of its own. */
    public boolean isDelegated() {
        return this.beanContextChildPeer != this;
    }

    /**
     * Moves this child to that context.
     *
     * <p>The order matters and is the one the contract sets: first the veto is asked, then the old
     * context's resources are released, then the context changes, then the change is announced, and
     * only last are the new context's resources taken. Releasing before asking would leave the
     * child without resources if someone vetoed.
     *
     * @throws PropertyVetoException if {@link #validatePendingSetBeanContext} or a listener objects
     *     and no change had been refused since the last completed move
     */
    public synchronized void setBeanContext(BeanContext bc) throws PropertyVetoException {
        if (bc == this.beanContext) {
            return;
        }
        BeanContext old = this.beanContext;
        if (!this.rejectedSetBCOnce) {
            if (!this.validatePendingSetBeanContext(bc)) {
                this.rejectedSetBCOnce = true;
                throw new PropertyVetoException("the child refuses the context change",
                        new java.beans.PropertyChangeEvent(this.beanContextChildPeer,
                                "beanContext", old, bc));
            }
            try {
                this.pendingContext = bc;
                this.fireVetoableChange("beanContext", old, bc);
            } catch (PropertyVetoException e) {
                this.rejectedSetBCOnce = true;
                this.pendingContext = null;
                throw e;
            }
        }
        if (old != null) {
            this.releaseBeanContextResources();
        }
        this.beanContext = bc;
        this.pendingContext = null;
        this.rejectedSetBCOnce = false;
        this.firePropertyChange("beanContext", old, bc);
        if (bc != null) {
            this.initializeBeanContextResources();
        }
    }

    /** The current context, or `null`. */
    public synchronized BeanContext getBeanContext() {
        return this.beanContext;
    }

    /**
     * The subclass's chance to refuse a move without registering a listener.
     *
     * <p>By default it accepts everything. Overriding it is the cheapest option for a child that
     * can only live in a certain kind of context.
     */
    public boolean validatePendingSetBeanContext(BeanContext newValue) {
        return true;
    }

    /**
     * The hook for taking the new context's resources.
     *
     * <p>Empty here rather than abstract: most children need none, and forcing them to write an
     * empty method would be noise. It is called **after** the context has changed, so {@link
     * #getBeanContext} can be used inside.
     */
    protected void initializeBeanContextResources() {
    }

    /**
     * The hook for releasing them. Called **before** the change, with the old context still set.
     */
    protected void releaseBeanContextResources() {
    }

    /** Registers a listener for changes to that property. */
    public void addPropertyChangeListener(String name, PropertyChangeListener pcl) {
        this.pcSupport.addPropertyChangeListener(name, pcl);
    }

    /** Removes it. */
    public void removePropertyChangeListener(String name, PropertyChangeListener pcl) {
        this.pcSupport.removePropertyChangeListener(name, pcl);
    }

    /** Registers a listener that can veto changes to that property. */
    public void addVetoableChangeListener(String name, VetoableChangeListener vcl) {
        this.vcSupport.addVetoableChangeListener(name, vcl);
    }

    /** Removes it. */
    public void removeVetoableChangeListener(String name, VetoableChangeListener vcl) {
        this.vcSupport.removeVetoableChangeListener(name, vcl);
    }

    /** Announces a property change in the bean's name. */
    public void firePropertyChange(String name, Object oldValue, Object newValue) {
        this.pcSupport.firePropertyChange(name, oldValue, newValue);
    }

    /**
     * Asks for vetoes on a property change.
     *
     * @throws PropertyVetoException if some listener objects
     */
    public void fireVetoableChange(String name, Object oldValue, Object newValue)
            throws PropertyVetoException {
        this.vcSupport.fireVetoableChange(name, oldValue, newValue);
    }

    /**
     * A new service appeared.
     *
     * <p>Empty by default, and rightly so: a child that uses no services has nothing to do here,
     * and this class exists precisely so it does not have to write it.
     */
    public void serviceAvailable(BeanContextServiceAvailableEvent bcsae) {
    }

    /** A service was revoked. Empty by default, for the same reason. */
    public void serviceRevoked(BeanContextServiceRevokedEvent bcsre) {
    }
}
