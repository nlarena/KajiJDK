package javax.management;

/**
 * A {@link StandardMBean} that also emits notifications.
 *
 * <p>The reason it exists as a separate class and not as one more {@code boolean} in
 * {@code StandardMBean} is composition: the emitter <b>is received already built</b>, and so the
 * same {@link NotificationBroadcasterSupport} can be shared between the MBean and the managed
 * object, or be one with an {@code Executor} of its own, or one that redefines
 * {@code handleNotification} to catch broken listeners. All of that stays on the side of whoever
 * builds it.
 *
 * <p>The detail to respect: {@link #getNotificationInfo} answers from <b>the emitter</b>, not from
 * the management interface. Reflection cannot know which notifications an object emits --they are
 * in no signature-- so the only one that knows is the emitter, and asking anyone else would be
 * returning an empty list dressed up as an answer.
 */
public class StandardEmitterMBean extends StandardMBean implements NotificationEmitter {

    private final NotificationEmitter emitter;

    /**
     * @param emitter cannot be {@code null}: without an emitter this class adds nothing to
     *        {@code StandardMBean}, and accepting it would only postpone the failure to the first
     *        {@code addNotificationListener}
     */
    public <T> StandardEmitterMBean(T implementation, Class<T> mbeanInterface,
                                    NotificationEmitter emitter) {
        super(implementation, mbeanInterface, false);
        this.emitter = require(emitter);
    }

    /**
     * @param isMXBean has to be {@code false}; see the MXBean note in {@link StandardMBean}
     */
    public <T> StandardEmitterMBean(T implementation, Class<T> mbeanInterface, boolean isMXBean,
                                    NotificationEmitter emitter) {
        super(implementation, mbeanInterface, isMXBean);
        this.emitter = require(emitter);
    }

    /** For subclassing: the implementation is {@code this}. */
    protected StandardEmitterMBean(Class<?> mbeanInterface, NotificationEmitter emitter) {
        super(mbeanInterface, false);
        this.emitter = require(emitter);
    }

    protected StandardEmitterMBean(Class<?> mbeanInterface, boolean isMXBean,
                                   NotificationEmitter emitter) {
        super(mbeanInterface, isMXBean);
        this.emitter = require(emitter);
    }

    private static NotificationEmitter require(NotificationEmitter emitter) {
        if (emitter == null) {
            throw new IllegalArgumentException("The emitter cannot be null");
        }
        return emitter;
    }

    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        emitter.removeNotificationListener(listener);
    }

    public void removeNotificationListener(NotificationListener listener,
                                           NotificationFilter filter, Object handback)
            throws ListenerNotFoundException {
        emitter.removeNotificationListener(listener, filter, handback);
    }

    public void addNotificationListener(NotificationListener listener, NotificationFilter filter,
                                        Object handback) {
        emitter.addNotificationListener(listener, filter, handback);
    }

    /** From the emitter, not from introspection; see the class note. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        return emitter.getNotificationInfo();
    }

    /**
     * Emits, if the emitter can emit.
     *
     * <p>{@link NotificationEmitter} does not declare {@code sendNotification} --emitting is a
     * matter for the implementer, not for the contract-- so it has to be asked. An emitter that is
     * not a {@link NotificationBroadcasterSupport} and cannot emit is a construction error, which
     * is why it is said now and not swallowed silently.
     */
    public void sendNotification(Notification n) {
        if (!(emitter instanceof NotificationBroadcasterSupport)) {
            throw new ClassCastException(
                "The emitter is not a NotificationBroadcasterSupport and cannot send: "
                + emitter.getClass().getName());
        }
        ((NotificationBroadcasterSupport) emitter).sendNotification(n);
    }
}
