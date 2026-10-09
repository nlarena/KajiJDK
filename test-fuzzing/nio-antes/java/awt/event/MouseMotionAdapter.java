package java.awt.event;

/**
 * A {@link MouseMotionListener} that does nothing, to override only what matters.
 */
public abstract class MouseMotionAdapter implements MouseMotionListener {

    /** For the subclasses. */
    protected MouseMotionAdapter() {
    }

    /** Does nothing. */
    public void mouseDragged(MouseEvent e) {
    }

    /** Does nothing. */
    public void mouseMoved(MouseEvent e) {
    }
}
