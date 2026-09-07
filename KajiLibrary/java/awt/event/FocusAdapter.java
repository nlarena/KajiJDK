package java.awt.event;

/**
 * A {@link FocusListener} that does nothing, to override only what matters.
 */
public abstract class FocusAdapter implements FocusListener {

    /** For the subclasses. */
    protected FocusAdapter() {
    }

    /** Does nothing. */
    public void focusGained(FocusEvent e) {
    }

    /** Does nothing. */
    public void focusLost(FocusEvent e) {
    }
}
