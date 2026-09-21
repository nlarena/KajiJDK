package javax.management;

import javax.management.loading.ClassLoaderRepository;

import java.util.Set;

/**
 * The local agent: the registry where this virtual machine's MBeans live.
 *
 * <p>It redeclares <b>all</b> the methods of {@link MBeanServerConnection} without
 * {@code IOException}. The repetition looks gratuitous and is not: it is the only way Java gives to
 * narrow the {@code throws} of an inherited interface, and it is what spares local code from
 * catching an impossible network failure.
 *
 * <p>It also adds what only makes sense in the same process: {@link #registerMBean}, which
 * registers an <b>already built</b> object --impossible to send over the network--, the four {@link
 * #instantiate} and access to the class loaders.
 *
 * <h2>What is missing and why</h2>
 *
 * <p>The three {@code deserialize} are there, and throw {@link UnsupportedOperationException} --
 * which is <b>literally the body they have in the JDK</b>. Since they became default methods they
 * are not a contract the interface promises to fulfil: they are a place where a concrete
 * implementation can put something if it wants, and the interface warns that it does not. They
 * have been deprecated since 1.5.
 *
 * <p>They used to be missing because {@code java.io.ObjectInputStream} was not in this library; it
 * is now.
 */
public interface MBeanServer extends MBeanServerConnection {

    /** Instantiates and registers an MBean of the given class. */
    ObjectInstance createMBean(String className, ObjectName name)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException, NotCompliantMBeanException;

    /** The same, loading the class with the loader registered under {@code loaderName}. */
    ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException, NotCompliantMBeanException,
                   InstanceNotFoundException;

    /** The same, choosing the constructor by signature. */
    ObjectInstance createMBean(String className, ObjectName name, Object[] params,
                               String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException, NotCompliantMBeanException;

    /** With loader and constructor chosen. */
    ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
                               Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException, NotCompliantMBeanException,
                   InstanceNotFoundException;

    /**
     * Registers an object that already exists.
     *
     * <p>It is what a remote agent cannot offer, which is why it is not in
     * {@link MBeanServerConnection}. The {@code name} may be {@code null} if the MBean implements
     * {@link MBeanRegistration} and names itself.
     */
    ObjectInstance registerMBean(Object object, ObjectName name)
            throws InstanceAlreadyExistsException, MBeanRegistrationException,
                   NotCompliantMBeanException;

    /** Unregisters an MBean. */
    void unregisterMBean(ObjectName name)
            throws InstanceNotFoundException, MBeanRegistrationException;

    /** Name and class of a registered MBean. */
    ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException;

    /** The MBeans that match, with their class. */
    Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query);

    /** The same, only the names. */
    Set<ObjectName> queryNames(ObjectName name, QueryExp query);

    /** Whether there is an MBean with that name. */
    boolean isRegistered(ObjectName name);

    /** How many MBeans there are. */
    Integer getMBeanCount();

    /** Reads an attribute. */
    Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                   ReflectionException;

    /** Reads several; the ones that fail do not appear in the answer. */
    AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException;

    /** Writes an attribute. */
    void setAttribute(ObjectName name, Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException,
                   InvalidAttributeValueException, MBeanException, ReflectionException;

    /** Writes several; returns the ones that could be written. */
    AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException;

    /** Invokes an operation. */
    Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException;

    /** The domain used when a name does not bring one. */
    String getDefaultDomain();

    /** The domains in which some MBean is registered. */
    String[] getDomains();

    /** Registers a listener against an MBean. */
    void addNotificationListener(ObjectName name, NotificationListener listener,
                                 NotificationFilter filter, Object handback)
            throws InstanceNotFoundException;

    /** Registers another MBean as a listener. */
    void addNotificationListener(ObjectName name, ObjectName listener,
                                 NotificationFilter filter, Object handback)
            throws InstanceNotFoundException;

    /** Removes all the registrations of that listener MBean. */
    void removeNotificationListener(ObjectName name, ObjectName listener)
            throws InstanceNotFoundException, ListenerNotFoundException;

    /** Removes the exact registration of that listener MBean. */
    void removeNotificationListener(ObjectName name, ObjectName listener,
                                    NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException;

    /** Removes all the registrations of that listener. */
    void removeNotificationListener(ObjectName name, NotificationListener listener)
            throws InstanceNotFoundException, ListenerNotFoundException;

    /** Removes the exact registration. */
    void removeNotificationListener(ObjectName name, NotificationListener listener,
                                    NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException;

    /** The MBean's metadata. */
    MBeanInfo getMBeanInfo(ObjectName name)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException;

    /** Whether the MBean is of that class or of a subclass. */
    boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException;

    /**
     * Builds an object <b>without</b> registering it.
     *
     * <p>It serves to make the arguments of another call using the agent's loaders.
     */
    Object instantiate(String className) throws ReflectionException, MBeanException;

    /** The same, with the loader registered under {@code loaderName}. */
    Object instantiate(String className, ObjectName loaderName)
            throws ReflectionException, MBeanException, InstanceNotFoundException;

    /** The same, choosing the constructor. */
    Object instantiate(String className, Object[] params, String[] signature)
            throws ReflectionException, MBeanException;

    /** With loader and constructor chosen. */
    Object instantiate(String className, ObjectName loaderName, Object[] params,
                       String[] signature)
            throws ReflectionException, MBeanException, InstanceNotFoundException;

    /** The class loader that MBean was loaded with. */
    ClassLoader getClassLoaderFor(ObjectName name) throws InstanceNotFoundException;

    /** The class loader that is registered <b>as</b> an MBean under that name. */
    ClassLoader getClassLoader(ObjectName name) throws InstanceNotFoundException;

    /**
     * The loaders this agent knows, to look for a class by name.
     *
     * <p>It is what allows loading a class that arrived named from outside and is not on the
     * agent's class path.
     */
    ClassLoaderRepository getClassLoaderRepository();

    /**
     * Deserializes a byte array with the named MBean's loader.
     *
     * <p>This interface does not do it: it throws {@link UnsupportedOperationException}, which is
     * the same body it has in the JDK. A concrete implementation can redefine it.
     *
     * @deprecated as in the JDK since 1.5: use {@link #getClassLoaderFor} and deserialize outside
     */
    @Deprecated
    default java.io.ObjectInputStream deserialize(ObjectName name, byte[] data)
            throws InstanceNotFoundException, OperationsException {
        throw new UnsupportedOperationException("deserialize");
    }

    /**
     * Deserializes a byte array with the named class's loader.
     *
     * <p>See {@link #deserialize(ObjectName, byte[])}: it throws {@link
     * UnsupportedOperationException}.
     *
     * @deprecated as in the JDK since 1.5: use {@link #getClassLoaderRepository}
     */
    @Deprecated
    default java.io.ObjectInputStream deserialize(String className, byte[] data)
            throws OperationsException, ReflectionException {
        throw new UnsupportedOperationException("deserialize");
    }

    /**
     * Deserializes a byte array with the named loader.
     *
     * <p>See {@link #deserialize(ObjectName, byte[])}: it throws {@link
     * UnsupportedOperationException}.
     *
     * @deprecated as in the JDK since 1.5: use {@link #getClassLoader} and deserialize outside
     */
    @Deprecated
    default java.io.ObjectInputStream deserialize(String className, ObjectName loaderName,
            byte[] data) throws InstanceNotFoundException, OperationsException,
            ReflectionException {
        throw new UnsupportedOperationException("deserialize");
    }
}
