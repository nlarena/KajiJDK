package javax.management.relation;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.ObjectName;

/**
 * A filter that lets through registration and unregistration notifications <b>only for certain
 * MBeans</b>.
 *
 * <h2>Why filtering by name and not only by type is needed</h2>
 *
 * <p>{@link NotificationFilterSupport} filters by the notification's <em>type</em>, and the MBean
 * server emits a single type for all registrations. Subscribing to it in a system with thousands
 * of MBeans means waking up for every one.
 *
 * <p>This filter adds the other dimension: <em>which</em> MBean. It is what lets the relation
 * service learn that exactly one of the MBeans its relations reference was unregistered, without
 * looking at all the others.
 *
 * <h2>The blacklist and the whitelist coexist</h2>
 *
 * <p>And the order between them is what has to be understood: <b>disabled wins</b>.
 * {@link #enableAllObjectNames} followed by {@link #disableObjectName} is "everyone but that
 * one", which is the useful way of expressing an exception without listing the rest.
 */
public class MBeanServerNotificationFilter extends NotificationFilterSupport {

    private static final long serialVersionUID = 2605900539589789736L;

    /** {@code null} means "all"; a list means "only these". */
    private List<ObjectName> selectedNames = new ArrayList<ObjectName>();

    /** {@code null} means "all disabled"; a list, "not these". */
    private List<ObjectName> deselectedNames = null;

    /**
     * A filter that lets nothing through yet.
     *
     * <p>It starts closed on purpose: it enables the server's notification type but with the list
     * of names empty. A filter that started open would deliver everything until someone remembered
     * to close it.
     */
    public MBeanServerNotificationFilter() {
        super();
        enableType(MBeanServerNotification.REGISTRATION_NOTIFICATION);
        enableType(MBeanServerNotification.UNREGISTRATION_NOTIFICATION);
    }

    /** No MBean passes. */
    public synchronized void disableAllObjectNames() {
        this.selectedNames = new ArrayList<ObjectName>();
        this.deselectedNames = null;
    }

    /**
     * That MBean does not pass, even if it is enabled.
     *
     * @throws IllegalArgumentException if it is {@code null}
     */
    public synchronized void disableObjectName(ObjectName objectName)
            throws IllegalArgumentException {
        if (objectName == null) {
            throw new IllegalArgumentException("the name cannot be null");
        }
        if (this.selectedNames != null) {
            this.selectedNames.remove(objectName);
        }
        if (this.deselectedNames != null && !this.deselectedNames.contains(objectName)) {
            this.deselectedNames.add(objectName);
        }
    }

    /** All MBeans pass. */
    public synchronized void enableAllObjectNames() {
        this.selectedNames = null;
        this.deselectedNames = new ArrayList<ObjectName>();
    }

    /**
     * That MBean passes.
     *
     * @throws IllegalArgumentException if it is {@code null}
     */
    public synchronized void enableObjectName(ObjectName objectName)
            throws IllegalArgumentException {
        if (objectName == null) {
            throw new IllegalArgumentException("the name cannot be null");
        }
        if (this.deselectedNames != null) {
            this.deselectedNames.remove(objectName);
        }
        if (this.selectedNames != null && !this.selectedNames.contains(objectName)) {
            this.selectedNames.add(objectName);
        }
    }

    /** The enabled ones, or {@code null} if they all are. */
    public synchronized Vector<ObjectName> getEnabledObjectNames() {
        return this.selectedNames == null ? null : new Vector<ObjectName>(this.selectedNames);
    }

    /** The disabled ones, or {@code null} if they all are. */
    public synchronized Vector<ObjectName> getDisabledObjectNames() {
        return this.deselectedNames == null ? null : new Vector<ObjectName>(this.deselectedNames);
    }

    /**
     * Whether the notification passes: first by type, then by name.
     *
     * <p>The order matters for cost: the type check is a string comparison and discards almost
     * everything before touching the lists.
     */
    public synchronized boolean isNotificationEnabled(Notification notif)
            throws IllegalArgumentException {
        if (notif == null) {
            throw new IllegalArgumentException("the notification cannot be null");
        }
        if (!super.isNotificationEnabled(notif)) {
            return false;
        }
        if (!(notif instanceof MBeanServerNotification)) {
            return false;
        }
        ObjectName name = ((MBeanServerNotification) notif).getMBeanName();
        // Disabled wins; see the class note.
        if (this.deselectedNames == null) {
            return false;
        }
        if (this.deselectedNames.contains(name)) {
            return false;
        }
        return this.selectedNames == null || this.selectedNames.contains(name);
    }
}
