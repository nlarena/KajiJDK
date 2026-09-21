package javax.swing;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicCheckBoxUI;

/**
 * A check box: a {@link JToggleButton} whose icon is the look and feel's ticked square.
 *
 * <p>It adds no state, save {@link #setBorderPaintedFlat}, which some looks and feels use in
 * order to draw the box with no relief. The text goes to the right of the icon and everything
 * aligned to the start, which is the visible difference with a button: a check box does not
 * centre.
 *
 * <p>An {@link Action}'s icon is ignored on purpose: a check box always shows its square, and
 * the action's icon is for buttons and menus.
 */
public class JCheckBox extends JToggleButton implements Accessible {

    public static final String BORDER_PAINTED_FLAT_CHANGED_PROPERTY = "flat";

    private static final String uiClassID = "CheckBoxUI";

    private boolean flat = false;

    public JCheckBox() {
        this(null, null, false);
    }

    public JCheckBox(Icon icon) {
        this(null, icon, false);
    }

    public JCheckBox(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    public JCheckBox(String text) {
        this(text, null, false);
    }

    public JCheckBox(Action a) {
        this();
        setAction(a);
    }

    public JCheckBox(String text, boolean selected) {
        this(text, null, selected);
    }

    public JCheckBox(String text, Icon icon) {
        this(text, icon, false);
    }

    public JCheckBox(String text, Icon icon, boolean selected) {
        super(text, icon, selected);
        setUIProperty("borderPainted", Boolean.FALSE);
        setHorizontalAlignment(LEADING);
    }

    /**
     * Whether the look and feel should draw the box flat; the basic one does not draw it
     * differently.
     */
    public void setBorderPaintedFlat(boolean b) {
        boolean old = flat;
        flat = b;
        firePropertyChange(BORDER_PAINTED_FLAT_CHANGED_PROPERTY, old, flat);
        if (b != old) {
            revalidate();
            repaint();
        }
    }

    public boolean isBorderPaintedFlat() {
        return flat;
    }

    /** It installs the basic look and feel; see {@code JButton#updateUI}. */
    public void updateUI() {
        setUI((ButtonUI) BasicCheckBoxUI.createUI(this));
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Nothing: see the class note. */
    void setIconFromAction(Action a) {
    }

    protected String paramString() {
        return super.paramString();
    }

    /** With no accessibility context: there is no assistive technology that reads it on this VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }
}
