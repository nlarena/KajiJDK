package java.awt.event;

import java.awt.Component;

/**
 * A component gained or lost the keyboard focus.
 *
 * <p>**Temporary** is the important distinction and the one that gets overlooked. A focus lost
 * temporarily —because a menu opened, because the window went to the background— is going to come
 * back, and it is not the moment to validate the field or to save. A permanent one is.
 *
 * <p>The cause, added much later, says **why**: whether it was a click, a tab, an explicit request
 * or the window starting up. It serves to treat a focus the user asked for differently from one that
 * merely fell to them.
 */
public class FocusEvent extends ComponentEvent {

    private static final long serialVersionUID = 523753786457416396L;

    /** Why the focus changed. */
    public static enum Cause {

        /** It is not known. */
        UNKNOWN,

        /** A mouse click. */
        MOUSE_EVENT,

        /** A traversal with the tab key. */
        TRAVERSAL,

        /** A traversal up the tree. */
        TRAVERSAL_UP,

        /** A traversal down the tree. */
        TRAVERSAL_DOWN,

        /** A traversal forwards. */
        TRAVERSAL_FORWARD,

        /** A traversal backwards. */
        TRAVERSAL_BACKWARD,

        /** Someone asked for it explicitly. */
        MANUAL_REQUEST,

        /** The system moved it on its own. */
        AUTOMATIC_TRAVERSAL,

        /** The previous focus was returned to because the new one did not accept it. */
        ROLLBACK,

        /** The window became the active one. */
        ACTIVATION,

        /** The global focus was released. */
        CLEAR_GLOBAL_FOCUS_OWNER,

        /** Something happened that fits none of the others. */
        UNEXPECTED
    }

    /** The family's first identifier. */
    public static final int FOCUS_FIRST = 1004;

    /** The component gained the focus. */
    public static final int FOCUS_GAINED = 1004;

    /** The family's last identifier. */
    public static final int FOCUS_LAST = 1005;

    /** The component lost the focus. */
    public static final int FOCUS_LOST = 1005;

    private final boolean temporary;
    private final Component opposite;
    private final Cause cause;

    /**
     * With everything given.
     *
     * @throws IllegalArgumentException if the source is `null`
     * @throws NullPointerException if the cause is `null`
     */
    public FocusEvent(Component source, int id, boolean temporary, Component opposite,
            Cause cause) {
        super(source, id);
        if (cause == null) {
            throw new NullPointerException("null cause");
        }
        this.temporary = temporary;
        this.opposite = opposite;
        this.cause = cause;
    }

    /**
     * Without saying the cause.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public FocusEvent(Component source, int id, boolean temporary, Component opposite) {
        this(source, id, temporary, opposite, Cause.UNKNOWN);
    }

    /**
     * Without the opposite component.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public FocusEvent(Component source, int id, boolean temporary) {
        this(source, id, temporary, null, Cause.UNKNOWN);
    }

    /**
     * Permanent and with no opposite.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public FocusEvent(Component source, int id) {
        this(source, id, false, null, Cause.UNKNOWN);
    }

    /** Whether the focus is going to come back. */
    public boolean isTemporary() {
        return this.temporary;
    }

    /** The other side of the change, or `null` if it belongs to another application. */
    public Component getOppositeComponent() {
        return this.opposite;
    }

    /** Why the focus changed. */
    public final Cause getCause() {
        return this.cause;
    }

    public String paramString() {
        String type;
        if (this.id == FOCUS_GAINED) {
            type = "FOCUS_GAINED";
        } else if (this.id == FOCUS_LOST) {
            type = "FOCUS_LOST";
        } else {
            type = "unknown type";
        }
        return type + (this.temporary ? ",temporary" : ",permanent") + ",opposite="
                + this.opposite + ",cause=" + this.cause;
    }
}
