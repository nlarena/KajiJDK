package java.awt.event;

/**
 * Un {@link MouseMotionListener} que no hace nada, para redefinir sólo lo que interese.
 */
public abstract class MouseMotionAdapter implements MouseMotionListener {

    /** Para las subclases. */
    protected MouseMotionAdapter() {
    }

    /** No hace nada. */
    public void mouseDragged(MouseEvent e) {
    }

    /** No hace nada. */
    public void mouseMoved(MouseEvent e) {
    }
}
