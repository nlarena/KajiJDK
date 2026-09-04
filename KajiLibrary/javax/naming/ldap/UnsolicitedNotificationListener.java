package javax.naming.ldap;

import javax.naming.event.NamingListener;

/**
 * Quien quiere enterarse de las {@link UnsolicitedNotification}.
 *
 * <p>Extiende {@link NamingListener} para entrar en el mismo registro de oyentes que el resto de
 * {@code javax.naming.event}, y de ahi hereda el manejo de errores: un fallo del oyente llega por
 * {@code namingExceptionThrown}, no por este metodo.
 */
public interface UnsolicitedNotificationListener extends NamingListener {

    /** Llego una notificacion. */
    void notificationReceived(UnsolicitedNotificationEvent evt);
}
