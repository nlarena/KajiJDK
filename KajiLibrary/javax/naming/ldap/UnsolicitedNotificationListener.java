package javax.naming.ldap;

import javax.naming.event.NamingListener;

/**
 * Whoever wants to hear about {@link UnsolicitedNotification}s.
 *
 * <p>It extends {@link NamingListener} to enter the same listener registry as the rest of
 * {@code javax.naming.event}, and from there it inherits the error handling: a failure of the
 * subscription arrives through {@code namingExceptionThrown}, not through this method.
 */
public interface UnsolicitedNotificationListener extends NamingListener {

    /** A notification arrived. */
    void notificationReceived(UnsolicitedNotificationEvent evt);
}
