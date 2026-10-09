package java.beans.beancontext;

import java.beans.DesignMode;
import java.beans.Visibility;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

/**
 * A container of beans: the collection of its children, plus the environment it offers them.
 *
 * <p>It is a {@link Collection} and should be read as one --adding a bean to the context **is**
 * `add`-ing it-- but it is also a {@link BeanContextChild}, and that is what shapes the whole API:
 * contexts nest. A context has children and is at the same time the child of another, which is why
 * service lookups climb the chain until someone answers. (This note said resource lookups climb it
 * too; {@link BeanContextSupport} resolves those with the asking child's class loader, and never
 * asks the parent.)
 *
 * <h2>The global lock</h2>
 *
 * <p>{@link #globalHierarchyLock} is **one for the whole hierarchy**, not one per context, because
 * an operation can touch several contexts at once: moving a child takes it out of one and puts it
 * into another. With one lock per context, two crossed moves would deadlock. With a single lock
 * there is no order to respect, because there are never two locks to take.
 */
public interface BeanContext extends BeanContextChild, Collection, DesignMode, Visibility {

    /**
     * The lock that serializes every operation on the hierarchy. See the interface note on why it
     * is one and not one per context.
     */
    public static final Object globalHierarchyLock = new Object();

    /**
     * Instantiates that bean **inside this context**, by name.
     *
     * @throws IOException if the bean could not be read
     * @throws ClassNotFoundException if the class was not found
     */
    Object instantiateChild(String beanName) throws IOException, ClassNotFoundException;

    /** The resource, looked up as that child would see it. */
    InputStream getResourceAsStream(String name, BeanContextChild bcc);

    /** The resource's URL, looked up as that child would see it. */
    URL getResource(String name, BeanContextChild bcc);

    /** Registers a listener for children being added and removed. */
    void addBeanContextMembershipListener(BeanContextMembershipListener bcml);

    /** Removes it. */
    void removeBeanContextMembershipListener(BeanContextMembershipListener bcml);
}
