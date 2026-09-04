package javax.management.remote;

import java.io.Serializable;
import javax.management.Notification;

/**
 * KajiLibrary's javax.management.remote.TargetedNotification -- una notificacion con el numero del
 * escucha al que va.
 *
 * <p>Existe por una razon de red. Un cliente remoto registra varios escuchas y el servidor le manda
 * las notificaciones en tandas; sin este numero habria que mandar tambien el filtro y el nombre del
 * MBean para que el cliente supiera a quien entregarle cada una.
 *
 * <p>El numero lo asigna el servidor cuando el cliente registra el escucha, y solo tiene sentido
 * dentro de esa conexion.
 *
 * <p>Los dos campos son {@code final} de hecho pero la clase no promete inmutabilidad: es un objeto de
 * transporte, no un valor.
 */
public class TargetedNotification implements Serializable {

    private static final long serialVersionUID = 7676132089779300926L;

    /** La notificacion. */
    private final Notification notif;

    /** A que escucha va. */
    private final Integer id;

    /**
     * @throws IllegalArgumentException si alguno de los dos es null
     */
    public TargetedNotification(Notification notification, Integer listenerID) {
        if (notification == null) {
            throw new IllegalArgumentException("Invalid notification: null");
        }
        if (listenerID == null) {
            throw new IllegalArgumentException("Invalid listener ID: null");
        }
        this.notif = notification;
        this.id = listenerID;
    }

    /** La notificacion. */
    public Notification getNotification() {
        return this.notif;
    }

    /** El numero del escucha. */
    public Integer getListenerID() {
        return this.id;
    }

    /** Las dos cosas entre llaves. */
    @Override
    public String toString() {
        return "{" + this.notif + ", " + this.id + "}";
    }
}
