package java.awt.event;

/**
 * Un {@link FocusListener} que no hace nada, para redefinir sólo lo que interese.
 */
public abstract class FocusAdapter implements FocusListener {

    /** Para las subclases. */
    protected FocusAdapter() {
    }

    /** No hace nada. */
    public void focusGained(FocusEvent e) {
    }

    /** No hace nada. */
    public void focusLost(FocusEvent e) {
    }
}
