package javax.management.timer;

import javax.management.Notification;

/**
 * KajiLibrary's javax.management.timer.TimerNotification -- lo que manda un {@link Timer}.
 *
 * <p>Agrega un solo campo a {@link Notification}: el identificador de la <b>inscripcion</b> que la
 * produjo. No es el numero de secuencia y no hay que confundirlos: el numero de secuencia cambia en
 * cada envio, el identificador es el mismo en todos los disparos de una notificacion periodica. Es
 * el que sirve para atar lo que llega con lo que se pidio, y para darlo de baja.
 */
public class TimerNotification extends Notification {

    private static final long serialVersionUID = 1798492029603825750L;

    /** El identificador de la inscripcion; ver la nota de la clase. */
    private Integer notificationID;

    /**
     * @param type el tipo que eligio quien inscribio la notificacion
     * @param source el {@link Timer} que la manda
     * @param sequenceNumber cambia en cada envio
     * @param id el identificador de la inscripcion, constante entre disparos
     */
    public TimerNotification(String type, Object source, long sequenceNumber, long timeStamp,
                             String msg, Integer id) {
        super(type, source, sequenceNumber, timeStamp, msg);
        this.notificationID = id;
    }

    /** El identificador de la inscripcion. Ver la nota de la clase. */
    public Integer getNotificationID() {
        return this.notificationID;
    }
}
