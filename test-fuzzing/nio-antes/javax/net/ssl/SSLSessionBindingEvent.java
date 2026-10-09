package javax.net.ssl;

import java.util.EventObject;

/**
 * An application object went into or out of an {@link SSLSession}.
 *
 * <p>The event's source is the session, so {@link #getSession} and {@code getSource} return the
 * same with a different type. Both are there because {@link EventObject} forces the second and
 * nobody wants to cast.
 */
public class SSLSessionBindingEvent extends EventObject {

    private static final long serialVersionUID = 3989172637106345L;

    private final String name;

    /**
     * @throws IllegalArgumentException if the session is {@code null}
     */
    public SSLSessionBindingEvent(SSLSession session, String name) {
        super(session);
        this.name = name;
    }

    /** The name the value was kept under. */
    public String getName() {
        return this.name;
    }

    /** The session where it happened. */
    public SSLSession getSession() {
        return (SSLSession) getSource();
    }
}
