package javax.swing.plaf;

/**
 * A scrolling viewport's look and feel.
 *
 * <p>It adds nothing to {@link ComponentUI}: a viewport draws nothing but its background. It
 * exists for the same reason as {@link ButtonUI}, so that each family has its type.
 */
public abstract class ViewportUI extends ComponentUI {

    protected ViewportUI() {
    }
}
