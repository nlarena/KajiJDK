package javax.swing.plaf;

import javax.swing.JOptionPane;

/**
 * A {@link JOptionPane}'s look and feel.
 *
 * <h2>Only two methods</h2>
 *
 * <p>Almost everything an option pane does -- assembling the buttons, measuring the message,
 * choosing the icon -- is resolved by the look and feel from {@code installUI}. The only things
 * that have to be asked for afterwards are these two, and both exist for the same reason: the
 * pane does not know which components the look and feel assembled.
 *
 * <p>{@link #selectInitialValue} gives the focus to the button that corresponds -- the pane does
 * not have the buttons, the look and feel has them. {@link #containsCustomComponents} says
 * whether the message brought components of its own, which is what decides whether on closing
 * they have to be taken out so that they can be reused.
 */
public abstract class OptionPaneUI extends ComponentUI {

    /** For the subclasses. */
    protected OptionPaneUI() {
    }

    /** It gives the focus to the initial value; see the class note. */
    public abstract void selectInitialValue(JOptionPane op);

    /** Whether the message brought components of its own; see the class note. */
    public abstract boolean containsCustomComponents(JOptionPane op);
}
