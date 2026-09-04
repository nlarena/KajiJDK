package javax.management.remote;

import java.io.Serializable;

/**
 * KajiLibrary's javax.management.remote.NotificationResult -- una tanda de notificaciones y donde
 * seguir.
 *
 * <p>Es la respuesta a un pedido de notificaciones pendientes. Trae las notificaciones y <b>dos</b>
 * numeros de secuencia, y la diferencia entre ellos es lo unico que hay que entender de esta clase:
 *
 * <ul>
 *   <li>{@link #getNextSequenceNumber} es por donde pedir la proxima vez;
 *   <li>{@link #getEarliestSequenceNumber} es la mas vieja que el servidor todavia guarda.
 * </ul>
 *
 * <p>Si el cliente pidio desde un numero <b>menor</b> que ese, perdio notificaciones: el servidor las
 * descarto porque su buffer se lleno mientras el cliente no preguntaba. Es como se detecta la perdida,
 * y es lo que dispara un {@link JMXConnectionNotification#NOTIFS_LOST}.
 */
public class NotificationResult implements Serializable {

    private static final long serialVersionUID = 1191800228721395279L;

    /** La mas vieja que todavia hay. */
    private final long earliestSequenceNumber;

    /** Por donde seguir. */
    private final long nextSequenceNumber;

    /** Las de esta tanda. */
    private final TargetedNotification[] targetedNotifications;

    /**
     * @throws IllegalArgumentException si el arreglo es null
     */
    public NotificationResult(long earliestSequenceNumber, long nextSequenceNumber,
                              TargetedNotification[] targetedNotifications) {
        if (targetedNotifications == null) {
            throw new IllegalArgumentException("Notifications null");
        }
        this.earliestSequenceNumber = earliestSequenceNumber;
        this.nextSequenceNumber = nextSequenceNumber;
        this.targetedNotifications = targetedNotifications;
    }

    /** La mas vieja que el servidor todavia guarda. Ver la nota de la clase. */
    public long getEarliestSequenceNumber() {
        return this.earliestSequenceNumber;
    }

    /** Por donde pedir la proxima vez. */
    public long getNextSequenceNumber() {
        return this.nextSequenceNumber;
    }

    /** Las de esta tanda. */
    public TargetedNotification[] getTargetedNotifications() {
        return this.targetedNotifications;
    }

    /** Los dos numeros y cuantas notificaciones vinieron. */
    @Override
    public String toString() {
        return "NotificationResult: earliest=" + getEarliestSequenceNumber()
            + "; next=" + getNextSequenceNumber()
            + "; nnotifs=" + this.targetedNotifications.length;
    }
}
