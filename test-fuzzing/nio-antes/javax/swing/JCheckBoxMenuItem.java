package javax.swing;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * A menu item that is also switched on and off.
 *
 * <h2>Two names for the same state</h2>
 *
 * <p>{@link #getState} and {@code isSelected} are the same, and so are {@link #setState} and
 * {@code setSelected}. The pair with "state" exists because a menu tick is thought of as on or
 * off and not as chosen; both write into the same button model, so they cannot get out of
 * step.
 *
 * <h2>It stays switched on by itself</h2>
 *
 * <p>Unlike a {@link JRadioButtonMenuItem}, nothing switches it off when another is switched on:
 * it belongs to no group unless it is put into one.
 */
public class JCheckBoxMenuItem extends JMenuItem implements SwingConstants, Accessible {

    private static final String uiClassID = "CheckBoxMenuItemUI";

    /** With neither text nor icon, switched off. */
    public JCheckBoxMenuItem() {
        this(null, null, false);
    }

    /** With that icon, switched off. */
    public JCheckBoxMenuItem(Icon icon) {
        this(null, icon, false);
    }

    /** With that text, switched off. */
    public JCheckBoxMenuItem(String text) {
        this(text, null, false);
    }

    /** Taking text, icon and the rest from that action. */
    public JCheckBoxMenuItem(Action a) {
        this();
        setAction(a);
    }

    /** With text and icon, switched off. */
    public JCheckBoxMenuItem(String text, Icon icon) {
        this(text, icon, false);
    }

    /** With that text, switched on or not. */
    public JCheckBoxMenuItem(String text, boolean b) {
        this(text, null, b);
    }

    /** With text, icon and state. */
    public JCheckBoxMenuItem(String text, Icon icon, boolean b) {
        super(text, icon);
        setModel(new JToggleButton.ToggleButtonModel());
        setSelected(b);
        setFocusable(false);
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Whether it is switched on; see the class note. */
    public boolean getState() {
        return isSelected();
    }

    public synchronized void setState(boolean b) {
        setSelected(b);
    }

    /**
     * The item's text if it is switched on, and null if not; it is what {@code ItemSelectable} asks
     * for.
     */
    public Object[] getSelectedObjects() {
        if (!isSelected()) {
            return null;
        }
        Object[] selectedObjects = new Object[1];
        selectedObjects[0] = getText();
        return selectedObjects;
    }

    protected String paramString() {
        return super.paramString();
    }

    /** A tick if it takes its state from the action; see {@code AbstractButton}. */
    boolean shouldUpdateSelectedStateFromAction() {
        return true;
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
