package javax.management;

import java.util.List;
import java.util.Vector;

/**
 * Filter of {@link AttributeChangeNotification}s by <b>attribute name</b>.
 *
 * <p>The difference from {@link NotificationFilterSupport} is twofold: here the comparison is by
 * <b>exact equality</b> and not by prefix --attribute names have no hierarchy--, and the filter
 * also requires the notification to really be an {@code AttributeChangeNotification}. Any other one
 * is left out even if its type matches.
 *
 * <p>Like the other one, it starts empty and therefore blocking everything.
 */
public class AttributeChangeNotificationFilter implements NotificationFilter {

    private static final long serialVersionUID = -6347317584796410029L;

    /**
     * @serial the enabled attribute names
     */
    private List<String> enabledAttributes = new Vector<String>();

    /** With the list empty: nothing passes until some attribute is enabled. */
    public AttributeChangeNotificationFilter() {
    }

    /**
     * Lets through only the attribute changes whose name is enabled.
     */
    public synchronized boolean isNotificationEnabled(Notification notification) {
        String type = notification.getType();
        if (type == null
                || !type.equals(AttributeChangeNotification.ATTRIBUTE_CHANGE)
                || !(notification instanceof AttributeChangeNotification)) {
            return false;
        }
        String name = ((AttributeChangeNotification) notification).getAttributeName();
        if (name == null) {
            return false;
        }
        return enabledAttributes.contains(name);
    }

    /**
     * Enables an attribute name.
     *
     * @throws IllegalArgumentException if it is {@code null}, for the same reason as in
     *         {@link NotificationFilterSupport#enableType}: fail here and not on every delivery.
     */
    public synchronized void enableAttribute(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("The attribute name cannot be null");
        }
        if (!enabledAttributes.contains(name)) {
            enabledAttributes.add(name);
        }
    }

    /** Removes that name; if it was not there, does nothing. */
    public synchronized void disableAttribute(String name) {
        enabledAttributes.remove(name);
    }

    /** Back to the initial state: blocks everything. */
    public synchronized void disableAllAttributes() {
        enabledAttributes.clear();
    }

    /**
     * The enabled names; it is the internal list, as in the JDK.
     */
    public synchronized Vector<String> getEnabledAttributes() {
        return (Vector<String>) enabledAttributes;
    }
}
