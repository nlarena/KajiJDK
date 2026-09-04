package javax.management.modelmbean;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationListener;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBeanNotificationBroadcaster -- avisos, y avisos de
 * cambio de atributo.
 *
 * <p>Extiende {@link NotificationBroadcaster} con dos cosas: la capacidad de <b>mandar</b> avisos
 * --un emisor comun solo los recibe-- y un canal aparte para los cambios de atributo.
 *
 * <p>El canal aparte es lo interesante. Un oyente de cambios de atributo se registra diciendo
 * <b>que atributo</b> le interesa, y solo recibe los de ese. Con el canal comun habria que mandarle
 * todos los avisos del MBean y filtrar del lado del oyente, que sobre un MBean con muchos atributos
 * es trabajo puro.
 *
 * <p>{@code sendNotification(String)} arma el aviso a partir del texto: es el atajo para el caso
 * comun de querer avisar algo sin construir un {@link Notification}.
 */
public interface ModelMBeanNotificationBroadcaster extends NotificationBroadcaster {

    /** Manda ese aviso a los oyentes del canal comun. */
    void sendNotification(Notification ntfyObj) throws MBeanException, RuntimeOperationsException;

    /** Idem, armando el aviso a partir del texto. */
    void sendNotification(String ntfyText) throws MBeanException, RuntimeOperationsException;

    /** Manda ese aviso a los oyentes del canal de cambios de atributo. */
    void sendAttributeChangeNotification(AttributeChangeNotification notification)
        throws MBeanException, RuntimeOperationsException;

    /**
     * Idem, armando el aviso a partir del valor viejo y el nuevo.
     *
     * @param oldValue como estaba; su nombre es el del atributo
     * @param newValue como quedo
     */
    void sendAttributeChangeNotification(Attribute oldValue, Attribute newValue)
        throws MBeanException, RuntimeOperationsException;

    /**
     * Registra un oyente para los cambios de <b>un</b> atributo.
     *
     * @param attributeName cual; null significa todos
     * @throws IllegalArgumentException si el oyente es null
     */
    void addAttributeChangeNotificationListener(NotificationListener listener,
                                                String attributeName, Object handback)
        throws MBeanException, RuntimeOperationsException, IllegalArgumentException;

    /**
     * Lo da de baja.
     *
     * @throws ListenerNotFoundException si no estaba registrado para ese atributo
     */
    void removeAttributeChangeNotificationListener(NotificationListener listener,
                                                   String attributeName)
        throws MBeanException, RuntimeOperationsException, ListenerNotFoundException;
}
