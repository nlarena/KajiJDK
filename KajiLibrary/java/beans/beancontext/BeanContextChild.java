package java.beans.beancontext;

import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

/**
 * A bean that knows which context it lives in.
 *
 * <p>The other half of {@link BeanContext}: the container keeps its children and the child keeps
 * its container. The two-way relation is not redundancy — it is what lets a child ask its
 * environment for services and resources without anyone passing them in as parameters.
 *
 * <p><strong>A child can refuse a change of context</strong>, and that is why
 * {@code setBeanContext} declares {@link PropertyVetoException}: the child registers veto listeners
 * on the `"beanContext"` property and, if any objects, the move does not happen. That is why both
 * listener families —change and veto— are here. This note called it the only property of this API
 * defined as vetoable; {@link BeanContextSupport#setLocale} asks for vetoes on `"locale"` too.
 */
public interface BeanContextChild {

    /**
     * Moves this child to that context.
     *
     * @throws PropertyVetoException if a veto listener objects; the context does not change
     */
    void setBeanContext(BeanContext bc) throws PropertyVetoException;

    /** The context it lives in, or `null` if it is not in one yet. */
    BeanContext getBeanContext();

    /** Registers a listener for changes to that property. */
    void addPropertyChangeListener(String name, PropertyChangeListener pcl);

    /** Removes it. */
    void removePropertyChangeListener(String name, PropertyChangeListener pcl);

    /** Registers a listener that can **veto** changes to that property. */
    void addVetoableChangeListener(String name, VetoableChangeListener vcl);

    /** Removes it. */
    void removeVetoableChangeListener(String name, VetoableChangeListener vcl);
}
