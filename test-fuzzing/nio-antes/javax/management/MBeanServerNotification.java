package javax.management;

/**
 * The agent announces that an MBean was registered or unregistered.
 *
 * <p>It is the only notification the server itself emits. The delegate emits it, registered under
 * {@code JMImplementation:type=MBeanServerDelegate}, and it has to be listened to there: it is what
 * lets a console discover new MBeans without asking again every so often.
 *
 * <p>The affected MBean goes in {@link #getMBeanName()}, not in {@code getSource()}: the source is
 * the delegate, always.
 */
public class MBeanServerNotification extends Notification {

    private static final long serialVersionUID = 2876477500475969677L;

    /** An MBean was registered: {@value}. */
    public static final String REGISTRATION_NOTIFICATION = "JMX.mbean.registered";

    /** An MBean was unregistered: {@value}. */
    public static final String UNREGISTRATION_NOTIFICATION = "JMX.mbean.unregistered";

    /**
     * @serial the MBean that was registered or unregistered
     */
    private final ObjectName objectName;

    /**
     * @param type {@link #REGISTRATION_NOTIFICATION} or {@link #UNREGISTRATION_NOTIFICATION}
     * @param objectName the affected MBean
     */
    public MBeanServerNotification(String type, Object source, long sequenceNumber,
                                   ObjectName objectName) {
        super(type, source, sequenceNumber);
        this.objectName = objectName;
    }

    /** The MBean that was registered or unregistered. */
    public ObjectName getMBeanName() {
        return objectName;
    }

    public String toString() {
        return super.toString() + "[mbeanName=" + objectName + "]";
    }
}
