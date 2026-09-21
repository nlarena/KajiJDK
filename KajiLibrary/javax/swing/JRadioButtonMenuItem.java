package javax.swing;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * A menu item that is chosen among several.
 *
 * <h2>The group does not come set</h2>
 *
 * <p>The class on its own switches nobody off: for choosing one to switch the previous one off,
 * they all have to be put into a {@link ButtonGroup}. With no group it behaves just like a
 * {@link JCheckBoxMenuItem}, only it is drawn round. It is the commonest mistake with this class
 * and it gives no warning.
 */
public class JRadioButtonMenuItem extends JMenuItem implements Accessible {

    private static final String uiClassID = "RadioButtonMenuItemUI";

    /** With neither text nor icon, unchosen. */
    public JRadioButtonMenuItem() {
        this(null, null, false);
    }

    /** With that icon. */
    public JRadioButtonMenuItem(Icon icon) {
        this(null, icon, false);
    }

    /** With that text. */
    public JRadioButtonMenuItem(String text) {
        this(text, null, false);
    }

    /** Taking text, icon and the rest from that action. */
    public JRadioButtonMenuItem(Action a) {
        this();
        setAction(a);
    }

    /** With text and icon. */
    public JRadioButtonMenuItem(String text, Icon icon) {
        this(text, icon, false);
    }

    /** With that text, chosen or not. */
    public JRadioButtonMenuItem(String text, boolean selected) {
        this(text);
        setSelected(selected);
    }

    /** With that icon, chosen or not. */
    public JRadioButtonMenuItem(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    /** With text, icon and state. */
    public JRadioButtonMenuItem(String text, Icon icon, boolean selected) {
        super(text, icon);
        setModel(new JToggleButton.ToggleButtonModel());
        setSelected(selected);
        setFocusable(false);
    }

    public String getUIClassID() {
        return uiClassID;
    }

    protected String paramString() {
        return super.paramString();
    }

    /** It takes its state from the action; see {@code AbstractButton}. */
    boolean shouldUpdateSelectedStateFromAction() {
        return true;
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
