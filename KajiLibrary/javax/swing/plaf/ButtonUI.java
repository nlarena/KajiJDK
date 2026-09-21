package javax.swing.plaf;

/**
 * A button's look and feel: the type through which {@code AbstractButton} talks to its UI.
 *
 * <p>It adds nothing to {@link ComponentUI}; it exists so that each family of components has its
 * own UI type and a look and feel cannot, by mistake, install a label's UI on a button.
 */
public abstract class ButtonUI extends ComponentUI {

    protected ButtonUI() {
    }
}
