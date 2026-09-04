package java.awt.event;

/**
 * Un oyente de ratón que no hace nada, para redefinir sólo lo que interese.

 <p>Implementa las **tres** interfaces de ratón, así que un solo objeto puede atender botones,
 movimiento y rueda. Es el adaptador más útil de todos justamente por eso: casi nadie quiere las tres
 por separado.
 */
public abstract class MouseAdapter implements MouseListener, MouseMotionListener, MouseWheelListener {

    /** Para las subclases. */
    protected MouseAdapter() {
    }

    /** No hace nada. */
    public void mouseClicked(MouseEvent e) {
    }

    /** No hace nada. */
    public void mousePressed(MouseEvent e) {
    }

    /** No hace nada. */
    public void mouseReleased(MouseEvent e) {
    }

    /** No hace nada. */
    public void mouseEntered(MouseEvent e) {
    }

    /** No hace nada. */
    public void mouseExited(MouseEvent e) {
    }

    /** No hace nada. */
    public void mouseDragged(MouseEvent e) {
    }

    /** No hace nada. */
    public void mouseMoved(MouseEvent e) {
    }

    /** No hace nada. */
    public void mouseWheelMoved(MouseWheelEvent e) {
    }
}
