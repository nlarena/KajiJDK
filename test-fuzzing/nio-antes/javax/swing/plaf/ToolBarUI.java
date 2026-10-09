package javax.swing.plaf;

/**
 * The look and feel of a tool bar.
 *
 * <h2>An empty class with a purpose</h2>
 *
 * <p>It adds no method over {@link ComponentUI}: it exists to name the type. The component
 * declares that its look and feel is a {@code ToolBarUI} and not just any {@code ComponentUI},
 * and that makes installing another component's look and feel a compile error instead of a
 * failure while drawing.
 *
 * <p>The looks and feels that do have something to ask -- {@link ListUI},
 * {@link ComboBoxUI}, {@link SplitPaneUI} -- declare their methods; those that do not are
 * left like this.
 */
public abstract class ToolBarUI extends ComponentUI {

    protected ToolBarUI() {
    }
}
