package javax.swing.plaf.synth;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Synth's check box, the last link of the chain that starts at {@link SynthButtonUI}.
 */
public class SynthCheckBoxUI extends SynthRadioButtonUI {

    public SynthCheckBoxUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new SynthCheckBoxUI();
    }

    protected String getPropertyPrefix() {
        return "CheckBox.";
    }

    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        super.paintBorder(context, g, x, y, w, h);
    }
}
