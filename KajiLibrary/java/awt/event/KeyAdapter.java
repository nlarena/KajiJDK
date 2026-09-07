package java.awt.event;

/**
 * A {@link KeyListener} that does nothing, to override only what matters.
 */
public abstract class KeyAdapter implements KeyListener {

    /** For the subclasses. */
    protected KeyAdapter() {
    }

    /** Does nothing. */
    public void keyTyped(KeyEvent e) {
    }

    /** Does nothing. */
    public void keyPressed(KeyEvent e) {
    }

    /** Does nothing. */
    public void keyReleased(KeyEvent e) {
    }
}
