package com.sun.nio.sctp;

/**
 * Quien atiende las {@link Notification} que llegan durante un {@code receive}.
 *
 * <p>Devolver {@link HandlerResult} es lo que le deja decidir si el {@code receive} sigue esperando
 * un mensaje o vuelve. Ver {@link AbstractNotificationHandler} para la forma comoda de escribir uno.
 *
 * @param <T> el objeto de contexto que se le pasa al {@code receive} y llega hasta aca sin que el
 *     canal lo mire
 */
public interface NotificationHandler<T> {

    /** Atiende una notificacion y dice si el {@code receive} sigue. */
    HandlerResult handleNotification(Notification notification, T attachment);
}
