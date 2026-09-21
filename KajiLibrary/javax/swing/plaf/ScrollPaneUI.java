package javax.swing.plaf;

/**
 * A pane with scroll bars' look and feel.
 *
 * <p>It adds nothing to {@link ComponentUI}. The only thing a pane's look and feel paints is the
 * viewport's border, if there is one; everything else is real components that paint themselves.
 */
public abstract class ScrollPaneUI extends ComponentUI {

    protected ScrollPaneUI() {
    }
}
