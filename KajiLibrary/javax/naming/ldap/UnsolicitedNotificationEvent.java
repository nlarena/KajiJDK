package javax.naming.ldap;

import java.util.EventObject;

/**
 * El evento que envuelve una {@link UnsolicitedNotification}.
 *
 * <p>{@link #dispatch} esta del lado del evento y no del oyente, y eso es del patron de eventos de
 * {@code javax.naming}: el evento sabe a que metodo del oyente corresponde, asi que quien reparte no
 * necesita un {@code if} por tipo de evento.
 */
public class UnsolicitedNotificationEvent extends EventObject {

    private static final long serialVersionUID = -2382603380799883705L;

    private final UnsolicitedNotification notice;

    /**
     * @param src quien la emitio
     * @param notice la notificacion
     */
    public UnsolicitedNotificationEvent(Object src, UnsolicitedNotification notice) {
        super(src);
        this.notice = notice;
    }

    /** La notificacion. */
    public UnsolicitedNotification getNotification() {
        return this.notice;
    }

    /** Se la entrega al oyente. */
    public void dispatch(UnsolicitedNotificationListener listener) {
        listener.notificationReceived(this);
    }
}
