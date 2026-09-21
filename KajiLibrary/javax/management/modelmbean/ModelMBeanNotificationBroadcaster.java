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
 * KajiLibrary's javax.management.modelmbean.ModelMBeanNotificationBroadcaster -- notices, and
 * attribute change notices.
 *
 * <p>It extends {@link NotificationBroadcaster} with two things: the ability to <b>send</b>
 * notices --a plain broadcaster only receives them-- and a separate channel for attribute
 * changes.
 *
 * <p>The separate channel is the interesting part. A listener of attribute changes registers
 * saying <b>which attribute</b> it cares about, and receives only that one's. With the common
 * channel it would have to be sent every notice of the MBean and filter on the listener's side,
 * which over an MBean with many attributes is pure work.
 *
 * <p>{@code sendNotification(String)} builds the notice from the text: it is the shortcut for the
 * common case of wanting to report something without constructing a {@link Notification}.
 */
public interface ModelMBeanNotificationBroadcaster extends NotificationBroadcaster {

    /** Sends that notice to the common channel's listeners. */
    void sendNotification(Notification ntfyObj) throws MBeanException, RuntimeOperationsException;

    /** The same, building the notice from the text. */
    void sendNotification(String ntfyText) throws MBeanException, RuntimeOperationsException;

    /** Sends that notice to the attribute change channel's listeners. */
    void sendAttributeChangeNotification(AttributeChangeNotification notification)
        throws MBeanException, RuntimeOperationsException;

    /**
     * The same, building the notice from the old value and the new one.
     *
     * @param oldValue how it was; its name is the attribute's
     * @param newValue how it ended up
     */
    void sendAttributeChangeNotification(Attribute oldValue, Attribute newValue)
        throws MBeanException, RuntimeOperationsException;

    /**
     * Registers a listener for the changes of <b>one</b> attribute.
     *
     * @param attributeName which one; null means all
     * @throws IllegalArgumentException if the listener is null
     */
    void addAttributeChangeNotificationListener(NotificationListener listener,
                                                String attributeName, Object handback)
        throws MBeanException, RuntimeOperationsException, IllegalArgumentException;

    /**
     * Removes it.
     *
     * @throws ListenerNotFoundException if it was not registered for that attribute
     */
    void removeAttributeChangeNotificationListener(NotificationListener listener,
                                                   String attributeName)
        throws MBeanException, RuntimeOperationsException, ListenerNotFoundException;
}
