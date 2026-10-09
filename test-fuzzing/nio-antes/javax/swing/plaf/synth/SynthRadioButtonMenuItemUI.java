package javax.swing.plaf.synth;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Synth's radio button menu item. It changes the prefix and nothing else.
 */
public class SynthRadioButtonMenuItemUI extends SynthMenuItemUI {

    public SynthRadioButtonMenuItemUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new SynthRadioButtonMenuItemUI();
    }

    protected String getPropertyPrefix() {
        return "RadioButtonMenuItem.";
    }

    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        super.paintBorder(context, g, x, y, w, h);
    }
}
