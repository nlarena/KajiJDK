package com.sun.nio.sctp;

/**
 * Un {@link NotificationHandler} que reparte cada notificacion al metodo de su tipo.
 *
 * <h2>Que problema resuelve, y por que es una clase y no una interfaz</h2>
 *
 * <p>Sin esto, todo manejador empieza con la misma cadena de {@code instanceof}: preguntar si la
 * notificacion es un cambio de asociacion, si es un cambio de direccion, si es un envio fallido.
 * Esta clase la escribe una vez — la <strong>sobrecarga</strong> hace el reparto — y cada subclase
 * sobrescribe solo los tipos que le interesan.
 *
 * <p>Es una clase con cuerpos y no una interfaz con {@code default} porque los cinco metodos tienen
 * que existir con una implementacion que no haga nada: quien atiende un solo tipo de notificacion no
 * deberia escribir cuatro metodos vacios.
 *
 * <p>Todos devuelven {@link HandlerResult#CONTINUE} por omision, que es la respuesta segura: seguir
 * esperando el mensaje que el programa pidio. Un manejador que quiera cortar tiene que decirlo.
 *
 * @param <T> el objeto de contexto que viaja desde el {@code receive}
 */
public class AbstractNotificationHandler<T> implements NotificationHandler<T> {

    /** Para las subclases. */
    protected AbstractNotificationHandler() {
    }

    /**
     * Lo que atiende una notificacion que no es de ninguno de los cuatro tipos conocidos.
     *
     * <p>Existe por la misma razon que un {@code default} en un {@code switch}: el protocolo puede
     * crecer, y una notificacion nueva tiene que caer en algun lado.
     */
    public HandlerResult handleNotification(Notification notification, T attachment) {
        return HandlerResult.CONTINUE;
    }

    /** La asociacion cambio de estado. */
    public HandlerResult handleNotification(AssociationChangeNotification notification, T attachment) {
        return HandlerResult.CONTINUE;
    }

    /** Una direccion del par cambio de estado. */
    public HandlerResult handleNotification(PeerAddressChangeNotification notification, T attachment) {
        return HandlerResult.CONTINUE;
    }

    /** Un mensaje no se pudo entregar y volvio. */
    public HandlerResult handleNotification(SendFailedNotification notification, T attachment) {
        return HandlerResult.CONTINUE;
    }

    /** El par empezo a cerrar la asociacion. */
    public HandlerResult handleNotification(ShutdownNotification notification, T attachment) {
        return HandlerResult.CONTINUE;
    }
}
