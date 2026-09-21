package javax.naming.event;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.event.EventContext -- a context you can listen to.
 *
 * <p>It adds listener registration to {@link Context}. The three scopes say <b>which part</b> of
 * the tree is listened to, and the choice has direct cost consequences: {@link #SUBTREE_SCOPE} on a
 * large root is a subscription the server has to maintain over everything.
 *
 * <h2>The name may not exist yet</h2>
 *
 * <p>{@link #targetMustExist} answers whether this implementation allows listening to a name that
 * <b>is not there yet</b>. When it does, you can wait for something to appear; when it does not,
 * registering on a nonexistent name fails with {@code NameNotFoundException}.
 *
 * <p>There is an unavoidable race there, worth keeping in mind: between querying and subscribing,
 * the entry may change. The way not to miss that change is to subscribe <b>first</b> and query
 * afterwards.
 *
 * <h2>Unsubscribing is per listener, not per name</h2>
 *
 * <p>{@link #removeNamingListener} takes the listener and removes <b>all</b> its subscriptions in
 * this context. There is no way to remove just one: a listener registered on three names is
 * unsubscribed from all three or from none. If granularity is needed, use different listeners.
 */
public interface EventContext extends Context {

    /** Only the named entry. */
    public static final int OBJECT_SCOPE = 0;

    /** Its direct children, without itself. */
    public static final int ONELEVEL_SCOPE = 1;

    /** Itself and its whole subtree. */
    public static final int SUBTREE_SCOPE = 2;

    /**
     * Registers a listener.
     *
     * @param scope one of the three above
     * @throws NamingException if the name does not exist and {@link #targetMustExist} is true
     */
    void addNamingListener(Name target, int scope, NamingListener l) throws NamingException;

    /** Same, with the name as text. */
    void addNamingListener(String target, int scope, NamingListener l) throws NamingException;

    /** Removes all of that listener's subscriptions. See the class note. */
    void removeNamingListener(NamingListener l) throws NamingException;

    /** Whether the name has to exist to be listened to. */
    boolean targetMustExist() throws NamingException;
}
