package javax.management;

/**
 * The MBean that represents the MBean server itself.
 *
 * <p>It plays two different roles and that is why it implements two interfaces. As
 * {@link MBeanServerDelegateMBean} it answers questions about the agent --who implements it, which
 * version of the specification it follows. As {@link NotificationEmitter} it is <b>the only
 * emitter</b> of {@link MBeanServerNotification}: when any MBean registers or leaves, the notice
 * comes from here and not from the affected MBean. It makes sense: the one that was just
 * unregistered is no longer there to say it left.
 *
 * <p>That is why it always lives under the same name, {@link #DELEGATE_NAME}: whoever wants to hear
 * about registrations and unregistrations registers there without having to discover anything.
 *
 * <p>The {@code MBeanServerId} is built only once, lazily, and from then on it does not change: it
 * is the agent's identity and has to survive any reconnection.
 */
public class MBeanServerDelegate implements MBeanServerDelegateMBean, NotificationEmitter {

    /** {@code JMImplementation:type=MBeanServerDelegate}, fixed by the specification. */
    public static final ObjectName DELEGATE_NAME = delegateName();

    private static ObjectName delegateName() {
        try {
            return ObjectName.getInstance("JMImplementation:type=MBeanServerDelegate");
        } catch (MalformedObjectNameException e) {
            // It is a constant of the specification: if this literal does not parse, this library's
            // ObjectName is broken and there is no sensible way to carry on.
            throw new IllegalStateException("The delegate's name does not parse", e);
        }
    }

    /** What is declared: a single notification class, with its two types. */
    private static final MBeanNotificationInfo[] INFO = new MBeanNotificationInfo[] {
        new MBeanNotificationInfo(
            new String[] { MBeanServerNotification.REGISTRATION_NOTIFICATION,
                           MBeanServerNotification.UNREGISTRATION_NOTIFICATION },
            MBeanServerNotification.class.getName(),
            "Registration and unregistration of MBeans in this agent")
    };

    private final NotificationBroadcasterSupport emitter = new NotificationBroadcasterSupport(INFO);

    /**
     * The sequence number of the notifications that come out of here.
     *
     * <p>The delegate keeps it and not each MBean: the receiver has to be able to detect that it
     * missed a registration, and for that the count has to belong to the emitter, which is only
     * one.
     */
    private long sequence = 1;

    private String id = null;

    public MBeanServerDelegate() {
    }

    /**
     * This agent's identity, stable throughout its life.
     *
     * <p>It is built on the first request and not in the constructor because most agents never ask
     * for it, and building it means querying the environment.
     */
    public synchronized String getMBeanServerId() {
        if (id == null) {
            String host;
            try {
                host = java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                // An agent without a host name is still an agent; what it cannot do is be left
                // without an identity.
                host = "localhost";
            }
            id = host + "_" + System.currentTimeMillis();
        }
        return id;
    }

    /** Data from the specification, not from this implementation. */
    public String getSpecificationName() {
        return "Java Management Extensions";
    }

    /** Data from the specification, not from this implementation. */
    public String getSpecificationVersion() {
        return "1.4";
    }

    /** Data from the specification, not from this implementation. */
    public String getSpecificationVendor() {
        return "Oracle Corporation";
    }

    /**
     * From here on it is data of <b>this</b> implementation, not the JDK's. Returning the JDK's
     * would be lying about who is running, which is exactly what these three methods exist to
     * answer.
     */
    public String getImplementationName() {
        return "KajiJDK JMX";
    }

    public String getImplementationVersion() {
        return "1.0";
    }

    public String getImplementationVendor() {
        return "KajiJDK";
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return emitter.getNotificationInfo();
    }

    public synchronized void addNotificationListener(NotificationListener listener,
                                                     NotificationFilter filter, Object handback)
            throws IllegalArgumentException {
        emitter.addNotificationListener(listener, filter, handback);
    }

    public synchronized void removeNotificationListener(NotificationListener listener,
                                                        NotificationFilter filter, Object handback)
            throws ListenerNotFoundException {
        emitter.removeNotificationListener(listener, filter, handback);
    }

    public synchronized void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        emitter.removeNotificationListener(listener);
    }

    /**
     * Emits, numbering.
     *
     * <p>A notification whose sequence number is zero or negative gets the delegate's next number;
     * one that already carries a positive number keeps it. The JDK does the same (it numbers those
     * below 1). (An earlier note said the number is always overwritten, on purpose; the code does
     * not do that.)
     */
    public void sendNotification(Notification notification) {
        synchronized (this) {
            if (notification.getSequenceNumber() <= 0) {
                notification.setSequenceNumber(sequence++);
            }
        }
        emitter.sendNotification(notification);
    }
}
