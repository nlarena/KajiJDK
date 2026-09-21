package javax.management.remote.rmi;

import java.io.Closeable;
import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.util.Set;

import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.NotificationResult;
import javax.security.auth.Subject;

/**
 * The remote object through which every call to another machine's {@code MBeanServer} travels.
 *
 * <h2>It is the MBeanServer, plus two things</h2>
 *
 * <p>The twenty-odd methods are {@link javax.management.MBeanServer}'s with two systematic
 * differences, and understanding those two differences is understanding the whole interface.
 *
 * <p><strong>A {@link Subject} at the end.</strong> Whoever opened the connection authenticated
 * once; this parameter says on whose behalf <strong>this</strong> call is made. It is what
 * allows an intermediate server to talk to the MBeanServer on behalf of several users without
 * opening one connection per user. With {@code null} the call goes on behalf of whoever
 * authenticated.
 *
 * <p><strong>{@link MarshalledObject} instead of the objects.</strong> The parameters that could
 * be of classes the server does not know --a notification filter of one's own, an operation's
 * argument-- travel serialized and are deserialized on the other side, with the connection's
 * class loader. If they travelled as objects, RMI would deserialize them on receipt, before
 * anybody could decide with which loader; and that moment is exactly where it has to be decided.
 *
 * <p>That wrapping is also what makes a per-connection deserialization filter possible: the bytes
 * can be looked at before turning them into objects.
 *
 * <h2>The notifications go the other way round</h2>
 *
 * <p>{@code fetchNotifications} is the only method with no equivalent in {@code MBeanServer}.
 * The client <strong>asks</strong> for the accumulated notifications instead of receiving them:
 * for the server to be able to call it, the client would have to be a remote object too,
 * reachable from the server, and that does not survive a firewall or a NAT.
 *
 * @since 1.5
 */
public interface RMIConnection extends Closeable, Remote {

    /**
     * This connection's identifier, the same one the server sees.
     * @return whatever the MBeanServer on the other side answers
     * @throws IOException if communication with the server was cut
     */
    String getConnectionId() throws IOException;

    /**
     * Closes the connection and frees what the server had reserved for it.
     * @throws IOException if communication with the server was cut
     */
    void close() throws IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param className the MBean's class name
     * @param name the MBean's name
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws InstanceAlreadyExistsException if there is already an MBean with that name
     * @throws MBeanRegistrationException if the MBean objected to being registered or unregistered
     * @throws MBeanException if the MBean itself threw an exception
     * @throws NotCompliantMBeanException if the class does not meet what an MBean has to be
     * @throws IOException if communication with the server was cut
     */
    ObjectInstance createMBean(String className, ObjectName name, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param className the MBean's class name
     * @param name the MBean's name
     * @param loaderName the class loader to use
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws InstanceAlreadyExistsException if there is already an MBean with that name
     * @throws MBeanRegistrationException if the MBean objected to being registered or unregistered
     * @throws MBeanException if the MBean itself threw an exception
     * @throws NotCompliantMBeanException if the class does not meet what an MBean has to be
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IOException if communication with the server was cut
     */
    ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
            Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param className the MBean's class name
     * @param name the MBean's name
     * @param params the arguments, serialized
     * @param signature the arguments' signature
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws InstanceAlreadyExistsException if there is already an MBean with that name
     * @throws MBeanRegistrationException if the MBean objected to being registered or unregistered
     * @throws MBeanException if the MBean itself threw an exception
     * @throws NotCompliantMBeanException if the class does not meet what an MBean has to be
     * @throws IOException if communication with the server was cut
     */
    ObjectInstance createMBean(String className, ObjectName name, MarshalledObject params,
            String[] signature, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param className the MBean's class name
     * @param name the MBean's name
     * @param loaderName the class loader to use
     * @param params the arguments, serialized
     * @param signature the arguments' signature
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws InstanceAlreadyExistsException if there is already an MBean with that name
     * @throws MBeanRegistrationException if the MBean objected to being registered or unregistered
     * @throws MBeanException if the MBean itself threw an exception
     * @throws NotCompliantMBeanException if the class does not meet what an MBean has to be
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IOException if communication with the server was cut
     */
    ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
            MarshalledObject params, String[] signature, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws MBeanRegistrationException if the MBean objected to being registered or unregistered
     * @throws IOException if communication with the server was cut
     */
    void unregisterMBean(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, MBeanRegistrationException, IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IOException if communication with the server was cut
     */
    ObjectInstance getObjectInstance(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param query the query filter, serialized
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws IOException if communication with the server was cut
     */
    Set<ObjectInstance> queryMBeans(ObjectName name, MarshalledObject query,
            Subject delegationSubject)
            throws IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param query the query filter, serialized
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws IOException if communication with the server was cut
     */
    Set<ObjectName> queryNames(ObjectName name, MarshalledObject query, Subject delegationSubject)
            throws IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws IOException if communication with the server was cut
     */
    boolean isRegistered(ObjectName name, Subject delegationSubject) throws IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws IOException if communication with the server was cut
     */
    Integer getMBeanCount(Subject delegationSubject) throws IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param attribute the attribute
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws MBeanException if the MBean itself threw an exception
     * @throws AttributeNotFoundException if the MBean has no such attribute
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws IOException if communication with the server was cut
     */
    Object getAttribute(ObjectName name, String attribute, Subject delegationSubject)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                   ReflectionException, IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param attributes the attributes
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws IOException if communication with the server was cut
     */
    AttributeList getAttributes(ObjectName name, String[] attributes, Subject delegationSubject)
            throws InstanceNotFoundException, ReflectionException, IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param attribute the attribute
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws AttributeNotFoundException if the MBean has no such attribute
     * @throws InvalidAttributeValueException if the value does not fit that attribute
     * @throws MBeanException if the MBean itself threw an exception
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws IOException if communication with the server was cut
     */
    void setAttribute(ObjectName name, MarshalledObject attribute, Subject delegationSubject)
            throws InstanceNotFoundException, AttributeNotFoundException,
                   InvalidAttributeValueException, MBeanException, ReflectionException, IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param attributes the attributes
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws IOException if communication with the server was cut
     */
    AttributeList setAttributes(ObjectName name, MarshalledObject attributes,
            Subject delegationSubject)
            throws InstanceNotFoundException, ReflectionException, IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param operationName the operation's name
     * @param params the arguments, serialized
     * @param signature the arguments' signature
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws MBeanException if the MBean itself threw an exception
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws IOException if communication with the server was cut
     */
    Object invoke(ObjectName name, String operationName, MarshalledObject params,
            String[] signature, Subject delegationSubject)
            throws InstanceNotFoundException, MBeanException, ReflectionException, IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws IOException if communication with the server was cut
     */
    String getDefaultDomain(Subject delegationSubject) throws IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws IOException if communication with the server was cut
     */
    String[] getDomains(Subject delegationSubject) throws IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IntrospectionException if the MBean's shape could not be found out
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws IOException if communication with the server was cut
     */
    MBeanInfo getMBeanInfo(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException,
                   IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param className the MBean's class name
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the MBeanServer on the other side answers
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IOException if communication with the server was cut
     */
    boolean isInstanceOf(ObjectName name, String className, Subject delegationSubject)
            throws InstanceNotFoundException, IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param listener the listener
     * @param filter the filter, serialized
     * @param handback the object handed back with each notification, serialized
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IOException if communication with the server was cut
     */
    void addNotificationListener(ObjectName name, ObjectName listener, MarshalledObject filter,
            MarshalledObject handback, Subject delegationSubject)
            throws InstanceNotFoundException, IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param listener the listener
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws ListenerNotFoundException if that listener was not registered
     * @throws IOException if communication with the server was cut
     */
    void removeNotificationListener(ObjectName name, ObjectName listener, Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param listener the listener
     * @param filter the filter, serialized
     * @param handback the object handed back with each notification, serialized
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws ListenerNotFoundException if that listener was not registered
     * @throws IOException if communication with the server was cut
     */
    void removeNotificationListener(ObjectName name, ObjectName listener, MarshalledObject filter,
            MarshalledObject handback, Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /**
     * Registers several listeners at once, and returns one identifier for each.
     *
     * <p>Several at once because each registration is a round trip over the network. The
     * identifiers are what later allows removing them without sending the filter again.
     *
     * @param names the MBeans' names
     * @param filters the filters, serialized
     * @param delegationSubjects on whose behalf each call is made
     * @return whatever the MBeanServer on the other side answers
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IOException if communication with the server was cut
     */
    Integer[] addNotificationListeners(ObjectName[] names, MarshalledObject[] filters,
            Subject[] delegationSubjects)
            throws InstanceNotFoundException, IOException;

    /**
     * Forwards the {@code MBeanServer} operation of the same name.
     *
     * @param name the MBean's name
     * @param listenerIDs the identifiers {@code addNotificationListeners} returned
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws ListenerNotFoundException if that listener was not registered
     * @throws IOException if communication with the server was cut
     */
    void removeNotificationListeners(ObjectName name, Integer[] listenerIDs,
            Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /**
     * Fetches the notifications that accumulated on the server side.
     *
     * <p>It is the heart of the model: notifications are not pushed to the client, the client comes
     * to get them. With RMI there is no way for the server to call the client without the client
     * being a remote object itself, and that does not survive a firewall.
     *
     * @param clientSequenceNumber from which sequence number to fetch
     * @param maxNotifications how many to fetch at most
     * @param timeout how long to wait if there is none
     * @return whatever the MBeanServer on the other side answers
     * @throws IOException if communication with the server was cut
     */
    NotificationResult fetchNotifications(long clientSequenceNumber, int maxNotifications,
            long timeout)
            throws IOException;
}
