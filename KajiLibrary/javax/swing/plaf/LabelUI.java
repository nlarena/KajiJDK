package javax.swing.plaf;

/**
 * A label's look and feel.
 *
 * <p>It adds nothing to {@link ComponentUI}, and even so it exists: it is the <em>type</em>
 * {@code JLabel.setUI} asks for, so that a button's look and feel cannot be installed on a label
 * by mistake. Every component has one of these, empty, for the same reason.
 */
public abstract class LabelUI extends ComponentUI {

    /** For the subclasses. */
    protected LabelUI() {
    }
}
