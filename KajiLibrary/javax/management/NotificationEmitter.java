package javax.management;

/**
 * El {@link NotificationBroadcaster} que si sabe sacar un registro puntual.
 *
 * <p>La diferencia importa cuando un mismo oyente se registro varias veces con filtros distintos:
 * con el metodo heredado se van todos, con este se va exactamente uno.
 */
public interface NotificationEmitter extends NotificationBroadcaster {

    /**
     * Saca el registro que coincide en los tres: oyente, filtro y handback.
     *
     * <p>La comparacion del filtro y del handback es por identidad de referencia en la practica del
     * JDK, no por `equals`.
     *
     * @throws ListenerNotFoundException si no hay ninguno asi
     */
    void removeNotificationListener(NotificationListener listener, NotificationFilter filter,
                                    Object handback) throws ListenerNotFoundException;
}
