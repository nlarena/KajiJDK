package javax.management.remote.rmi;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.server.Unreferenced;
import java.util.ArrayList;
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
import javax.management.MBeanServer;
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
import javax.security.auth.Subject;

/**
 * A client's connection, on the server side.
 *
 * <h2>Almost everything is forwarding</h2>
 *
 * <p>Each method takes the {@link MBeanServer} from the {@link RMIServerImpl} that created it
 * and passes the call on. The only thing it adds is what the remote API needs and the local one
 * does not: deserializing the arguments that came wrapped, and keeping the notification queue.
 *
 * <h2>The {@link MarshalledObject}s</h2>
 *
 * <p>The arguments that could be of classes the server does not know travel serialized. Here
 * they are opened with {@link #unwrap}, and that is the moment --the only one-- when it can be
 * chosen with which class loader they are built. If they travelled as objects, RMI would already
 * have deserialized them before getting here, with whichever loader it happened to use.
 *
 * <h2>The notification queue</h2>
 *
 * <p>The client does not receive notifications: it comes to get them with
 * {@link #fetchNotifications}. Each listener it registers is noted down with a number, and what
 * arrives is kept in a queue with a growing sequence number. The client asks "from number N"
 * and takes whatever is there.
 *
 * <p>The queue has a cap. When it fills up the oldest ones are thrown away, and the number of the
 * oldest one left goes up: that way the client that fell asleep learns that it lost things,
 * instead of getting a silent gap.
 *
 * <h2>State in this library</h2>
 *
 * <p>This class <strong>works entirely</strong>, and it is the only one in the package of which
 * that can be said. It does not need the RMI transport: the transport is what would bring the
 * calls here, and if it is built by hand --which is what the {@code java/RMI1.java} test does--
 * it forwards to the {@link MBeanServer} just as in the JDK.
 *
 * @since 1.5
 */
public class RMIConnectionImpl implements RMIConnection, Unreferenced {

    /** How many notifications are kept before the old ones start being thrown away. */
    private static final int MAX_QUEUE = 1000;

    private final RMIServerImpl server;
    private final String connectionId;
    private final ClassLoader defaultLoader;
    private final Subject subject;

    private final List<TargetedNotification> queue = new ArrayList<TargetedNotification>();
    private final List<Registration> listeners = new ArrayList<Registration>();

    /** The sequence number of the first notification still in the queue. */
    private long firstSeq;

    /** The number the next notification to arrive will get. */
    private long nextSeq;

    private int nextListenerId;
    private boolean closed;

    /** A listener registered by the client, with the number that identifies it. */
    private static final class Registration {
        final Integer id;
        final ObjectName name;
        final NotificationListener listener;

        Registration(Integer id, ObjectName name, NotificationListener listener) {
            this.id = id;
            this.name = name;
            this.listener = listener;
        }
    }

    /**
     * A connection for that client.
     *
     * @param rmiServer the server that creates it
     * @param connectionId the connection's identifier
     * @param defaultClassLoader the loader deserialization uses, or {@code null}
     * @param subject who authenticated, or {@code null}
     * @param env the configuration properties, or {@code null}
     * @throws NullPointerException if {@code rmiServer} or {@code connectionId} are {@code null}
     */
    public RMIConnectionImpl(RMIServerImpl rmiServer, String connectionId,
            ClassLoader defaultClassLoader, Subject subject, Map<String, ?> env) {
        if (rmiServer == null || connectionId == null) {
            throw new NullPointerException("Illegal null argument");
        }
        this.server = rmiServer;
        this.connectionId = connectionId;
        this.defaultLoader = defaultClassLoader;
        this.subject = subject;
        final long now = System.currentTimeMillis();
        this.firstSeq = now;
        this.nextSeq = now;
    }

    /**
     * This connection's identifier.
     *
     * @return the identifier
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Closes the connection and tells the server.
     *
     * <p>It is re-entrant on purpose: the server may close it while it is telling the server that
     * it closed. Without the flag, those two paths would call each other in a circle.
     *
     * @throws IOException if the server could not process the close
     */
    public void close() throws IOException {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            queue.clear();
            listeners.clear();
        }
        server.clientClosed(this);
    }

    /**
     * RMI's notice that there is nobody left on the other side.
     *
     * <p>It is the safety net for the client that dies without closing. Without this, its listeners
     * would stay registered in the MBeanServer for ever.
     */
    public void unreferenced() {
        try {
            close();
        } catch (IOException e) {
            // There is nobody to tell: on the other side there is no longer anybody, which is
            // exactly why we got here. Closing is what mattered and it has already been attempted.
        }
    }

    /** The MBeanServer this connection works against, checking that it is still open. */
    private synchronized MBeanServer mbs() throws IOException {
        if (closed) {
            throw new IOException("The connection has been closed.");
        }
        final MBeanServer m = server.getMBeanServer();
        if (m == null) {
            throw new IllegalStateException("Not attached to an MBean server");
        }
        return m;
    }

    /**
     * Opens an argument that came serialized.
     *
     * <p>A {@code null} wrapper and a wrapper of {@code null} are different things and both are
     * legitimate: the first means "the argument was not sent" and the second "{@code null} was
     * sent". Both give {@code null} here, which is right, but by different paths.
     *
     * @param mo the wrapper, or {@code null}
     * @return what it carried inside
     * @throws IOException if the bytes could not be read, or carried a class that is not there
     */
    private Object unwrap(MarshalledObject<?> mo) throws IOException {
        if (mo == null) {
            return null;
        }
        try {
            return mo.get();
        } catch (ClassNotFoundException e) {
            throw new IOException("Class not found: " + e.getMessage(), e);
        }
    }

    public ObjectInstance createMBean(String className, ObjectName name, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException {
        return mbs().createMBean(className, name);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
            Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException {
        return mbs().createMBean(className, name, loaderName);
    }

    public ObjectInstance createMBean(String className, ObjectName name,
            MarshalledObject params, String[] signature, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException {
        return mbs().createMBean(className, name, (Object[]) unwrap(params), signature);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
            MarshalledObject params, String[] signature, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException {
        return mbs().createMBean(className, name, loaderName,
                (Object[]) unwrap(params), signature);
    }

    public void unregisterMBean(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, MBeanRegistrationException, IOException {
        mbs().unregisterMBean(name);
    }

    public ObjectInstance getObjectInstance(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, IOException {
        return mbs().getObjectInstance(name);
    }

    public Set<ObjectInstance> queryMBeans(ObjectName name, MarshalledObject query,
            Subject delegationSubject) throws IOException {
        return mbs().queryMBeans(name, (QueryExp) unwrap(query));
    }

    public Set<ObjectName> queryNames(ObjectName name, MarshalledObject query,
            Subject delegationSubject) throws IOException {
        return mbs().queryNames(name, (QueryExp) unwrap(query));
    }

    public boolean isRegistered(ObjectName name, Subject delegationSubject) throws IOException {
        return mbs().isRegistered(name);
    }

    public Integer getMBeanCount(Subject delegationSubject) throws IOException {
        return mbs().getMBeanCount();
    }

    public Object getAttribute(ObjectName name, String attribute, Subject delegationSubject)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                   ReflectionException, IOException {
        return mbs().getAttribute(name, attribute);
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes,
            Subject delegationSubject)
            throws InstanceNotFoundException, ReflectionException, IOException {
        return mbs().getAttributes(name, attributes);
    }

    public void setAttribute(ObjectName name, MarshalledObject attribute,
            Subject delegationSubject)
            throws InstanceNotFoundException, AttributeNotFoundException,
                   InvalidAttributeValueException, MBeanException, ReflectionException,
                   IOException {
        mbs().setAttribute(name, (Attribute) unwrap(attribute));
    }

    public AttributeList setAttributes(ObjectName name, MarshalledObject attributes,
            Subject delegationSubject)
            throws InstanceNotFoundException, ReflectionException, IOException {
        return mbs().setAttributes(name, (AttributeList) unwrap(attributes));
    }

    public Object invoke(ObjectName name, String operationName, MarshalledObject params,
            String[] signature, Subject delegationSubject)
            throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return mbs().invoke(name, operationName, (Object[]) unwrap(params), signature);
    }

    public String getDefaultDomain(Subject delegationSubject) throws IOException {
        return mbs().getDefaultDomain();
    }

    public String[] getDomains(Subject delegationSubject) throws IOException {
        return mbs().getDomains();
    }

    public MBeanInfo getMBeanInfo(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException,
                   IOException {
        return mbs().getMBeanInfo(name);
    }

    public boolean isInstanceOf(ObjectName name, String className, Subject delegationSubject)
            throws InstanceNotFoundException, IOException {
        return mbs().isInstanceOf(name, className);
    }

    public void addNotificationListener(ObjectName name, ObjectName listener,
            MarshalledObject filter, MarshalledObject handback, Subject delegationSubject)
            throws InstanceNotFoundException, IOException {
        mbs().addNotificationListener(name, listener, (NotificationFilter) unwrap(filter),
                unwrap(handback));
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener,
            Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        mbs().removeNotificationListener(name, listener);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener,
            MarshalledObject filter, MarshalledObject handback, Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        mbs().removeNotificationListener(name, listener, (NotificationFilter) unwrap(filter),
                unwrap(handback));
    }

    /**
     * Registers several listeners and returns each one's number.
     *
     * <p>The listener registered in the MBeanServer is not the client's: it is one from here, whose
     * only job is to put what arrives in the queue with the number that corresponds to it. That
     * number is what later lets the client know which of its listeners each notification goes to.
     *
     * <p>Several at once because each registration would be a trip over the network.
     *
     * @param names the MBeans' names
     * @param filters the filters, serialized
     * @param delegationSubjects on whose behalf each one is made
     * @return one number per listener, in the same order
     * @throws InstanceNotFoundException if one of the MBeans is not there
     * @throws IOException if the connection is closed or a filter could not be opened
     * @throws IllegalArgumentException if the arrays are not the same length
     */
    public Integer[] addNotificationListeners(ObjectName[] names, MarshalledObject[] filters,
            Subject[] delegationSubjects) throws InstanceNotFoundException, IOException {
        if (names == null || filters == null) {
            throw new IllegalArgumentException("Got null arguments.");
        }
        if (names.length != filters.length
                || delegationSubjects != null && delegationSubjects.length != names.length) {
            throw new IllegalArgumentException("The value lengths of 3 parameters are not same.");
        }
        final MBeanServer m = mbs();
        final Integer[] ids = new Integer[names.length];
        final List<Registration> added = new ArrayList<Registration>();
        try {
            for (int i = 0; i < names.length; i++) {
                if (names[i] == null) {
                    throw new IllegalArgumentException("Null Object name.");
                }
                final NotificationFilter f = (NotificationFilter) unwrap(filters[i]);
                final Integer id;
                synchronized (this) {
                    id = Integer.valueOf(nextListenerId++);
                }
                final Registration r = new Registration(id, names[i], new Forwarder(id));
                m.addNotificationListener(names[i], r.listener, f, null);
                added.add(r);
                ids[i] = id;
            }
        } catch (Exception e) {
            // If one fails, the ones already added are undone: leaving half of them registered
            // would be worse than registering none, because the client believes none was left and
            // nobody removes them.
            for (final Registration r : added) {
                try {
                    m.removeNotificationListener(r.name, r.listener);
                } catch (Exception other) {
                    // It is already being undone because of a failure; that one cannot be removed
                    // does not change which problem has to be reported.
                }
            }
            if (e instanceof InstanceNotFoundException) {
                throw (InstanceNotFoundException) e;
            }
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new IOException(e.getMessage(), e);
        }
        synchronized (this) {
            listeners.addAll(added);
        }
        return ids;
    }

    /**
     * Removes the listeners with those numbers.
     *
     * @param name the MBean's name
     * @param listenerIDs the numbers {@link #addNotificationListeners} returned
     * @param delegationSubject on whose behalf it is done
     * @throws InstanceNotFoundException if the MBean is not there
     * @throws ListenerNotFoundException if one of those numbers does not match a listener
     * @throws IOException if the connection is closed
     * @throws IllegalArgumentException if some number is {@code null}
     */
    public void removeNotificationListeners(ObjectName name, Integer[] listenerIDs,
            Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        if (name == null || listenerIDs == null) {
            throw new IllegalArgumentException("Illegal null parameter");
        }
        final MBeanServer m = mbs();
        for (final Integer id : listenerIDs) {
            if (id == null) {
                throw new IllegalArgumentException("Null listener ID");
            }
            Registration r = null;
            synchronized (this) {
                for (final Registration x : listeners) {
                    if (x.id.equals(id) && x.name.equals(name)) {
                        r = x;
                        break;
                    }
                }
                if (r != null) {
                    listeners.remove(r);
                }
            }
            if (r == null) {
                throw new ListenerNotFoundException("Listener id: " + id);
            }
            m.removeNotificationListener(name, r.listener);
        }
    }

    /**
     * Fetches the notifications that accumulated since that sequence number.
     *
     * <p>It returns the number of the oldest one still in the queue and the one the next will get.
     * If the first is greater than what the client asked for, the client knows it lost
     * notifications because it took too long to come back.
     *
     * <p>The {@code timeout} is how long to wait if there is none. It is what turns this into
     * something usable: without the wait, a client that wants to be up to date would have to ask in
     * a tight loop.
     *
     * @param clientSequenceNumber from which number to fetch; negative means "from now"
     * @param maxNotifications how many to fetch at most
     * @param timeout how long to wait if there is none, in milliseconds
     * @return whatever there was
     * @throws IOException if the connection is closed
     * @throws IllegalArgumentException if {@code maxNotifications} or {@code timeout} are negative
     */
    public NotificationResult fetchNotifications(long clientSequenceNumber, int maxNotifications,
            long timeout) throws IOException {
        if (maxNotifications < 0 || timeout < 0) {
            throw new IllegalArgumentException("Illegal negative argument");
        }
        final long deadline = System.currentTimeMillis() + timeout;
        synchronized (this) {
            if (closed) {
                throw new IOException("The connection has been closed.");
            }
            long from = clientSequenceNumber < 0 ? nextSeq : clientSequenceNumber;
            while (from >= nextSeq && !closed) {
                final long left = deadline - System.currentTimeMillis();
                if (left <= 0) {
                    break;
                }
                try {
                    wait(left);
                } catch (InterruptedException e) {
                    // Being interrupted while waiting is not an error: it means "return whatever
                    // you have now". The flag is restored so that whoever interrupted finds out all
                    // the same.
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (closed) {
                throw new IOException("The connection has been closed.");
            }
            if (from < firstSeq) {
                from = firstSeq;
            }
            final List<TargetedNotification> out = new ArrayList<TargetedNotification>();
            for (long i = from; i < nextSeq && out.size() < maxNotifications; i++) {
                out.add(queue.get((int) (i - firstSeq)));
            }
            final long next = from + out.size();
            return new NotificationResult(firstSeq, next,
                    out.toArray(new TargetedNotification[out.size()]));
        }
    }

    /** Puts a notification in the queue and wakes whoever is waiting. */
    private synchronized void enqueue(Integer id, Notification n) {
        if (closed) {
            return;
        }
        queue.add(new TargetedNotification(n, id));
        nextSeq++;
        while (queue.size() > MAX_QUEUE) {
            // The oldest one is thrown away and the first one's number goes up. That the number
            // goes up is precisely the notice: the client compares it with what it had asked for
            // and knows some got away from it.
            queue.remove(0);
            firstSeq++;
        }
        notifyAll();
    }

    /** The listener registered in the MBeanServer in the client's stead. */
    private final class Forwarder implements NotificationListener {
        private final Integer id;

        Forwarder(Integer id) {
            this.id = id;
        }

        public void handleNotification(Notification notification, Object handback) {
            enqueue(id, notification);
        }
    }

    /**
     * The connection's identifier, for the log.
     *
     * @return a description
     */
    @Override
    public String toString() {
        return super.toString() + ": connectionId=" + connectionId;
    }
}
