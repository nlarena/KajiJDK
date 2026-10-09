package javax.management;

/**
 * An MBean that knows how to store and recover itself.
 *
 * <p>It deliberately does not say where or how: the contract is only "there is a place and these
 * two operations use it". Whoever implements it picks a file, a database or whatever.
 */
public interface PersistentMBean {

    /**
     * Recovers the stored state and applies it.
     *
     * @throws InstanceNotFoundException if there is nothing stored for this MBean
     */
    void load() throws MBeanException, RuntimeOperationsException, InstanceNotFoundException;

    /** Stores the current state. */
    void store() throws MBeanException, RuntimeOperationsException, InstanceNotFoundException;
}
