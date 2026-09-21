package javax.naming.event;

/**
 * KajiLibrary's javax.naming.event.NamespaceChangeListener -- changes in the <b>namespace</b>.
 *
 * <p>The three events are about the existence and location of entries: one appeared, one
 * disappeared, one was renamed. What it does <b>not</b> cover is an entry's content changing --
 * that is {@link ObjectChangeListener}.
 *
 * <p>The split matters when listening: implementing only this interface on a very busy directory
 * avoids getting an event for every attribute modification, which are usually the majority.
 *
 * <p>In {@link #objectRenamed} the event carries both bindings --the old and the new-- and one of
 * them may be null: renaming into or out of the subscribed scope looks like a partial appearance
 * or disappearance.
 */
public interface NamespaceChangeListener extends NamingListener {

    /** A new entry appeared. */
    void objectAdded(NamingEvent evt);

    /** One disappeared. */
    void objectRemoved(NamingEvent evt);

    /** One was renamed. See the class note about the nulls. */
    void objectRenamed(NamingEvent evt);
}
