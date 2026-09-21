package javax.management.modelmbean;

import java.lang.reflect.Method;
import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.RequiredModelMBean -- the implementation every JMX
 * platform has to ship.
 *
 * <p>It is given any object --{@link #setManagedResource}-- and a description of what to expose
 * --{@link #setModelMBeanInfo}--, and it becomes manageable without touching its code. It is the
 * only way to put a third party's class under JMX.
 *
 * <h2>How it resolves a query</h2>
 *
 * <p>Every attribute and every operation is resolved by <b>reflection</b> over the managed
 * object, using the corresponding {@code Info}'s descriptor:
 *
 * <ul>
 *   <li>for an attribute, the {@code getMethod} or {@code setMethod} field names the method;
 *   <li>for an operation, the descriptor's {@code name} field, or the operation's name if it is
 *       not there.
 * </ul>
 *
 * <p>Hence the configuration can lie and the error appear late: a {@code getMethod} that names a
 * method which does not exist is discovered only when somebody reads the attribute, and comes
 * out as {@link ReflectionException}.
 *
 * <h2>The two notice channels</h2>
 *
 * <p>The common notices and the attribute change ones go through <b>separate</b> listener lists.
 * See {@link ModelMBeanNotificationBroadcaster}: the second channel filters by attribute, and
 * that filtering happens here, not on the listener's side.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #load} and {@link #store} throw {@link MBeanException}. A model MBean's persistence
 * --writing itself to a file according to {@code persistPolicy} and {@code persistLocation}--
 * needs object serialization, which this library does not have. Both methods already declare
 * that exception for the "this implementation does not persist" case, so there is no lie: there
 * is an operation that says it cannot.
 *
 * <p>The descriptor fields that ask for behaviour beyond dispatch are <b>carried and not
 * obeyed</b>: {@code currencyTimeLimit} caches nothing --every read reaches the managed object--,
 * {@code log} and {@code logfile} write nothing, and {@code targetObject} and
 * {@code targetType} are not read, so everything is invoked on the managed resource.
 *
 * <p>Everything else --the dispatch by reflection, the descriptors, the two notice channels-- is
 * implemented.
 */
public class RequiredModelMBean implements ModelMBean, MBeanRegistration, NotificationEmitter {

    /** What to expose. */
    private ModelMBeanInfo modelMBeanInfo;

    /** Of what object. */
    private Object managedResource;

    /** The common channel's listeners. */
    private final NotificationBroadcasterSupport general = new NotificationBroadcasterSupport();

    /** Those of the attribute change channel, with the attribute each one listens to. */
    private final java.util.List<AttributeWatcher> watchers =
        new java.util.ArrayList<AttributeWatcher>();

    /** The notices' sequence number. */
    private long sequence = 1;

    /** The agent it was registered in, or null. See {@link #getClassLoaderRepository}. */
    private MBeanServer server;

    /**
     * With nothing configured; {@link #setModelMBeanInfo} has to be called before registering it.
     */
    public RequiredModelMBean() throws MBeanException, RuntimeOperationsException {
    }

    /**
     * With the description already set.
     *
     * @throws RuntimeOperationsException if it is null
     */
    public RequiredModelMBean(ModelMBeanInfo mbi)
        throws MBeanException, RuntimeOperationsException {
        setModelMBeanInfo(mbi);
    }

    /**
     * What to expose.
     *
     * @throws RuntimeOperationsException if it is null
     */
    public void setModelMBeanInfo(ModelMBeanInfo mbi)
        throws MBeanException, RuntimeOperationsException {
        if (mbi == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "ModelMBeanInfo is null"));
        }
        this.modelMBeanInfo = (ModelMBeanInfo) mbi.clone();
    }

    /**
     * Of what object.
     *
     * @param mr_type the only supported one is {@code "ObjectReference"}; see
     *     {@link InvalidTargetObjectTypeException}
     * @throws RuntimeOperationsException if the object is null
     */
    public void setManagedResource(Object mr, String mr_type)
        throws MBeanException, RuntimeOperationsException, InstanceNotFoundException,
               InvalidTargetObjectTypeException {
        if (mr == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Managed resource is null"));
        }
        if (mr_type != null && !"objectreference".equalsIgnoreCase(mr_type)) {
            throw new InvalidTargetObjectTypeException(mr_type);
        }
        this.managedResource = mr;
    }

    /**
     * Restores the persisted state.
     *
     * @throws MBeanException always in KajiLibrary; see the class note
     */
    public void load() throws MBeanException, RuntimeOperationsException, InstanceNotFoundException {
        throw new MBeanException(new UnsupportedOperationException(
            "KajiLibrary does not implement ModelMBean persistence"));
    }

    /**
     * Persists the state.
     *
     * @throws MBeanException always in KajiLibrary
     */
    public void store()
        throws MBeanException, RuntimeOperationsException, InstanceNotFoundException {
        throw new MBeanException(new UnsupportedOperationException(
            "KajiLibrary does not implement ModelMBean persistence"));
    }

    /** The configured description. */
    public MBeanInfo getMBeanInfo() {
        if (this.modelMBeanInfo == null) {
            return new MBeanInfo(getClass().getName(), "", null, null, null, null);
        }
        return (MBeanInfo) ((ModelMBeanInfoSupport) this.modelMBeanInfo).clone();
    }

    /**
     * Invokes an operation on the managed object.
     *
     * <p>See the class note on how the method is resolved, and on {@code targetObject} not being
     * read.
     *
     * @throws MBeanException if the operation is not declared
     * @throws ReflectionException if the method the descriptor names does not exist or fails
     */
    public Object invoke(String opName, Object[] opArgs, String[] sig)
        throws MBeanException, ReflectionException {
        if (opName == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Operation name is null"));
        }
        require();
        ModelMBeanOperationInfo info;
        try {
            info = this.modelMBeanInfo.getOperation(opName);
        } catch (Exception e) {
            throw new MBeanException(e);
        }
        if (info == null) {
            throw new MBeanException(new ServiceNotFound("No such operation: " + opName));
        }
        String methodName = fieldOrDefault(info, "name", opName);
        try {
            Class<?>[] types = resolveSignature(sig);
            Method m = this.managedResource.getClass().getMethod(methodName, types);
            return m.invoke(this.managedResource, opArgs);
        } catch (Exception e) {
            throw new ReflectionException(e, "Cannot invoke " + methodName);
        }
    }

    /**
     * Reads an attribute of the managed object.
     *
     * <p>It reaches the object every time: see the class note on {@code currencyTimeLimit}.
     *
     * @throws AttributeNotFoundException if it is not declared, or if it has no {@code getMethod}
     */
    public Object getAttribute(String attrName)
        throws AttributeNotFoundException, MBeanException, ReflectionException {
        if (attrName == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Attribute name is null"));
        }
        require();
        ModelMBeanAttributeInfo info;
        try {
            info = this.modelMBeanInfo.getAttribute(attrName);
        } catch (Exception e) {
            throw new MBeanException(e);
        }
        if (info == null) {
            throw new AttributeNotFoundException("No such attribute: " + attrName);
        }
        String getter = field(info, "getMethod");
        if (getter == null) {
            throw new AttributeNotFoundException(
                "Attribute " + attrName + " has no getMethod descriptor field");
        }
        try {
            Method m = this.managedResource.getClass().getMethod(getter, new Class<?>[0]);
            return m.invoke(this.managedResource, new Object[0]);
        } catch (Exception e) {
            throw new ReflectionException(e, "Cannot read " + attrName);
        }
    }

    /**
     * Reads several.
     *
     * <p>The ones that fail are <b>skipped</b> instead of throwing. It is what
     * {@code DynamicMBean}'s contract asks for: a console that asks for twenty attributes cannot be
     * left with none because a single one failed.
     */
    public AttributeList getAttributes(String[] attrNames) {
        AttributeList out = new AttributeList();
        if (attrNames == null) {
            return out;
        }
        int i = 0;
        while (i < attrNames.length) {
            try {
                out.add(new Attribute(attrNames[i], getAttribute(attrNames[i])));
            } catch (Exception e) {
                // Skipped on purpose; see the method's note.
            }
            i = i + 1;
        }
        return out;
    }

    /**
     * Writes an attribute, and reports the change.
     *
     * <p>The notice goes out <b>after</b> writing and only if writing worked, with the old value
     * and the new one. It is what lets a listener trust that the change happened.
     *
     * @throws AttributeNotFoundException if it is not declared, or if it has no {@code setMethod}
     */
    public void setAttribute(Attribute attribute)
        throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException,
               ReflectionException {
        if (attribute == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Attribute is null"));
        }
        require();
        String attrName = attribute.getName();
        ModelMBeanAttributeInfo info;
        try {
            info = this.modelMBeanInfo.getAttribute(attrName);
        } catch (Exception e) {
            throw new MBeanException(e);
        }
        if (info == null) {
            throw new AttributeNotFoundException("No such attribute: " + attrName);
        }
        String setter = field(info, "setMethod");
        if (setter == null) {
            throw new AttributeNotFoundException(
                "Attribute " + attrName + " has no setMethod descriptor field");
        }
        Object oldValue = null;
        try {
            oldValue = getAttribute(attrName);
        } catch (Exception e) {
            // Without an old value the notice goes out all the same, with null: not being able to
            // read it
                        // does not prevent writing.
        }
        try {
            Method[] all = this.managedResource.getClass().getMethods();
            Method chosen = null;
            int i = 0;
            while (i < all.length) {
                if (all[i].getName().equals(setter) && all[i].getParameterCount() == 1) {
                    chosen = all[i];
                    break;
                }
                i = i + 1;
            }
            if (chosen == null) {
                throw new NoSuchMethodException(setter);
            }
            chosen.invoke(this.managedResource, new Object[] {attribute.getValue()});
        } catch (Exception e) {
            throw new ReflectionException(e, "Cannot write " + attrName);
        }
        sendAttributeChangeNotification(new Attribute(attrName, oldValue), attribute);
    }

    /** Writes several; the ones that fail are skipped, the same as when reading. */
    public AttributeList setAttributes(AttributeList attributes) {
        AttributeList out = new AttributeList();
        if (attributes == null) {
            return out;
        }
        for (Object o : attributes) {
            if (!(o instanceof Attribute)) {
                continue;
            }
            try {
                setAttribute((Attribute) o);
                out.add(o);
            } catch (Exception e) {
                // Skipped on purpose.
            }
        }
        return out;
    }

    // ---- common channel -----------------------------------------------------------------------

    /** Registers a listener for the common notices. */
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter,
                                        Object handback)
        throws IllegalArgumentException {
        this.general.addNotificationListener(listener, filter, handback);
    }

    /** Removes it. */
    public void removeNotificationListener(NotificationListener listener)
        throws ListenerNotFoundException {
        this.general.removeNotificationListener(listener);
    }

    /** The same, with the exact filter and handback. */
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter,
                                           Object handback) throws ListenerNotFoundException {
        this.general.removeNotificationListener(listener, filter, handback);
    }

    /** Sends that notice through the common channel. */
    public void sendNotification(Notification ntfyObj)
        throws MBeanException, RuntimeOperationsException {
        if (ntfyObj == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Notification is null"));
        }
        this.general.sendNotification(ntfyObj);
    }

    /** The same, building the notice from the text. */
    public void sendNotification(String ntfyText)
        throws MBeanException, RuntimeOperationsException {
        if (ntfyText == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Notification text is null"));
        }
        sendNotification(new Notification("jmx.modelmbean.generic", this, nextSequence(), ntfyText));
    }

    /** What this MBean declares it sends through the common channel. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        if (this.modelMBeanInfo == null) {
            return new MBeanNotificationInfo[0];
        }
        return this.modelMBeanInfo.getNotifications();
    }

    // ---- attribute change channel -----------------------------------------------------------

    /**
     * Registers a listener for one attribute's changes.
     *
     * @param attributeName which one; null means all
     * @throws IllegalArgumentException if the listener is null
     */
    public void addAttributeChangeNotificationListener(NotificationListener inlistener,
                                                       String inAttributeName, Object inhandback)
        throws MBeanException, RuntimeOperationsException, IllegalArgumentException {
        if (inlistener == null) {
            throw new IllegalArgumentException("Listener to be registered is null");
        }
        synchronized (this.watchers) {
            this.watchers.add(new AttributeWatcher(inlistener, inAttributeName, inhandback));
        }
    }

    /**
     * Removes it.
     *
     * @throws ListenerNotFoundException if it was not registered for that attribute
     */
    public void removeAttributeChangeNotificationListener(NotificationListener inlistener,
                                                          String inAttributeName)
        throws MBeanException, RuntimeOperationsException, ListenerNotFoundException {
        if (inlistener == null) {
            throw new ListenerNotFoundException("Listener to be removed is null");
        }
        synchronized (this.watchers) {
            int i = 0;
            boolean removed = false;
            while (i < this.watchers.size()) {
                AttributeWatcher w = this.watchers.get(i);
                boolean sameName = (inAttributeName == null)
                    ? w.attribute == null : inAttributeName.equals(w.attribute);
                if (w.listener == inlistener && sameName) {
                    this.watchers.remove(i);
                    removed = true;
                } else {
                    i = i + 1;
                }
            }
            if (!removed) {
                throw new ListenerNotFoundException(
                    "Listener not registered for attribute " + inAttributeName);
            }
        }
    }

    /** Sends that notice to the listeners of the attribute it names. */
    public void sendAttributeChangeNotification(AttributeChangeNotification ntfyObj)
        throws MBeanException, RuntimeOperationsException {
        if (ntfyObj == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "AttributeChangeNotification is null"));
        }
        java.util.List<AttributeWatcher> copy;
        synchronized (this.watchers) {
            copy = new java.util.ArrayList<AttributeWatcher>(this.watchers);
        }
        String name = ntfyObj.getAttributeName();
        int i = 0;
        while (i < copy.size()) {
            AttributeWatcher w = copy.get(i);
            // A listener without an attribute hears them all; see the class note.
            if (w.attribute == null || w.attribute.equals(name)) {
                w.listener.handleNotification(ntfyObj, w.handback);
            }
            i = i + 1;
        }
    }

    /**
     * The same, building the notice from the old value and the new one.
     *
     * @throws RuntimeOperationsException if the two attributes do not have the same name
     */
    public void sendAttributeChangeNotification(Attribute inOldVal, Attribute inNewVal)
        throws MBeanException, RuntimeOperationsException {
        if (inOldVal == null || inNewVal == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Attribute object passed in is null"));
        }
        if (!inOldVal.getName().equals(inNewVal.getName())) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Attribute names are not the same"));
        }
        Object newValue = inNewVal.getValue();
        String type = (newValue == null) ? "java.lang.Object" : newValue.getClass().getName();
        sendAttributeChangeNotification(new AttributeChangeNotification(this, nextSequence(),
            System.currentTimeMillis(), inNewVal.getName() + " changed from "
                + inOldVal.getValue() + " to " + newValue,
            inNewVal.getName(), type, inOldVal.getValue(), newValue));
    }

    // ---- MBeanRegistration -------------------------------------------------------------------

    /** It keeps the agent --{@link #getClassLoaderRepository} needs it-- and accepts the name. */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.server = server;
        return name;
    }

    /**
     * The class loaders of the agent this MBean is registered in.
     *
     * <p>It is {@code protected} because it is there for the subclasses: a model MBean that
     * resolves class names --when invoking an operation, when deserializing an argument-- has to
     * look for them where the agent looks, and not in this class's classpath. Looking here would
     * give the classic JMX error: the class exists in the agent and "is not found".
     *
     * @return null if it has not been registered in any agent yet
     */
    protected javax.management.loading.ClassLoaderRepository getClassLoaderRepository() {
        return (this.server == null) ? null : this.server.getClassLoaderRepository();
    }

    /** Nothing to do. */
    public void postRegister(Boolean registrationDone) {
    }

    /** Nothing to do. */
    public void preDeregister() throws Exception {
    }

    /** Nothing to do. */
    public void postDeregister() {
    }

    // ---- internals ---------------------------------------------------------------------------

    /** The next sequence number. */
    private synchronized long nextSequence() {
        long seq = this.sequence;
        this.sequence = this.sequence + 1;
        return seq;
    }

    /** That it is configured. */
    private void require() throws MBeanException {
        if (this.modelMBeanInfo == null) {
            throw new MBeanException(new IllegalStateException("ModelMBeanInfo is not set"));
        }
        if (this.managedResource == null) {
            throw new MBeanException(new IllegalStateException("Managed resource is not set"));
        }
    }

    /** That descriptor field's value, or null. */
    private static String field(javax.management.DescriptorAccess info, String name) {
        Object v = info.getDescriptor().getFieldValue(name);
        return (v == null) ? null : v.toString();
    }

    /** The same, with a default value. */
    private static String fieldOrDefault(javax.management.DescriptorAccess info, String name,
                                         String fallback) {
        String v = field(info, name);
        return (v == null) ? fallback : v;
    }

    /** A signature's types, resolved by name. */
    private static Class<?>[] resolveSignature(String[] sig) throws ClassNotFoundException {
        if (sig == null) {
            return new Class<?>[0];
        }
        Class<?>[] out = new Class<?>[sig.length];
        int i = 0;
        while (i < sig.length) {
            out[i] = primitiveOrClass(sig[i]);
            i = i + 1;
        }
        return out;
    }

    /** A type name, primitives included, which {@code Class.forName} does not resolve. */
    private static Class<?> primitiveOrClass(String name) throws ClassNotFoundException {
        if ("int".equals(name)) {
            return int.class;
        }
        if ("long".equals(name)) {
            return long.class;
        }
        if ("boolean".equals(name)) {
            return boolean.class;
        }
        if ("byte".equals(name)) {
            return byte.class;
        }
        if ("char".equals(name)) {
            return char.class;
        }
        if ("short".equals(name)) {
            return short.class;
        }
        if ("float".equals(name)) {
            return float.class;
        }
        if ("double".equals(name)) {
            return double.class;
        }
        if ("void".equals(name)) {
            return void.class;
        }
        return Class.forName(name);
    }

    /** A listener of the change channel, with the attribute it hears. */
    private static final class AttributeWatcher {

        private final NotificationListener listener;

        /** Null means all. */
        private final String attribute;

        private final Object handback;

        AttributeWatcher(NotificationListener listener, String attribute, Object handback) {
            this.listener = listener;
            this.attribute = attribute;
            this.handback = handback;
        }
    }

    /** What gets wrapped when the operation asked for is not declared. */
    private static final class ServiceNotFound extends Exception {

        private static final long serialVersionUID = 1L;

        ServiceNotFound(String message) {
            super(message);
        }
    }
}
