package javax.management;

/**
 * El MBean que representa al propio servidor de MBeans.
 *
 * <p>Cumple dos papeles distintos y por eso implementa dos interfaces. Como
 * {@link MBeanServerDelegateMBean} contesta las preguntas sobre el agente --quien lo implementa, que
 * version de la especificacion cumple--. Como {@link NotificationEmitter} es <b>el unico emisor</b>
 * de {@link MBeanServerNotification}: cuando cualquier MBean se registra o se va, el aviso sale de
 * aca y no del MBean afectado. Tiene sentido: el que se acaba de desregistrar ya no esta para
 * avisar que se fue.
 *
 * <p>Por eso vive siempre bajo el mismo nombre, {@link #DELEGATE_NAME}: quien quiera enterarse de
 * altas y bajas se registra ahi sin tener que descubrir nada.
 *
 * <p>El `MBeanServerId` se arma una sola vez, perezosamente, y de ahi en mas no cambia: es la
 * identidad del agente y tiene que sobrevivir a cualquier reconexion.
 */
public class MBeanServerDelegate implements MBeanServerDelegateMBean, NotificationEmitter {

    /** `JMImplementation:type=MBeanServerDelegate`, fijo por especificacion. */
    public static final ObjectName DELEGATE_NAME = nombreDelegado();

    private static ObjectName nombreDelegado() {
        try {
            return ObjectName.getInstance("JMImplementation:type=MBeanServerDelegate");
        } catch (MalformedObjectNameException e) {
            // Es una constante de la especificacion: si este literal no parsea, el ObjectName de
            // esta biblioteca esta roto y no hay forma sensata de seguir.
            throw new IllegalStateException("El nombre del delegado no parsea", e);
        }
    }

    /** Lo declarado: una sola clase de notificacion, con sus dos tipos. */
    private static final MBeanNotificationInfo[] INFO = new MBeanNotificationInfo[] {
        new MBeanNotificationInfo(
            new String[] { MBeanServerNotification.REGISTRATION_NOTIFICATION,
                           MBeanServerNotification.UNREGISTRATION_NOTIFICATION },
            MBeanServerNotification.class.getName(),
            "Alta y baja de MBeans en este agente")
    };

    private final NotificationBroadcasterSupport emisor = new NotificationBroadcasterSupport(INFO);

    /**
     * El numero de secuencia de las notificaciones que salen de aca.
     *
     * <p>Lo lleva el delegado y no cada MBean: el receptor tiene que poder detectar que perdio un
     * alta, y para eso la cuenta tiene que ser del emisor, que es uno solo.
     */
    private long secuencia = 1;

    private String id = null;

    public MBeanServerDelegate() {
    }

    /**
     * La identidad de este agente, estable durante toda su vida.
     *
     * <p>Se arma al primer pedido y no en el constructor porque la mayoria de los agentes nunca la
     * piden, y armarla implica consultar el entorno.
     */
    public synchronized String getMBeanServerId() {
        if (id == null) {
            String host;
            try {
                host = java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                // Un agente sin nombre de host sigue siendo un agente; lo que no puede es quedarse
                // sin identidad.
                host = "localhost";
            }
            id = host + "_" + System.currentTimeMillis();
        }
        return id;
    }

    /** Dato de la especificacion, no de esta implementacion. */
    public String getSpecificationName() {
        return "Java Management Extensions";
    }

    /** Dato de la especificacion, no de esta implementacion. */
    public String getSpecificationVersion() {
        return "1.4";
    }

    /** Dato de la especificacion, no de esta implementacion. */
    public String getSpecificationVendor() {
        return "Oracle Corporation";
    }

    /**
     * De aca en adelante son datos de <b>esta</b> implementacion, no del JDK. Devolver los del JDK
     * seria mentir sobre quien esta corriendo, que es justo lo que estos tres metodos existen para
     * responder.
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
        return emisor.getNotificationInfo();
    }

    public synchronized void addNotificationListener(NotificationListener listener,
                                                     NotificationFilter filter, Object handback)
            throws IllegalArgumentException {
        emisor.addNotificationListener(listener, filter, handback);
    }

    public synchronized void removeNotificationListener(NotificationListener listener,
                                                        NotificationFilter filter, Object handback)
            throws ListenerNotFoundException {
        emisor.removeNotificationListener(listener, filter, handback);
    }

    public synchronized void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        emisor.removeNotificationListener(listener);
    }

    /**
     * Emite, numerando.
     *
     * <p>Le pisa el numero de secuencia a la notificacion recibida. No es un descuido: la cuenta es
     * del delegado, y dejar que el que llama la elija romperia la unica garantia que la secuencia
     * da.
     */
    public void sendNotification(Notification notification) {
        synchronized (this) {
            if (notification.getSequenceNumber() <= 0) {
                notification.setSequenceNumber(secuencia++);
            }
        }
        emisor.sendNotification(notification);
    }
}
