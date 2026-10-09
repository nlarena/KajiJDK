package javax.swing.plaf.synth;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Synth's check box menu item. It changes the prefix and nothing else.
 */
public class SynthCheckBoxMenuItemUI extends SynthMenuItemUI {

    public SynthCheckBoxMenuItemUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new SynthCheckBoxMenuItemUI();
    }

    protected String getPropertyPrefix() {
        return "CheckBoxMenuItem.";
    }

    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        super.paintBorder(context, g, x, y, w, h);
    }
}
