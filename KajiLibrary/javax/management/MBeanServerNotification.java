package javax.management;

/**
 * El agente avisa que se dio de alta o de baja un MBean.
 *
 * <p>Es la unica notificacion que emite el propio servidor. La emite el delegado, registrado bajo
 * {@code JMImplementation:type=MBeanServerDelegate}, y hay que escucharla ahi: es lo que permite a
 * una consola descubrir MBeans nuevos sin repreguntar cada tanto.
 *
 * <p>El MBean afectado va en {@link #getMBeanName()}, no en `getSource()`: la fuente es el delegado,
 * siempre.
 */
public class MBeanServerNotification extends Notification {

    private static final long serialVersionUID = 2876477500475969677L;

    /** Se registro un MBean: {@value}. */
    public static final String REGISTRATION_NOTIFICATION = "JMX.mbean.registered";

    /** Se dio de baja un MBean: {@value}. */
    public static final String UNREGISTRATION_NOTIFICATION = "JMX.mbean.unregistered";

    /**
     * @serial el MBean que se dio de alta o de baja
     */
    private final ObjectName objectName;

    /**
     * @param type {@link #REGISTRATION_NOTIFICATION} o {@link #UNREGISTRATION_NOTIFICATION}
     * @param objectName el MBean afectado
     */
    public MBeanServerNotification(String type, Object source, long sequenceNumber,
                                   ObjectName objectName) {
        super(type, source, sequenceNumber);
        this.objectName = objectName;
    }

    /** El MBean que se dio de alta o de baja. */
    public ObjectName getMBeanName() {
        return objectName;
    }

    public String toString() {
        return super.toString() + "[mbeanName=" + objectName + "]";
    }
}
