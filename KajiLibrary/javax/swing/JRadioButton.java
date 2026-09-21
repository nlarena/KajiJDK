package javax.swing;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;

/**
 * A radio button: a {@link JToggleButton} with the look and feel's circle, meant to live in a
 * {@link ButtonGroup}.
 *
 * <p>The exclusion is not here but in the group and in {@code ToggleButtonModel}: a loose radio
 * button is ticked and unticked like a check box. Like the check box, it ignores its
 * {@link Action}'s icon.
 */
public class JRadioButton extends JToggleButton implements Accessible {

    private static final String uiClassID = "RadioButtonUI";

    public JRadioButton() {
        this(null, null, false);
    }

    public JRadioButton(Icon icon) {
        this(null, icon, false);
    }

    public JRadioButton(Action a) {
        this();
        setAction(a);
    }

    public JRadioButton(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    public JRadioButton(String text) {
        this(text, null, false);
    }

    public JRadioButton(String text, boolean selected) {
        this(text, null, selected);
    }

    public JRadioButton(String text, Icon icon) {
        this(text, icon, false);
    }

    public JRadioButton(String text, Icon icon, boolean selected) {
        super(text, icon, selected);
        setBorderPainted(false);
        setHorizontalAlignment(LEADING);
    }

    /** It installs the basic look and feel; see {@code JButton#updateUI}. */
    public void updateUI() {
        setUI((ButtonUI) BasicRadioButtonUI.createUI(this));
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
