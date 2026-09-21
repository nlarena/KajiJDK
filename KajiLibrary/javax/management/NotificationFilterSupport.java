package javax.management;

import java.util.List;
import java.util.Vector;

/**
 * Filter by notification type, with <b>prefix</b> semantics.
 *
 * <p>The subtlety is there: {@code enableType("a.b")} does not enable the type {@code a.b} but
 * everything that <b>starts</b> with {@code a.b}. It is what makes the dotted convention useful
 * --{@code jmx.mbean.registered} and {@code jmx.mbean.unregistered} are both enabled with
 * {@code jmx.mbean}-- and also what explains that {@code enableType("")} enables everything.
 *
 * <p>It starts with the list empty, that is, <b>blocking</b> everything. It is the opposite of what
 * "default filter" suggests and it is intentional: a filter just built and never configured lets
 * nothing through.
 */
public class NotificationFilterSupport implements NotificationFilter {

    private static final long serialVersionUID = 6579080007561786969L;

    /**
     * @serial the enabled prefixes
     */
    private List<String> enabledTypes = new Vector<String>();

    /** With the list empty: no notification passes until some prefix is enabled. */
    public NotificationFilterSupport() {
    }

    /**
     * Lets through if the notification's type starts with one of the enabled prefixes.
     */
    public synchronized boolean isNotificationEnabled(Notification notification) {
        String type = notification.getType();
        if (type == null) {
            return false;
        }
        for (int i = 0; i < enabledTypes.size(); i++) {
            if (type.startsWith(enabledTypes.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enables a prefix.
     *
     * @throws IllegalArgumentException if it is {@code null}. It is not accepted because a {@code
     *         null} in the list would make every later evaluation of the filter fail, far from
     *         where the mistake was.
     */
    public synchronized void enableType(String prefix) throws IllegalArgumentException {
        if (prefix == null) {
            throw new IllegalArgumentException("The type prefix cannot be null");
        }
        // Idempotent: repeating the same prefix does not change what the filter lets through, and
        // storing it twice would only make the walk longer.
        if (!enabledTypes.contains(prefix)) {
            enabledTypes.add(prefix);
        }
    }

    /** Removes that exact prefix; if it was not there, does nothing. */
    public synchronized void disableType(String prefix) {
        enabledTypes.remove(prefix);
    }

    /** Back to the initial state: blocks everything. */
    public synchronized void disableAllTypes() {
        enabledTypes.clear();
    }

    /**
     * The enabled prefixes.
     *
     * <p>It returns the <b>internal</b> list, as the JDK does: modifying it modifies the filter.
     * The historical signature is {@code Vector} and cannot be narrowed without breaking whoever
     * assigns it.
     */
    public synchronized Vector<String> getEnabledTypes() {
        return (Vector<String>) enabledTypes;
    }
}
