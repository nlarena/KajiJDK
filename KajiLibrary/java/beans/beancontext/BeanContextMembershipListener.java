package java.beans.beancontext;

import java.util.EventListener;

/** Listens for children being added to and removed from a {@link BeanContext}. */
public interface BeanContextMembershipListener extends EventListener {

    /** The children the event names were added. */
    void childrenAdded(BeanContextMembershipEvent bcme);

    /** The children the event names were removed. */
    void childrenRemoved(BeanContextMembershipEvent bcme);
}
