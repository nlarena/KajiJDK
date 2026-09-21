package javax.swing.plaf.basic;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalIconFactory;

/**
 * The basic look and feel of a check box: {@link BasicRadioButtonUI} with another prefix, and
 * therefore another default icon, the ticked square of
 * {@link MetalIconFactory#getCheckBoxIcon}.
 */
public class BasicCheckBoxUI extends BasicRadioButtonUI {

    private static final BasicCheckBoxUI checkboxUI = new BasicCheckBoxUI();

    private static final String propertyPrefix = "CheckBox.";

    public BasicCheckBoxUI() {
    }

    /** The shared look and feel. */
    public static ComponentUI createUI(JComponent b) {
        return checkboxUI;
    }

    public String getPropertyPrefix() {
        return propertyPrefix;
    }

    Icon defaultIcon() {
        return MetalIconFactory.getCheckBoxIcon();
    }
}
