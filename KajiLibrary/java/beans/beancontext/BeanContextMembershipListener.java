package java.beans.beancontext;

import java.util.EventListener;

/** Escucha las altas y las bajas de hijos de un {@link BeanContext}. */
public interface BeanContextMembershipListener extends EventListener {

    /** Se agregaron los hijos que el evento nombra. */
    void childrenAdded(BeanContextMembershipEvent bcme);

    /** Se quitaron los hijos que el evento nombra. */
    void childrenRemoved(BeanContextMembershipEvent bcme);
}
