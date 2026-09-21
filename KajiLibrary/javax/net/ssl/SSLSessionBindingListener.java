package javax.net.ssl;

import java.util.EventListener;

/**
 * Finds out when an own object goes into or out of an {@link SSLSession}.
 *
 * <p>A session can keep application objects with {@link SSLSession#putValue}. If the kept object
 * implements this interface, the session notifies it — and that gives it the chance to release
 * whatever it holds when it is removed or when the session is invalidated. Without this notice, an
 * object kept in a session that dies would have no way of knowing.
 */
public interface SSLSessionBindingListener extends EventListener {

    /** It was just kept in a session. */
    void valueBound(SSLSessionBindingEvent event);

    /** It was just removed, or the session was invalidated. */
    void valueUnbound(SSLSessionBindingEvent event);
}
