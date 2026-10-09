package javax.swing.plaf;

import javax.swing.JComboBox;

/**
 * A {@link JComboBox}'s look and feel.
 *
 * <h2>Only the drop-down part</h2>
 *
 * <p>The three methods are about the part the list cannot handle on its own: the little window
 * that opens. Opening and closing it belongs to the look and feel because it depends on whether
 * it is drawn inside the window or in one of its own, and that is decided by the look and feel
 * according to the size and what is around.
 */
public abstract class ComboBoxUI extends ComponentUI {

    protected ComboBoxUI() {
    }

    /** Opens or closes the drop-down. */
    public abstract void setPopupVisible(JComboBox<?> c, boolean v);

    public abstract boolean isPopupVisible(JComboBox<?> c);

    /** Whether the list can take the focus with the tab key. */
    public abstract boolean isFocusTraversable(JComboBox<?> c);
}
