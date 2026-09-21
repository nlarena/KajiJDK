package javax.swing;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * A button that is pressed: text, icon, or both, and an {@code ActionEvent} on releasing.
 *
 * <p>Almost everything is in {@link AbstractButton}: the model, the icons by state, the
 * alignments, the {@link Action}. What this class adds is the concept of the <em>default
 * button</em> -- the one Enter fires in a dialog --, and that concept lives in
 * {@code JRootPane}, which is not there. Hence {@link #isDefaultButton} is always
 * {@code false}: there is no root pane that has named it. {@link #isDefaultCapable} does work,
 * because it is a property of the button.
 *
 * <p>With no {@code UIManager}, {@link #updateUI} installs the basic look and feel directly; see
 * {@link BasicButtonUI} for the default values and where they come from.
 */
public class JButton extends AbstractButton implements Accessible {

    private static final String uiClassID = "ButtonUI";

    public JButton() {
        this(null, null);
    }

    public JButton(Icon icon) {
        this(null, icon);
    }

    public JButton(String text) {
        this(text, null);
    }

    /** A button that takes text, icon, mnemonic and state from that action, and fires it. */
    public JButton(Action a) {
        this();
        setAction(a);
    }

    public JButton(String text, Icon icon) {
        setModel(new DefaultButtonModel());
        init(text, icon);
    }

    /** It installs the basic look and feel; see the class note. */
    public void updateUI() {
        setUI((ButtonUI) BasicButtonUI.createUI(this));
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Whether it is its root pane's default button: never, because there is no root pane. */
    public boolean isDefaultButton() {
        return false;
    }

    /** Whether it can be a dialog's default button; {@code true} unless it is taken away. */
    public boolean isDefaultCapable() {
        return defaultCapable;
    }

    public void setDefaultCapable(boolean defaultCapable) {
        boolean old = this.defaultCapable;
        this.defaultCapable = defaultCapable;
        firePropertyChange("defaultCapable", old, defaultCapable);
    }

    /**
     * It leaves the hierarchy.
     *
     * <p>The JDK takes the chance to stop being its root pane's default button; with no root pane,
     * what is left is {@link AbstractButton#removeNotify}.
     */
    public void removeNotify() {
        super.removeNotify();
    }

    protected String paramString() {
        String defaultCapableString = defaultCapable ? "true" : "false";
        return super.paramString() + ",defaultCapable=" + defaultCapableString;
    }

    /** With no accessibility context: there is no assistive technology that reads it on this VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }
}
