package javax.swing.plaf.metal;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Metal's check box.
 *
 * <p>All the drawing is done by {@link MetalRadioButtonUI}, which it inherits from. The only
 * thing this class changes is {@link #getPropertyPrefix}, which becomes {@code "CheckBox."}:
 * with that, the same four methods of the class above read {@code "CheckBox.focus"} instead of
 * {@code "RadioButton.focus"} and the check box can have colours of its own without a single
 * repeated line of drawing.
 *
 * <p>It is the shortest class in the package and the one that best shows what the prefix is for.
 */
public class MetalCheckBoxUI extends MetalRadioButtonUI {

    private static final MetalCheckBoxUI SHARED = new MetalCheckBoxUI();

    public MetalCheckBoxUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return SHARED;
    }

    public String getPropertyPrefix() {
        return "CheckBox.";
    }

    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
    }

    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
    }
}
