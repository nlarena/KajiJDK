package java.awt.event;

import java.awt.Window;

/**
 * Something happened to a window: it opened, it closed, it was minimized, it gained or lost the
 * focus.
 *
 * <p>The distinction most often got wrong is between {@code WINDOW_CLOSING} and
 * {@code WINDOW_CLOSED}. The first is the **request**: it arrives when the user hits the cross, the
 * window is still there, and it is where they are asked whether they want to save or it is decided
 * not to close. The second arrives once it has closed and there is nothing left to decide.
 *
 * <p>The "opposite window" is the other side of the focus change: on losing it, which one took it;
 * on gaining it, which one it was taken from. It is `null` when the other window belongs to another
 * application.
 */
public class WindowEvent extends ComponentEvent {

    private static final long serialVersionUID = -1567959133147912127L;

    /** It became the active window. */
    public static final int WINDOW_ACTIVATED = 205;

    /** The window has already closed. */
    public static final int WINDOW_CLOSED = 202;

    /** The user asked to close it; it is still open. */
    public static final int WINDOW_CLOSING = 201;

    /** It stopped being the active window. */
    public static final int WINDOW_DEACTIVATED = 206;

    /** It was restored. */
    public static final int WINDOW_DEICONIFIED = 204;

    /** The family's first identifier. */
    public static final int WINDOW_FIRST = 200;

    /** It gained the keyboard focus. */
    public static final int WINDOW_GAINED_FOCUS = 207;

    /** It was minimized. */
    public static final int WINDOW_ICONIFIED = 203;

    /** The family's last identifier. */
    public static final int WINDOW_LAST = 209;

    /** It lost the keyboard focus. */
    public static final int WINDOW_LOST_FOCUS = 208;

    /** The window was opened for the first time. */
    public static final int WINDOW_OPENED = 200;

    /** It changed between normal, minimized and maximized. */
    public static final int WINDOW_STATE_CHANGED = 209;

    private final Window opposite;
    private final int oldState;
    private final int newState;

    /**
     * With everything given.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public WindowEvent(Window source, int id, Window opposite, int oldState, int newState) {
        super(source, id);
        this.opposite = opposite;
        this.oldState = oldState;
        this.newState = newState;
    }

    /**
     * With the opposite window, for the focus changes.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public WindowEvent(Window source, int id, Window opposite) {
        this(source, id, opposite, 0, 0);
    }

    /**
     * With both states, for the state changes.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public WindowEvent(Window source, int id, int oldState, int newState) {
        this(source, id, null, oldState, newState);
    }

    /**
     * With the window and the identifier alone.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public WindowEvent(Window source, int id) {
        this(source, id, null, 0, 0);
    }

    /** The window it happened to. */
    public Window getWindow() {
        if (this.source instanceof Window) {
            return (Window) this.source;
        }
        return null;
    }

    /** The other side of the focus change, or `null` if it belongs to another application. */
    public Window getOppositeWindow() {
        return this.opposite;
    }

    /** How it was before. */
    public int getOldState() {
        return this.oldState;
    }

    /** How it ended up. */
    public int getNewState() {
        return this.newState;
    }

    public String paramString() {
        String type;
        if (this.id == WINDOW_OPENED) {
            type = "WINDOW_OPENED";
        } else if (this.id == WINDOW_CLOSING) {
            type = "WINDOW_CLOSING";
        } else if (this.id == WINDOW_CLOSED) {
            type = "WINDOW_CLOSED";
        } else if (this.id == WINDOW_ICONIFIED) {
            type = "WINDOW_ICONIFIED";
        } else if (this.id == WINDOW_DEICONIFIED) {
            type = "WINDOW_DEICONIFIED";
        } else if (this.id == WINDOW_ACTIVATED) {
            type = "WINDOW_ACTIVATED";
        } else if (this.id == WINDOW_DEACTIVATED) {
            type = "WINDOW_DEACTIVATED";
        } else if (this.id == WINDOW_GAINED_FOCUS) {
            type = "WINDOW_GAINED_FOCUS";
        } else if (this.id == WINDOW_LOST_FOCUS) {
            type = "WINDOW_LOST_FOCUS";
        } else if (this.id == WINDOW_STATE_CHANGED) {
            type = "WINDOW_STATE_CHANGED";
        } else {
            type = "unknown type";
        }
        return type + ",opposite=" + this.opposite + ",oldState=" + this.oldState + ",newState="
                + this.newState;
    }
}
