package javax.management.remote.rmi;

import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.rmi.UnexpectedException;
import java.rmi.server.RemoteRef;
import java.rmi.server.RemoteStub;
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
 * {@link RMIConnectionImpl}'s stub: the connection as the client sees it.
 *
 * <h2>What each method does</h2>
 *
 * <p>The same thing, always: it puts the arguments in an array, passes it to the
 * {@link RemoteRef} together with the number that identifies the method --see {@link Hash}--,
 * and converts what comes back. The declared exceptions are let through as they are; any other
 * checked exception would be a breach of the interface's contract, and that is why it is wrapped
 * in {@link UnexpectedException}.
 *
 * <p>It is generated code in the JDK too, where {@code rmic} writes it. The only difference is
 * that there the numbers are literals and here they are computed when the class is loaded.
 *
 * <h2>State in this VM</h2>
 *
 * <p>The class works; what there is not is anywhere to get a live {@link RemoteRef} from,
 * because that is the RMI transport. To talk to an {@link RMIConnectionImpl} of this same
 * process no stub is needed: it is used directly, which is what {@link RMIConnector} does.
 *
 * @since 1.5
 */
public final class RMIConnectionImpl_Stub extends RemoteStub implements RMIConnection {

    private static final long serialVersionUID = 2L;

    private static Method $method_getConnectionId_0;
    private static Method $method_close_1;
    private static Method $method_createMBean_2;
    private static Method $method_createMBean_3;
    private static Method $method_createMBean_4;
    private static Method $method_createMBean_5;
    private static Method $method_unregisterMBean_6;
    private static Method $method_getObjectInstance_7;
    private static Method $method_queryMBeans_8;
    private static Method $method_queryNames_9;
    private static Method $method_isRegistered_10;
    private static Method $method_getMBeanCount_11;
    private static Method $method_getAttribute_12;
    private static Method $method_getAttributes_13;
    private static Method $method_setAttribute_14;
    private static Method $method_setAttributes_15;
    private static Method $method_invoke_16;
    private static Method $method_getDefaultDomain_17;
    private static Method $method_getDomains_18;
    private static Method $method_getMBeanInfo_19;
    private static Method $method_isInstanceOf_20;
    private static Method $method_addNotificationListener_21;
    private static Method $method_removeNotificationListener_22;
    private static Method $method_removeNotificationListener_23;
    private static Method $method_addNotificationListeners_24;
    private static Method $method_removeNotificationListeners_25;
    private static Method $method_fetchNotifications_26;

    private static long $hash_getConnectionId_0;
    private static long $hash_close_1;
    private static long $hash_createMBean_2;
    private static long $hash_createMBean_3;
    private static long $hash_createMBean_4;
    private static long $hash_createMBean_5;
    private static long $hash_unregisterMBean_6;
    private static long $hash_getObjectInstance_7;
    private static long $hash_queryMBeans_8;
    private static long $hash_queryNames_9;
    private static long $hash_isRegistered_10;
    private static long $hash_getMBeanCount_11;
    private static long $hash_getAttribute_12;
    private static long $hash_getAttributes_13;
    private static long $hash_setAttribute_14;
    private static long $hash_setAttributes_15;
    private static long $hash_invoke_16;
    private static long $hash_getDefaultDomain_17;
    private static long $hash_getDomains_18;
    private static long $hash_getMBeanInfo_19;
    private static long $hash_isInstanceOf_20;
    private static long $hash_addNotificationListener_21;
    private static long $hash_removeNotificationListener_22;
    private static long $hash_removeNotificationListener_23;
    private static long $hash_addNotificationListeners_24;
    private static long $hash_removeNotificationListeners_25;
    private static long $hash_fetchNotifications_26;

    static {
        try {
            $method_getConnectionId_0 = RMIConnection.class.getMethod("getConnectionId");
            $method_close_1 = RMIConnection.class.getMethod("close");
            $method_createMBean_2 = RMIConnection.class.getMethod("createMBean", String.class,
                    ObjectName.class, Subject.class);
            $method_createMBean_3 = RMIConnection.class.getMethod("createMBean", String.class,
                    ObjectName.class, ObjectName.class, Subject.class);
            $method_createMBean_4 = RMIConnection.class.getMethod("createMBean", String.class,
                    ObjectName.class, MarshalledObject.class, String[].class, Subject.class);
            $method_createMBean_5 = RMIConnection.class.getMethod("createMBean", String.class,
                    ObjectName.class, ObjectName.class, MarshalledObject.class, String[].class,
                    Subject.class);
            $method_unregisterMBean_6 = RMIConnection.class.getMethod("unregisterMBean",
                    ObjectName.class, Subject.class);
            $method_getObjectInstance_7 = RMIConnection.class.getMethod("getObjectInstance",
                    ObjectName.class, Subject.class);
            $method_queryMBeans_8 = RMIConnection.class.getMethod("queryMBeans", ObjectName.class,
                    MarshalledObject.class, Subject.class);
            $method_queryNames_9 = RMIConnection.class.getMethod("queryNames", ObjectName.class,
                    MarshalledObject.class, Subject.class);
            $method_isRegistered_10 = RMIConnection.class.getMethod("isRegistered",
                    ObjectName.class, Subject.class);
            $method_getMBeanCount_11 = RMIConnection.class.getMethod("getMBeanCount",
                    Subject.class);
            $method_getAttribute_12 = RMIConnection.class.getMethod("getAttribute",
                    ObjectName.class, String.class, Subject.class);
            $method_getAttributes_13 = RMIConnection.class.getMethod("getAttributes",
                    ObjectName.class, String[].class, Subject.class);
            $method_setAttribute_14 = RMIConnection.class.getMethod("setAttribute",
                    ObjectName.class, MarshalledObject.class, Subject.class);
            $method_setAttributes_15 = RMIConnection.class.getMethod("setAttributes",
                    ObjectName.class, MarshalledObject.class, Subject.class);
            $method_invoke_16 = RMIConnection.class.getMethod("invoke", ObjectName.class,
                    String.class, MarshalledObject.class, String[].class, Subject.class);
            $method_getDefaultDomain_17 = RMIConnection.class.getMethod("getDefaultDomain",
                    Subject.class);
            $method_getDomains_18 = RMIConnection.class.getMethod("getDomains", Subject.class);
            $method_getMBeanInfo_19 = RMIConnection.class.getMethod("getMBeanInfo",
                    ObjectName.class, Subject.class);
            $method_isInstanceOf_20 = RMIConnection.class.getMethod("isInstanceOf",
                    ObjectName.class, String.class, Subject.class);
            $method_addNotificationListener_21 = RMIConnection.class.getMethod(
                    "addNotificationListener", ObjectName.class, ObjectName.class,
                    MarshalledObject.class, MarshalledObject.class, Subject.class);
            $method_removeNotificationListener_22 = RMIConnection.class.getMethod(
                    "removeNotificationListener", ObjectName.class, ObjectName.class,
                    Subject.class);
            $method_removeNotificationListener_23 = RMIConnection.class.getMethod(
                    "removeNotificationListener", ObjectName.class, ObjectName.class,
                    MarshalledObject.class, MarshalledObject.class, Subject.class);
            $method_addNotificationListeners_24 = RMIConnection.class.getMethod(
                    "addNotificationListeners", ObjectName[].class, MarshalledObject[].class,
                    Subject[].class);
            $method_removeNotificationListeners_25 = RMIConnection.class.getMethod(
                    "removeNotificationListeners", ObjectName.class, Integer[].class,
                    Subject.class);
            $method_fetchNotifications_26 = RMIConnection.class.getMethod("fetchNotifications",
                    long.class, int.class, long.class);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError("stub class initialization failed");
        }
        $hash_getConnectionId_0 = Hash.de($method_getConnectionId_0);
        $hash_close_1 = Hash.de($method_close_1);
        $hash_createMBean_2 = Hash.de($method_createMBean_2);
        $hash_createMBean_3 = Hash.de($method_createMBean_3);
        $hash_createMBean_4 = Hash.de($method_createMBean_4);
        $hash_createMBean_5 = Hash.de($method_createMBean_5);
        $hash_unregisterMBean_6 = Hash.de($method_unregisterMBean_6);
        $hash_getObjectInstance_7 = Hash.de($method_getObjectInstance_7);
        $hash_queryMBeans_8 = Hash.de($method_queryMBeans_8);
        $hash_queryNames_9 = Hash.de($method_queryNames_9);
        $hash_isRegistered_10 = Hash.de($method_isRegistered_10);
        $hash_getMBeanCount_11 = Hash.de($method_getMBeanCount_11);
        $hash_getAttribute_12 = Hash.de($method_getAttribute_12);
        $hash_getAttributes_13 = Hash.de($method_getAttributes_13);
        $hash_setAttribute_14 = Hash.de($method_setAttribute_14);
        $hash_setAttributes_15 = Hash.de($method_setAttributes_15);
        $hash_invoke_16 = Hash.de($method_invoke_16);
        $hash_getDefaultDomain_17 = Hash.de($method_getDefaultDomain_17);
        $hash_getDomains_18 = Hash.de($method_getDomains_18);
        $hash_getMBeanInfo_19 = Hash.de($method_getMBeanInfo_19);
        $hash_isInstanceOf_20 = Hash.de($method_isInstanceOf_20);
        $hash_addNotificationListener_21 = Hash.de($method_addNotificationListener_21);
        $hash_removeNotificationListener_22 = Hash.de($method_removeNotificationListener_22);
        $hash_removeNotificationListener_23 = Hash.de($method_removeNotificationListener_23);
        $hash_addNotificationListeners_24 = Hash.de($method_addNotificationListeners_24);
        $hash_removeNotificationListeners_25 = Hash.de($method_removeNotificationListeners_25);
        $hash_fetchNotifications_26 = Hash.de($method_fetchNotifications_26);
    }

    /**
     * A stub over that reference.
     *
     * @param ref the reference that knows how to reach the remote object
     */
    public RMIConnectionImpl_Stub(RemoteRef ref) {
        super(ref);
    }

    /**
     * This connection's identifier, the same one the server sees.
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws IOException if communication with the server was cut
     */
    public String getConnectionId() throws IOException {
        try {
            final Object r = ref.invoke(this, $method_getConnectionId_0,
                    null, $hash_getConnectionId_0);
            return (String) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Closes the connection and frees what the server had reserved for it.
     * @throws RemoteException if communication with the server was cut
     * @throws IOException if communication with the server was cut
     */
    public void close() throws IOException {
        try {
            ref.invoke(this, $method_close_1, null, $hash_close_1);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param className the MBean's class name
     * @param name the MBean's name
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws InstanceAlreadyExistsException if there is already an MBean with that name
     * @throws MBeanRegistrationException if the MBean objected to being registered or unregistered
     * @throws MBeanException if the MBean itself threw an exception
     * @throws NotCompliantMBeanException if the class does not meet what an MBean has to be
     * @throws IOException if communication with the server was cut
     */
    public ObjectInstance createMBean(String className, ObjectName name, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException {
        try {
            final Object r = ref.invoke(this, $method_createMBean_2,
                    new Object[] {className, name, delegationSubject}, $hash_createMBean_2);
            return (ObjectInstance) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (ReflectionException e) {
            throw e;
        } catch (InstanceAlreadyExistsException e) {
            throw e;
        } catch (MBeanRegistrationException e) {
            throw e;
        } catch (MBeanException e) {
            throw e;
        } catch (NotCompliantMBeanException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param className the MBean's class name
     * @param name the MBean's name
     * @param loaderName the class loader to use
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws InstanceAlreadyExistsException if there is already an MBean with that name
     * @throws MBeanRegistrationException if the MBean objected to being registered or unregistered
     * @throws MBeanException if the MBean itself threw an exception
     * @throws NotCompliantMBeanException if the class does not meet what an MBean has to be
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IOException if communication with the server was cut
     */
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
            Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException {
        try {
            final Object r = ref.invoke(this, $method_createMBean_3,
                    new Object[] {className, name, loaderName,
                            delegationSubject}, $hash_createMBean_3);
            return (ObjectInstance) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (ReflectionException e) {
            throw e;
        } catch (InstanceAlreadyExistsException e) {
            throw e;
        } catch (MBeanRegistrationException e) {
            throw e;
        } catch (MBeanException e) {
            throw e;
        } catch (NotCompliantMBeanException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param className the MBean's class name
     * @param name the MBean's name
     * @param params the arguments, serialized
     * @param signature the arguments' signature
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws InstanceAlreadyExistsException if there is already an MBean with that name
     * @throws MBeanRegistrationException if the MBean objected to being registered or unregistered
     * @throws MBeanException if the MBean itself threw an exception
     * @throws NotCompliantMBeanException if the class does not meet what an MBean has to be
     * @throws IOException if communication with the server was cut
     */
    public ObjectInstance createMBean(String className, ObjectName name, MarshalledObject params,
            String[] signature, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException {
        try {
            final Object r = ref.invoke(this, $method_createMBean_4,
                    new Object[] {className, name, params, signature,
                            delegationSubject}, $hash_createMBean_4);
            return (ObjectInstance) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (ReflectionException e) {
            throw e;
        } catch (InstanceAlreadyExistsException e) {
            throw e;
        } catch (MBeanRegistrationException e) {
            throw e;
        } catch (MBeanException e) {
            throw e;
        } catch (NotCompliantMBeanException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param className the MBean's class name
     * @param name the MBean's name
     * @param loaderName the class loader to use
     * @param params the arguments, serialized
     * @param signature the arguments' signature
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws InstanceAlreadyExistsException if there is already an MBean with that name
     * @throws MBeanRegistrationException if the MBean objected to being registered or unregistered
     * @throws MBeanException if the MBean itself threw an exception
     * @throws NotCompliantMBeanException if the class does not meet what an MBean has to be
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IOException if communication with the server was cut
     */
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
            MarshalledObject params, String[] signature, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException {
        try {
            final Object r = ref.invoke(this, $method_createMBean_5,
                    new Object[] {className, name, loaderName, params, signature,
                            delegationSubject}, $hash_createMBean_5);
            return (ObjectInstance) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (ReflectionException e) {
            throw e;
        } catch (InstanceAlreadyExistsException e) {
            throw e;
        } catch (MBeanRegistrationException e) {
            throw e;
        } catch (MBeanException e) {
            throw e;
        } catch (NotCompliantMBeanException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @throws RemoteException if communication with the server was cut
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws MBeanRegistrationException if the MBean objected to being registered or unregistered
     * @throws IOException if communication with the server was cut
     */
    public void unregisterMBean(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, MBeanRegistrationException, IOException {
        try {
            ref.invoke(this, $method_unregisterMBean_6,
                    new Object[] {name, delegationSubject}, $hash_unregisterMBean_6);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (MBeanRegistrationException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IOException if communication with the server was cut
     */
    public ObjectInstance getObjectInstance(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, IOException {
        try {
            final Object r = ref.invoke(this, $method_getObjectInstance_7,
                    new Object[] {name, delegationSubject}, $hash_getObjectInstance_7);
            return (ObjectInstance) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param query the query filter, serialized
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws IOException if communication with the server was cut
     */
    public Set<ObjectInstance> queryMBeans(ObjectName name, MarshalledObject query,
            Subject delegationSubject)
            throws IOException {
        try {
            final Object r = ref.invoke(this, $method_queryMBeans_8,
                    new Object[] {name, query, delegationSubject}, $hash_queryMBeans_8);
            return (Set<ObjectInstance>) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param query the query filter, serialized
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws IOException if communication with the server was cut
     */
    public Set<ObjectName> queryNames(ObjectName name, MarshalledObject query,
            Subject delegationSubject)
            throws IOException {
        try {
            final Object r = ref.invoke(this, $method_queryNames_9,
                    new Object[] {name, query, delegationSubject}, $hash_queryNames_9);
            return (Set<ObjectName>) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws IOException if communication with the server was cut
     */
    public boolean isRegistered(ObjectName name, Subject delegationSubject) throws IOException {
        try {
            final Object r = ref.invoke(this, $method_isRegistered_10,
                    new Object[] {name, delegationSubject}, $hash_isRegistered_10);
            return ((Boolean) r).booleanValue();
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws IOException if communication with the server was cut
     */
    public Integer getMBeanCount(Subject delegationSubject) throws IOException {
        try {
            final Object r = ref.invoke(this, $method_getMBeanCount_11,
                    new Object[] {delegationSubject}, $hash_getMBeanCount_11);
            return (Integer) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param attribute the attribute
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws MBeanException if the MBean itself threw an exception
     * @throws AttributeNotFoundException if the MBean has no such attribute
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws IOException if communication with the server was cut
     */
    public Object getAttribute(ObjectName name, String attribute, Subject delegationSubject)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                   ReflectionException, IOException {
        try {
            final Object r = ref.invoke(this, $method_getAttribute_12,
                    new Object[] {name, attribute, delegationSubject}, $hash_getAttribute_12);
            return (Object) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (MBeanException e) {
            throw e;
        } catch (AttributeNotFoundException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (ReflectionException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param attributes the attributes
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws IOException if communication with the server was cut
     */
    public AttributeList getAttributes(ObjectName name, String[] attributes,
            Subject delegationSubject)
            throws InstanceNotFoundException, ReflectionException, IOException {
        try {
            final Object r = ref.invoke(this, $method_getAttributes_13,
                    new Object[] {name, attributes, delegationSubject}, $hash_getAttributes_13);
            return (AttributeList) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (ReflectionException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param attribute the attribute
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @throws RemoteException if communication with the server was cut
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws AttributeNotFoundException if the MBean has no such attribute
     * @throws InvalidAttributeValueException if the value does not fit that attribute
     * @throws MBeanException if the MBean itself threw an exception
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws IOException if communication with the server was cut
     */
    public void setAttribute(ObjectName name, MarshalledObject attribute, Subject delegationSubject)
            throws InstanceNotFoundException, AttributeNotFoundException,
                   InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
        try {
            ref.invoke(this, $method_setAttribute_14,
                    new Object[] {name, attribute, delegationSubject}, $hash_setAttribute_14);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (AttributeNotFoundException e) {
            throw e;
        } catch (InvalidAttributeValueException e) {
            throw e;
        } catch (MBeanException e) {
            throw e;
        } catch (ReflectionException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param attributes the attributes
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws IOException if communication with the server was cut
     */
    public AttributeList setAttributes(ObjectName name, MarshalledObject attributes,
            Subject delegationSubject)
            throws InstanceNotFoundException, ReflectionException, IOException {
        try {
            final Object r = ref.invoke(this, $method_setAttributes_15,
                    new Object[] {name, attributes, delegationSubject}, $hash_setAttributes_15);
            return (AttributeList) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (ReflectionException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param operationName the operation's name
     * @param params the arguments, serialized
     * @param signature the arguments' signature
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws MBeanException if the MBean itself threw an exception
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws IOException if communication with the server was cut
     */
    public Object invoke(ObjectName name, String operationName, MarshalledObject params,
            String[] signature, Subject delegationSubject)
            throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        try {
            final Object r = ref.invoke(this, $method_invoke_16,
                    new Object[] {name, operationName, params, signature,
                            delegationSubject}, $hash_invoke_16);
            return (Object) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (MBeanException e) {
            throw e;
        } catch (ReflectionException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws IOException if communication with the server was cut
     */
    public String getDefaultDomain(Subject delegationSubject) throws IOException {
        try {
            final Object r = ref.invoke(this, $method_getDefaultDomain_17,
                    new Object[] {delegationSubject}, $hash_getDefaultDomain_17);
            return (String) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws IOException if communication with the server was cut
     */
    public String[] getDomains(Subject delegationSubject) throws IOException {
        try {
            final Object r = ref.invoke(this, $method_getDomains_18,
                    new Object[] {delegationSubject}, $hash_getDomains_18);
            return (String[]) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IntrospectionException if the MBean's shape could not be found out
     * @throws ReflectionException if reflection failed while constructing or calling
     * @throws IOException if communication with the server was cut
     */
    public MBeanInfo getMBeanInfo(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException,
                   IOException {
        try {
            final Object r = ref.invoke(this, $method_getMBeanInfo_19,
                    new Object[] {name, delegationSubject}, $hash_getMBeanInfo_19);
            return (MBeanInfo) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (IntrospectionException e) {
            throw e;
        } catch (ReflectionException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param className the MBean's class name
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IOException if communication with the server was cut
     */
    public boolean isInstanceOf(ObjectName name, String className, Subject delegationSubject)
            throws InstanceNotFoundException, IOException {
        try {
            final Object r = ref.invoke(this, $method_isInstanceOf_20,
                    new Object[] {name, className, delegationSubject}, $hash_isInstanceOf_20);
            return ((Boolean) r).booleanValue();
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param listener the listener
     * @param filter the filter, serialized
     * @param handback the object handed back with each notification, serialized
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @throws RemoteException if communication with the server was cut
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IOException if communication with the server was cut
     */
    public void addNotificationListener(ObjectName name, ObjectName listener,
            MarshalledObject filter, MarshalledObject handback, Subject delegationSubject)
            throws InstanceNotFoundException, IOException {
        try {
            ref.invoke(this, $method_addNotificationListener_21,
                    new Object[] {name, listener, filter, handback,
                            delegationSubject}, $hash_addNotificationListener_21);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param listener the listener
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @throws RemoteException if communication with the server was cut
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws ListenerNotFoundException if that listener was not registered
     * @throws IOException if communication with the server was cut
     */
    public void removeNotificationListener(ObjectName name, ObjectName listener,
            Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        try {
            ref.invoke(this, $method_removeNotificationListener_22,
                    new Object[] {name, listener,
                            delegationSubject}, $hash_removeNotificationListener_22);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (ListenerNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param listener the listener
     * @param filter the filter, serialized
     * @param handback the object handed back with each notification, serialized
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @throws RemoteException if communication with the server was cut
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws ListenerNotFoundException if that listener was not registered
     * @throws IOException if communication with the server was cut
     */
    public void removeNotificationListener(ObjectName name, ObjectName listener,
            MarshalledObject filter, MarshalledObject handback, Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        try {
            ref.invoke(this, $method_removeNotificationListener_23,
                    new Object[] {name, listener, filter, handback,
                            delegationSubject}, $hash_removeNotificationListener_23);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (ListenerNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Registers several listeners at once, and returns one identifier for each.
     *
     * <p>Several at once because each registration is a round trip over the network. The
     * identifiers are what later allows removing them without sending the filter again.
     *
     * @param names the MBeans' names
     * @param filters the filters, serialized
     * @param delegationSubjects on whose behalf each call is made
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws IOException if communication with the server was cut
     */
    public Integer[] addNotificationListeners(ObjectName[] names, MarshalledObject[] filters,
            Subject[] delegationSubjects)
            throws InstanceNotFoundException, IOException {
        try {
            final Object r = ref.invoke(this, $method_addNotificationListeners_24,
                    new Object[] {names, filters,
                            delegationSubjects}, $hash_addNotificationListeners_24);
            return (Integer[]) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Forwards the operation to the other side of the connection.
     *
     * @param name the MBean's name
     * @param listenerIDs the identifiers {@code addNotificationListeners} returned
     * @param delegationSubject on whose behalf it is done, or {@code null} for the authenticated
     *     one
     * @throws RemoteException if communication with the server was cut
     * @throws InstanceNotFoundException if there is no MBean with that name
     * @throws ListenerNotFoundException if that listener was not registered
     * @throws IOException if communication with the server was cut
     */
    public void removeNotificationListeners(ObjectName name, Integer[] listenerIDs,
            Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        try {
            ref.invoke(this, $method_removeNotificationListeners_25,
                    new Object[] {name, listenerIDs,
                            delegationSubject}, $hash_removeNotificationListeners_25);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (InstanceNotFoundException e) {
            throw e;
        } catch (ListenerNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }

    /**
     * Fetches the notifications that accumulated on the server side.
     *
     * <p>It is the heart of the model: notifications are not pushed to the client, the client
     * comes to get them. With RMI there is no way for the server to call the client without the
     * client being a remote object itself, and that does not survive a firewall.
     *
     * @param clientSequenceNumber from which sequence number to fetch
     * @param maxNotifications how many to fetch at most
     * @param timeout how long to wait if there is none
     * @return whatever the other side answers
     * @throws RemoteException if communication with the server was cut
     * @throws IOException if communication with the server was cut
     */
    public NotificationResult fetchNotifications(long clientSequenceNumber, int maxNotifications,
            long timeout)
            throws IOException {
        try {
            final Object r = ref.invoke(this, $method_fetchNotifications_26,
                    new Object[] {Long.valueOf(clientSequenceNumber),
                            Integer.valueOf(maxNotifications),
                            Long.valueOf(timeout)}, $hash_fetchNotifications_26);
            return (NotificationResult) r;
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }
}
