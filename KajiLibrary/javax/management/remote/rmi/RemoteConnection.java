package javax.management.remote.rmi;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.MarshalledObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
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
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.remote.NotificationResult;
import javax.management.remote.TargetedNotification;

/**
 * The {@link MBeanServerConnection} the client sees, over an {@link RMIConnection}.
 *
 * <h2>What it translates</h2>
 *
 * <p>{@link MBeanServerConnection} is the convenient interface: it takes objects.
 * {@link RMIConnection} is the network one: it takes {@link MarshalledObject}. This class is the
 * step from one to the other, and all it does is wrap what goes out and add the {@code null}
 * subject to every call.
 *
 * <h2>The notifications the other way round</h2>
 *
 * <p>On the server side the notifications pile up in a queue. Here there is a thread that comes
 * to get them and delivers them among the local listeners. That thread is what lets the client
 * write {@code addNotificationListener} with a listener of its own and have it work, when over
 * the network only numbers travel.
 *
 * <p>The thread starts with the first listener and not before: a client that only reads
 * attributes has no reason to pay for a thread nor for a periodic query.
 *
 * <p>The filter travels to the server and is applied over there. It is what avoids bringing over
 * the network notifications the client was going to discard, which is precisely what a filter
 * has to avoid.
 */
final class RemoteConnection implements MBeanServerConnection {

    /** How many notifications are asked for at a time. */
    private static final int BATCH = 100;

    /** How long the server waits for notifications before answering that there are none, in ms. */
    private static final long WAIT = 500;

    private final RMIConnection conn;
    private final Map<Integer, Local> locals = new LinkedHashMap<Integer, Local>();

    private Thread pump;
    private volatile boolean closed;
    private long sequence = -1;

    /** A client listener, with the filter and the object handed back with each notification. */
    private static final class Local {
        final ObjectName name;
        final NotificationListener listener;
        final NotificationFilter filter;
        final Object handback;

        Local(ObjectName name, NotificationListener listener, NotificationFilter filter,
                Object handback) {
            this.name = name;
            this.listener = listener;
            this.filter = filter;
            this.handback = handback;
        }
    }

    RemoteConnection(RMIConnection conn) {
        this.conn = conn;
    }

    /** Stops fetching notifications. The connector calls it when closing. */
    void close() {
        closed = true;
        final Thread t;
        synchronized (this) {
            locals.clear();
            t = pump;
            pump = null;
        }
        if (t != null) {
            t.interrupt();
        }
    }

    /** Wraps an argument so that it can travel. */
    private static MarshalledObject<Object> wrap(Object o) throws IOException {
        if (o == null) {
            return null;
        }
        if (!(o instanceof Serializable)) {
            throw new IOException("Not serializable: " + o.getClass().getName());
        }
        return new MarshalledObject<Object>(o);
    }

    public ObjectInstance createMBean(String className, ObjectName name)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException {
        return conn.createMBean(className, name, null);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException {
        return conn.createMBean(className, name, loaderName, null);
    }

    public ObjectInstance createMBean(String className, ObjectName name, Object[] params,
            String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException {
        return conn.createMBean(className, name, wrap(params), signature, null);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
            Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException {
        return conn.createMBean(className, name, loaderName, wrap(params), signature, null);
    }

    public void unregisterMBean(ObjectName name)
            throws InstanceNotFoundException, MBeanRegistrationException, IOException {
        conn.unregisterMBean(name, null);
    }

    public ObjectInstance getObjectInstance(ObjectName name)
            throws InstanceNotFoundException, IOException {
        return conn.getObjectInstance(name, null);
    }

    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
        return conn.queryMBeans(name, wrap(query), null);
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
        return conn.queryNames(name, wrap(query), null);
    }

    public boolean isRegistered(ObjectName name) throws IOException {
        return conn.isRegistered(name, null);
    }

    public Integer getMBeanCount() throws IOException {
        return conn.getMBeanCount(null);
    }

    public Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                   ReflectionException, IOException {
        return conn.getAttribute(name, attribute, null);
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException, IOException {
        return conn.getAttributes(name, attributes, null);
    }

    public void setAttribute(ObjectName name, Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException,
                   InvalidAttributeValueException, MBeanException, ReflectionException,
                   IOException {
        conn.setAttribute(name, wrap(attribute), null);
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException, IOException {
        return conn.setAttributes(name, wrap(attributes), null);
    }

    public Object invoke(ObjectName name, String operationName, Object[] params,
            String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return conn.invoke(name, operationName, wrap(params), signature, null);
    }

    public String getDefaultDomain() throws IOException {
        return conn.getDefaultDomain(null);
    }

    public String[] getDomains() throws IOException {
        return conn.getDomains(null);
    }

    public MBeanInfo getMBeanInfo(ObjectName name)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException,
                   IOException {
        return conn.getMBeanInfo(name, null);
    }

    public boolean isInstanceOf(ObjectName name, String className)
            throws InstanceNotFoundException, IOException {
        return conn.isInstanceOf(name, className, null);
    }

    public void addNotificationListener(ObjectName name, NotificationListener listener,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, IOException {
        if (listener == null) {
            throw new IllegalArgumentException("Null listener");
        }
        final Integer[] ids = conn.addNotificationListeners(new ObjectName[] {name},
                new MarshalledObject[] {wrap(filter)}, null);
        synchronized (this) {
            locals.put(ids[0], new Local(name, listener, filter, handback));
            startPump();
        }
    }

    public void addNotificationListener(ObjectName name, ObjectName listener,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, IOException {
        conn.addNotificationListener(name, listener, wrap(filter), wrap(handback), null);
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        remove(name, listener, false, null, null);
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        remove(name, listener, true, filter, handback);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        conn.removeNotificationListener(name, listener, null);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        conn.removeNotificationListener(name, listener, wrap(filter), wrap(handback), null);
    }

    /**
     * Removes the local registrations that match.
     *
     * <p>Without the three data it removes all those of the same listener; with them it removes
     * only the one that matches in all three. It is the same rule as
     * {@code NotificationBroadcasterSupport}'s: one same listener may be registered several times
     * with different filters, and removing the wrong one would be worse than removing none.
     */
    private void remove(ObjectName name, NotificationListener listener, boolean withFilter,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        final List<Integer> ids = new ArrayList<Integer>();
        synchronized (this) {
            for (final Map.Entry<Integer, Local> e : locals.entrySet()) {
                final Local l = e.getValue();
                if (!l.name.equals(name) || l.listener != listener) {
                    continue;
                }
                if (withFilter && (l.filter != filter || l.handback != handback)) {
                    continue;
                }
                ids.add(e.getKey());
            }
            for (final Integer id : ids) {
                locals.remove(id);
            }
        }
        if (ids.isEmpty()) {
            throw new ListenerNotFoundException("Listener not found");
        }
        conn.removeNotificationListeners(name, ids.toArray(new Integer[ids.size()]), null);
    }

    /**
     * Starts the thread that fetches the notifications, if it was not running. With the lock held.
     */
    private void startPump() {
        if (pump != null || closed) {
            return;
        }
        pump = new Thread(new Runnable() {
            public void run() {
                fetch();
            }
        }, "JMX client notification fetcher");
        pump.setDaemon(true);
        pump.start();
    }

    /** The thread's cycle: ask, deliver, ask again. */
    private void fetch() {
        while (!closed) {
            final NotificationResult r;
            try {
                r = conn.fetchNotifications(sequence, BATCH, WAIT);
            } catch (IOException e) {
                // The connection was cut: there is nobody left to ask. Leaving is the right thing;
                // insisting would leave a thread spinning against a server that is no longer there.
                return;
            } catch (RuntimeException e) {
                return;
            }
            sequence = r.getNextSequenceNumber();
            for (final TargetedNotification tn : r.getTargetedNotifications()) {
                final Local l;
                synchronized (this) {
                    l = locals.get(tn.getListenerID());
                }
                if (l != null) {
                    deliver(l, tn.getNotification());
                }
            }
        }
    }

    private static void deliver(Local l, Notification n) {
        try {
            l.listener.handleNotification(n, l.handback);
        } catch (RuntimeException e) {
            // A listener that fails cannot break the delivery: the ones that follow are not to
            // blame, and this thread is the only one there is.
        }
    }
}
