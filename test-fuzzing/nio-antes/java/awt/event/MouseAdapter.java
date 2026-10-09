package java.awt.event;

/**
 * A mouse listener that does nothing, to override only what matters.
 *
 * <p>It implements the **three** mouse interfaces, so a single object can attend to buttons, motion
 * and wheel. It is the most useful adapter of all precisely because of that: hardly anyone wants the
 * three separately.
 */
public abstract class MouseAdapter implements MouseListener, MouseMotionListener, MouseWheelListener {

    /** For the subclasses. */
    protected MouseAdapter() {
    }

    /** Does nothing. */
    public void mouseClicked(MouseEvent e) {
    }

    /** Does nothing. */
    public void mousePressed(MouseEvent e) {
    }

    /** Does nothing. */
    public void mouseReleased(MouseEvent e) {
    }

    /** Does nothing. */
    public void mouseEntered(MouseEvent e) {
    }

    /** Does nothing. */
    public void mouseExited(MouseEvent e) {
    }

    /** Does nothing. */
    public void mouseDragged(MouseEvent e) {
    }

    /** Does nothing. */
    public void mouseMoved(MouseEvent e) {
    }

    /** Does nothing. */
    public void mouseWheelMoved(MouseWheelEvent e) {
    }
}
