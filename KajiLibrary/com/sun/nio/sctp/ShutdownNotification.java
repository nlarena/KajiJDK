package com.sun.nio.sctp;

/**
 * El par empezo a cerrar la asociacion ordenadamente.
 *
 * <p>Ordenadamente quiere decir que lo que ya estaba en vuelo se entrega igual: SCTP separa el
 * cierre de la perdida, y esta notificacion es la del cierre. La perdida llega como un
 * {@link AssociationChangeNotification} con {@code COMM_LOST}.
 */
public abstract class ShutdownNotification implements Notification {

    /** Para las implementaciones de SCTP. */
    protected ShutdownNotification() {
    }

    /** La asociacion que se esta cerrando. */
    public abstract Association association();
}
