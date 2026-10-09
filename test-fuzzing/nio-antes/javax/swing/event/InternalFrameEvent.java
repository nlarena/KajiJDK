package javax.swing.event;

import java.awt.AWTEvent;

import javax.swing.JInternalFrame;

/**
 * Something happened with an internal frame.
 *
 * <p>The identifiers replicate {@link java.awt.event.WindowEvent}'s in a range of their own, and
 * that symmetry is on purpose: an internal frame has the same life cycle as a real one --it
 * opens, is activated, is iconified, closes-- even though it lives inside another.
 *
 * <p>The distinction that matters is {@link #INTERNAL_FRAME_CLOSING} against
 * {@link #INTERNAL_FRAME_CLOSED}: the first can be cancelled, the second has already happened.
 */
public class InternalFrameEvent extends AWTEvent {

    private static final long serialVersionUID = 1L;

    /** The first of this range's identifiers. */
    public static final int INTERNAL_FRAME_FIRST = 25549;

    /** The last of this range's identifiers. */
    public static final int INTERNAL_FRAME_LAST = 25555;

    /** It opened. */
    public static final int INTERNAL_FRAME_OPENED = 25549;

    /** It is about to close; it can still be cancelled. */
    public static final int INTERNAL_FRAME_CLOSING = 25550;

    /** It closed. */
    public static final int INTERNAL_FRAME_CLOSED = 25551;

    /** It was iconified. */
    public static final int INTERNAL_FRAME_ICONIFIED = 25552;

    /** It was restored. */
    public static final int INTERNAL_FRAME_DEICONIFIED = 25553;

    /** It took the focus. */
    public static final int INTERNAL_FRAME_ACTIVATED = 25554;

    /** It lost the focus. */
    public static final int INTERNAL_FRAME_DEACTIVATED = 25555;

    public InternalFrameEvent(JInternalFrame source, int id) {
        super(source, id);
    }

    public String paramString() {
        int id = getID();
        if (id == INTERNAL_FRAME_OPENED) {
            return "INTERNAL_FRAME_OPENED";
        }
        if (id == INTERNAL_FRAME_CLOSING) {
            return "INTERNAL_FRAME_CLOSING";
        }
        if (id == INTERNAL_FRAME_CLOSED) {
            return "INTERNAL_FRAME_CLOSED";
        }
        if (id == INTERNAL_FRAME_ICONIFIED) {
            return "INTERNAL_FRAME_ICONIFIED";
        }
        if (id == INTERNAL_FRAME_DEICONIFIED) {
            return "INTERNAL_FRAME_DEICONIFIED";
        }
        if (id == INTERNAL_FRAME_ACTIVATED) {
            return "INTERNAL_FRAME_ACTIVATED";
        }
        if (id == INTERNAL_FRAME_DEACTIVATED) {
            return "INTERNAL_FRAME_DEACTIVATED";
        }
        return "unknown type";
    }

    /** The internal frame. */
    public JInternalFrame getInternalFrame() {
        return (JInternalFrame) getSource();
    }
}
