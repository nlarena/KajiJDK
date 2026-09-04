package javax.management;

/**
 * Un MBean que emite notificaciones.
 *
 * <p>Su carencia esta en {@link #removeNotificationListener}: saca <b>todos</b> los registros de
 * ese oyente, sin poder distinguir filtro ni handback. {@link NotificationEmitter} existe justamente
 * para arreglar eso, y es lo que conviene implementar hoy.
 */
public interface NotificationBroadcaster {

    /**
     * Registra un oyente.
     *
     * @param filter si es `null`, pasan todas
     * @param handback objeto opaco que vuelve en cada entrega
     */
    void addNotificationListener(NotificationListener listener, NotificationFilter filter,
                                 Object handback) throws IllegalArgumentException;

    /**
     * Saca todos los registros del oyente.
     *
     * @throws ListenerNotFoundException si no estaba registrado
     */
    void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException;

    /** Que notificaciones puede llegar a emitir este MBean. */
    MBeanNotificationInfo[] getNotificationInfo();
}
