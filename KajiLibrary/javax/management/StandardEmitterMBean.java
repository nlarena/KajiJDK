package javax.management;

/**
 * Un {@link StandardMBean} que ademas emite notificaciones.
 *
 * <p>La razon de que exista como clase aparte y no como un `boolean` mas en `StandardMBean` es la
 * composicion: el emisor <b>se recibe ya construido</b>, y por eso el mismo
 * {@link NotificationBroadcasterSupport} puede estar compartido entre el MBean y el objeto
 * administrado, o ser uno con `Executor` propio, o ser uno que redefina `handleNotification` para
 * atajar oyentes rotos. Todo eso queda del lado de quien construye.
 *
 * <p>El detalle que hay que respetar: {@link #getNotificationInfo} contesta desde <b>el emisor</b>,
 * no desde la interfaz de administracion. La reflexion no puede saber que notificaciones emite un
 * objeto --no estan en ninguna firma-- asi que el unico que lo sabe es el emisor, y preguntarle a
 * otro seria devolver una lista vacia disfrazada de respuesta.
 */
public class StandardEmitterMBean extends StandardMBean implements NotificationEmitter {

    private final NotificationEmitter emisor;

    /**
     * @param emitter no puede ser `null`: sin emisor esta clase no agrega nada a `StandardMBean`,
     *        y aceptarlo solo aplazaria el fallo hasta el primer `addNotificationListener`
     */
    public <T> StandardEmitterMBean(T implementation, Class<T> mbeanInterface,
                                    NotificationEmitter emitter) {
        super(implementation, mbeanInterface, false);
        this.emisor = exigir(emitter);
    }

    /**
     * @param isMXBean tiene que ser `false`; ver la nota sobre MXBean en {@link StandardMBean}
     */
    public <T> StandardEmitterMBean(T implementation, Class<T> mbeanInterface, boolean isMXBean,
                                    NotificationEmitter emitter) {
        super(implementation, mbeanInterface, isMXBean);
        this.emisor = exigir(emitter);
    }

    /** Para subclasear: la implementacion es `this`. */
    protected StandardEmitterMBean(Class<?> mbeanInterface, NotificationEmitter emitter) {
        super(mbeanInterface, false);
        this.emisor = exigir(emitter);
    }

    protected StandardEmitterMBean(Class<?> mbeanInterface, boolean isMXBean,
                                   NotificationEmitter emitter) {
        super(mbeanInterface, isMXBean);
        this.emisor = exigir(emitter);
    }

    private static NotificationEmitter exigir(NotificationEmitter emitter) {
        if (emitter == null) {
            throw new IllegalArgumentException("El emisor no puede ser null");
        }
        return emitter;
    }

    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        emisor.removeNotificationListener(listener);
    }

    public void removeNotificationListener(NotificationListener listener,
                                           NotificationFilter filter, Object handback)
            throws ListenerNotFoundException {
        emisor.removeNotificationListener(listener, filter, handback);
    }

    public void addNotificationListener(NotificationListener listener, NotificationFilter filter,
                                        Object handback) {
        emisor.addNotificationListener(listener, filter, handback);
    }

    /** Del emisor, no de la introspeccion; ver la nota de la clase. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        return emisor.getNotificationInfo();
    }

    /**
     * Emite, si el emisor sabe emitir.
     *
     * <p>{@link NotificationEmitter} no declara `sendNotification` --emitir es cosa del que
     * implementa, no del contrato--, asi que hay que preguntar. Un emisor que no sea un
     * {@link NotificationBroadcasterSupport} y no sepa emitir es un error de construccion, y por
     * eso se dice ahora y no se traga en silencio.
     */
    public void sendNotification(Notification n) {
        if (!(emisor instanceof NotificationBroadcasterSupport)) {
            throw new ClassCastException(
                "El emisor no es un NotificationBroadcasterSupport y no sabe emitir: "
                + emisor.getClass().getName());
        }
        ((NotificationBroadcasterSupport) emisor).sendNotification(n);
    }
}
