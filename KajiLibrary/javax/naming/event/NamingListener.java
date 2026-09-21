package javax.naming.event;

import java.util.EventListener;

/**
 * KajiLibrary's javax.naming.event.NamingListener -- the base of a context's listeners.
 *
 * <p>It declares no change method: only {@link #namingExceptionThrown}. The changes are declared by
 * its two subinterfaces, and this split is not cosmetic -- a listener registers by saying
 * <b>what</b> it implements, and the provider only asks the server for the notifications someone
 * listens to. Listening to more than needed costs traffic against the directory.
 *
 * <p>{@link #namingExceptionThrown} is what always has to be implemented and what almost nobody
 * looks at. When it arrives, the subscription <b>has already been cancelled</b>: the listener will
 * receive nothing more. Ignoring it is how a program ends up watching a directory that stopped
 * notifying it hours ago.
 */
public interface NamingListener extends EventListener {

    /**
     * The subscription failed and was cancelled.
     *
     * <p>See the class note: there is no automatic retry. Listening again is up to whoever receives
     * this.
     */
    void namingExceptionThrown(NamingExceptionEvent evt);
}
