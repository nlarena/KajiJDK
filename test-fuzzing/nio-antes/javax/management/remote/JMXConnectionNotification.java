package javax.management.remote;

import javax.management.Notification;

/**
 * KajiLibrary's javax.management.remote.JMXConnectionNotification -- a JMX connection's state
 * change.
 *
 * <p>Both the client-side {@link JMXConnector} and the server-side {@link JMXConnectorServer}
 * emit it, with the same four types.
 *
 * <p>{@link #NOTIFS_LOST} is the one that matters and the one that gets ignored. It is not a
 * connection error: the connection is still alive and what was lost are <b>notifications</b>,
 * because the server's buffer filled up before the client collected them. Its {@code userData} is
 * a {@link Long} with how many were lost. See {@link NotificationResult} on how it is detected.
 *
 * <p>{@link #FAILED} is final: the connection was cut without anybody closing it.
 */
public class JMXConnectionNotification extends Notification {

    private static final long serialVersionUID = -2331308725952627538L;

    /** A connection was opened. */
    public static final String OPENED = "jmx.remote.connection.opened";

    /** It was closed in an orderly way. */
    public static final String CLOSED = "jmx.remote.connection.closed";

    /** It was cut by itself. */
    public static final String FAILED = "jmx.remote.connection.failed";

    /** Notifications were lost. See the class note. */
    public static final String NOTIFS_LOST = "jmx.remote.connection.notifs.lost";

    /** Which connection. */
    private final String connectionId;

    /**
     * @param type one of the four types
     * @param source who emits it: the connector or the server
     * @param connectionId the connection's identifier
     * @param sequenceNumber the emitter's sequence number
     * @param message text to show, or null
     * @param userData the extra datum; for {@link #NOTIFS_LOST}, how many were lost
     * @throws NullPointerException if the type, the source or the identifier are null
     */
    public JMXConnectionNotification(String type, Object source, String connectionId,
                                     long sequenceNumber, String message, Object userData) {
        super(type, source, sequenceNumber, System.currentTimeMillis(), message);
        if (type == null || source == null || connectionId == null) {
            throw new NullPointerException("Illegal null argument");
        }
        this.connectionId = connectionId;
        setUserData(userData);
    }

    /** The connection's identifier. */
    public String getConnectionId() {
        return this.connectionId;
    }
}
