package java.awt.event;

/**
 * Un {@link KeyListener} que no hace nada, para redefinir sólo lo que interese.
 */
public abstract class KeyAdapter implements KeyListener {

    /** Para las subclases. */
    protected KeyAdapter() {
    }

    /** No hace nada. */
    public void keyTyped(KeyEvent e) {
    }

    /** No hace nada. */
    public void keyPressed(KeyEvent e) {
    }

    /** No hace nada. */
    public void keyReleased(KeyEvent e) {
    }
}
