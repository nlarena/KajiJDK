package javax.management;

import java.io.Serializable;

/**
 * Decide, del lado del emisor, que notificaciones valen el viaje.
 *
 * <p>Es `Serializable` por la misma razon que {@link QueryExp}: sobre una conexion remota el filtro
 * viaja al agente y se evalua alla. Filtrar en el cliente seria filtrar despues de pagar el costo
 * que el filtro existe para evitar.
 */
public interface NotificationFilter extends Serializable {

    /** Si esta notificacion se le entrega al oyente. */
    boolean isNotificationEnabled(Notification notification);
}
