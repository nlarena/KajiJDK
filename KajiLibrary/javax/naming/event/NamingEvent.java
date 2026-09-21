package javax.naming.event;

import java.util.EventObject;
import javax.naming.Binding;

/**
 * KajiLibrary's javax.naming.event.NamingEvent -- something changed in the context.
 *
 * <p>A type --one of the four below-- and up to two bindings: how the entry was and how it ended
 * up. Which of the two come depends on the type, and it is the first thing to know to use it:
 *
 * <ul>
 *   <li>{@link #OBJECT_ADDED}: only the new one;
 *   <li>{@link #OBJECT_REMOVED}: only the old one;
 *   <li>{@link #OBJECT_CHANGED}: both;
 *   <li>{@link #OBJECT_RENAMED}: both, unless the rename crosses the edge of the subscribed scope,
 *       and then the one outside is missing.
 * </ul>
 *
 * <p>(An earlier note let {@code OBJECT_CHANGED} lose a binding at the scope edge too; the JDK
 * requires both bindings to be non-null for it.)
 *
 * <p>The fields are {@code protected} and not private, as in the JDK: the class dates from JNDI 1.2
 * and provider subclasses touch them directly.
 *
 * <p>{@link #getChangeInfo} returns whatever the provider wants to add --an LDAP change number, for
 * example-- and is specific to each one. Depending on it ties the program to a provider.
 */
public class NamingEvent extends EventObject {

    private static final long serialVersionUID = 2716268041038319063L;

    /** An entry appeared. */
    public static final int OBJECT_ADDED = 0;

    /** One disappeared. */
    public static final int OBJECT_REMOVED = 1;

    /** One was renamed. */
    public static final int OBJECT_RENAMED = 2;

    /** The content of one changed. */
    public static final int OBJECT_CHANGED = 3;

    /** Whatever the provider wants to add; see the class note. */
    protected Object changeInfo;

    /** Which of the four. */
    protected int type;

    /** How it was, or null. */
    protected Binding oldBinding;

    /** How it ended up, or null. */
    protected Binding newBinding;

    /**
     * @param source the context where it happened
     * @param type one of the four above
     * @param newBd how it ended up; null if it disappeared
     * @param oldBd how it was; null if it is new
     * @param changeInfo whatever the provider wants to add, or null
     */
    public NamingEvent(EventContext source, int type, Binding newBd, Binding oldBd,
                       Object changeInfo) {
        super(source);
        this.type = type;
        this.changeInfo = changeInfo;
        this.oldBinding = oldBd;
        this.newBinding = newBd;
    }

    /** Which of the four. */
    public int getType() {
        return this.type;
    }

    /** The context where it happened. */
    public EventContext getEventContext() {
        return (EventContext) getSource();
    }

    /** How it was, or null. See the class note. */
    public Binding getOldBinding() {
        return this.oldBinding;
    }

    /** How it ended up, or null. */
    public Binding getNewBinding() {
        return this.newBinding;
    }

    /** What the provider added, or null. */
    public Object getChangeInfo() {
        return this.changeInfo;
    }

    /**
     * Dispatched to the method that matches its type.
     *
     * <p>A listener that does not implement the interface for the event's type gets nothing: the
     * {@code instanceof} filters it out instead of throwing, so a dispatcher can hold a mixed list
     * of listeners without knowing which listens to what. The JDK does not do this: it casts, and a
     * listener of the wrong kind gets a {@code ClassCastException}.
     */
    public void dispatch(NamingListener listener) {
        switch (this.type) {
            case OBJECT_ADDED:
                if (listener instanceof NamespaceChangeListener) {
                    ((NamespaceChangeListener) listener).objectAdded(this);
                }
                break;
            case OBJECT_REMOVED:
                if (listener instanceof NamespaceChangeListener) {
                    ((NamespaceChangeListener) listener).objectRemoved(this);
                }
                break;
            case OBJECT_RENAMED:
                if (listener instanceof NamespaceChangeListener) {
                    ((NamespaceChangeListener) listener).objectRenamed(this);
                }
                break;
            case OBJECT_CHANGED:
                if (listener instanceof ObjectChangeListener) {
                    ((ObjectChangeListener) listener).objectChanged(this);
                }
                break;
            default:
                break;
        }
    }
}
