package javax.management;

import java.util.EventListener;

/**
 * El que recibe notificaciones de un MBean.
 *
 * <p>El segundo argumento de {@link #handleNotification} es la clave del dise&ntilde;o y suele
 * pasar desapercibido: es el objeto que el oyente entrego al registrarse, y vuelve tal cual. Con
 * eso un mismo oyente puede atender veinte fuentes distintas y saber cual es cual sin llevar un
 * mapa propio ni registrar veinte objetos.
 */
public interface NotificationListener extends EventListener {

    /**
     * Llega una notificacion.
     *
     * <p>Se llama, en general, en un hilo del emisor: bloquear aca frena al MBean que notifica.
     *
     * @param handback el objeto entregado al registrarse, o `null`
     */
    void handleNotification(Notification notification, Object handback);
}
