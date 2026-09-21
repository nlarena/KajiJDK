package java.awt.dnd;

import java.awt.Point;
import java.util.EventObject;

/**
 * The base of the events that reach the **source** of a drag.
 *
 * <p>The position is of the **screen**, not of the component, and it cannot be otherwise: the drag
 * may be passing over another window or another program, so there is no component of one's own to
 * measure against.
 *
 * <p>There may be no position: the one-argument constructor builds an event with no point, and
 * there {@link #getLocation} returns `null`. It is what fits when the event does not come from a
 * movement but from the end of the drag.
 */
public class DragSourceEvent extends EventObject {

    private static final long serialVersionUID = -763287114604032641L;

    private final boolean locationSpecified;
    private final int x;
    private final int y;

    /**
     * With no position.
     *
     * @throws IllegalArgumentException if the context is `null`
     */
    public DragSourceEvent(DragSourceContext dsc) {
        super(dsc);
        this.locationSpecified = false;
        this.x = 0;
        this.y = 0;
    }

    /**
     * With the position on the screen.
     *
     * @throws IllegalArgumentException if the context is `null`
     */
    public DragSourceEvent(DragSourceContext dsc, int x, int y) {
        super(dsc);
        this.locationSpecified = true;
        this.x = x;
        this.y = y;
    }

    /** The context of the drag under way. */
    public DragSourceContext getDragSourceContext() {
        return (DragSourceContext) this.getSource();
    }

    /**
     * Where the pointer is, in screen coordinates.
     *
     * @return the point, or `null` if this event brings no position
     */
    public Point getLocation() {
        if (this.locationSpecified) {
            return new Point(this.x, this.y);
        }
        return null;
    }

    /** The X on the screen, or 0 if there is no position. */
    public int getX() {
        return this.x;
    }

    /** The Y on the screen, or 0 if there is no position. */
    public int getY() {
        return this.y;
    }
}
