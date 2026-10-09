package javax.management;

import java.io.IOException;
import java.util.Set;

/**
 * Everything that can be asked of a JMX agent, wherever it is.
 *
 * <p>Its reason to be is a {@code throws}: every method declares {@code IOException}. {@link
 * MBeanServer}, which is the <b>local</b> agent, redeclares the same methods without it. That way
 * code that will only talk to the agent of its own machine does not pay the cost of handling a
 * network failure that cannot happen, and code written against this interface works with both.
 *
 * <p>Hence the relation is {@code MBeanServer extends MBeanServerConnection} and not the other way
 * round: remote is the general case and local the restriction.
 */
public interface MBeanServerConnection {

    /** Instantiates and registers an MBean of the given class. */
    ObjectInstance createMBean(String className, ObjectName name)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException,
                   NotCompliantMBeanException, IOException;

    /** The same, but loading the class with the loader registered under {@code loaderName}. */
    ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException, NotCompliantMBeanException,
                   InstanceNotFoundException, IOException;

    /**
     * The same, choosing the constructor.
     *
     * @param signature the class names of the parameters, which is how the signature is chosen
     */
    ObjectInstance createMBean(String className, ObjectName name, Object[] params,
                               String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException,
                   NotCompliantMBeanException, IOException;

    /** With loader and constructor chosen. */
    ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
                               Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException, NotCompliantMBeanException,
                   InstanceNotFoundException, IOException;

    /** Unregisters an MBean. */
    void unregisterMBean(ObjectName name)
            throws InstanceNotFoundException, MBeanRegistrationException, IOException;

    /** Name and class of a registered MBean. */
    ObjectInstance getObjectInstance(ObjectName name)
            throws InstanceNotFoundException, IOException;

    /**
     * The MBeans that match, with their class.
     *
     * <p>A null {@code name} is equivalent to {@link ObjectName#WILDCARD}; a null {@code query}
     * filters nothing more.
     */
    Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException;

    /** The same, but only the names: cheaper if the class is not needed. */
    Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException;

    /** Whether there is an MBean with that name. */
    boolean isRegistered(ObjectName name) throws IOException;

    /** How many MBeans there are. */
    Integer getMBeanCount() throws IOException;

    /** Reads an attribute. */
    Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                   ReflectionException, IOException;

    /**
     * Reads several at once.
     *
     * <p>The returned list may be <b>shorter</b> than the requested one: the attributes that failed
     * are simply not there. There is no way to know which one failed and why, and it is like that
     * by design -- the operation is best effort.
     */
    AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException, IOException;

    /** Writes an attribute. */
    void setAttribute(ObjectName name, Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException,
                   InvalidAttributeValueException, MBeanException, ReflectionException, IOException;

    /** Writes several; returns the ones that could be written. */
    AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException, IOException;

    /**
     * Invokes an operation.
     *
     * @param signature the class names of the parameters, to disambiguate overloads
     */
    Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException, IOException;

    /** The domain used when a name does not bring one. */
    String getDefaultDomain() throws IOException;

    /** The domains in which some MBean is registered. */
    String[] getDomains() throws IOException;

    /** Registers a listener against an MBean. */
    void addNotificationListener(ObjectName name, NotificationListener listener,
                                 NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, IOException;

    /**
     * Registers <b>another MBean</b> as a listener.
     *
     * <p>It is the variant that really works over a remote connection: the listener lives in the
     * agent, so the notifications do not cross the network.
     */
    void addNotificationListener(ObjectName name, ObjectName listener,
                                 NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, IOException;

    /** Removes all the registrations of that listener MBean. */
    void removeNotificationListener(ObjectName name, ObjectName listener)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /** Removes the exact registration of that listener MBean. */
    void removeNotificationListener(ObjectName name, ObjectName listener,
                                    NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /** Removes all the registrations of that listener. */
    void removeNotificationListener(ObjectName name, NotificationListener listener)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /** Removes the exact registration. */
    void removeNotificationListener(ObjectName name, NotificationListener listener,
                                    NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /** The MBean's metadata: the entry point to everything else. */
    MBeanInfo getMBeanInfo(ObjectName name)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException,
                   IOException;

    /** Whether the MBean is of that class or of a subclass. */
    boolean isInstanceOf(ObjectName name, String className)
            throws InstanceNotFoundException, IOException;
}
