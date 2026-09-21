package javax.swing.plaf.metal;

import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.JTextComponent;

/**
 * Metal's text field.
 *
 * <p>The only thing it adds is watching the {@code "editable"} property and, when it changes,
 * setting the background that corresponds: an editable field's is not the same as a read-only
 * one's, and the basic one does not tell them apart.
 *
 * <p>It does it in {@code propertyChange} and not in {@code installDefaults} because a field
 * becomes read-only at any moment -- a form that freezes while saving -- and the colour has to
 * follow it.
 */
public class MetalTextFieldUI extends BasicTextFieldUI {

    public MetalTextFieldUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalTextFieldUI();
    }

    public void propertyChange(PropertyChangeEvent e) {
        if ("editable".equals(e.getPropertyName())) {
            JTextComponent c = getComponent();
            if (c != null && (c.getBackground() instanceof javax.swing.plaf.UIResource)) {
                c.setBackground(c.isEditable()
                        ? MetalLookAndFeel.getWindowBackground()
                        : MetalLookAndFeel.getControl());
            }
        }
        super.propertyChange(e);
    }
}
