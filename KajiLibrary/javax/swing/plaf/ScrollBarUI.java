package javax.swing.plaf;

/**
 * A scroll bar's look and feel.
 *
 * <p>It adds nothing to {@link ComponentUI}: the bar is a model plus two buttons, and everything
 * there is to know about it is asked through its API. It exists for the same reason as
 * {@link ButtonUI}.
 */
public abstract class ScrollBarUI extends ComponentUI {

    protected ScrollBarUI() {
    }
}
