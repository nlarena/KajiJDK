package java.awt.event;

import java.awt.Component;
import java.awt.Rectangle;

/**
 * A part of a component has to be repainted.
 *
 * <p>It is AWT's only event that **is not handed to a listener**: there is no `PaintListener`. It
 * goes straight to `Component.paint` or `Component.update`, and that is why it lives in the event
 * package but takes no part in the listener model.
 *
 * <p>The rectangle to update is what makes repainting cheap: instead of redrawing the whole component
 * when a window uncovers it, the uncovered piece is redrawn.
 */
public class PaintEvent extends ComponentEvent {

    private static final long serialVersionUID = 1267492026433337593L;

    /** It has to be painted, starting by clearing the background. */
    public static final int PAINT = 800;

    /** The family's first identifier. */
    public static final int PAINT_FIRST = 800;

    /** The family's last identifier. */
    public static final int PAINT_LAST = 801;

    /** It has to be updated, without clearing the background. */
    public static final int UPDATE = 801;

    private Rectangle updateRect;

    /**
     * With the component, the identifier and the rectangle.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public PaintEvent(Component source, int id, Rectangle updateRect) {
        super(source, id);
        this.updateRect = updateRect;
    }

    /** Which part has to be repainted. */
    public Rectangle getUpdateRect() {
        return this.updateRect;
    }

    /**
     * Changes which part has to be repainted.
     *
     * <p>It serves to merge several requests into one: the system grows the rectangle instead of
     * queueing two events.
     */
    public void setUpdateRect(Rectangle updateRect) {
        this.updateRect = updateRect;
    }

    public String paramString() {
        String type;
        if (this.id == PAINT) {
            type = "PAINT";
        } else if (this.id == UPDATE) {
            type = "UPDATE";
        } else {
            type = "unknown type";
        }
        return type + ",updateRect=" + this.updateRect;
    }
}
