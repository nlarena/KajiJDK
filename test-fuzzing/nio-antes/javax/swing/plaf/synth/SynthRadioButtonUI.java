package javax.swing.plaf.synth;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Synth's radio button, one link below the toggle in the chain that starts at
 * {@link SynthButtonUI}.
 */
public class SynthRadioButtonUI extends SynthToggleButtonUI {

    public SynthRadioButtonUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new SynthRadioButtonUI();
    }

    protected String getPropertyPrefix() {
        return "RadioButton.";
    }

    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        super.paintBorder(context, g, x, y, w, h);
    }
}
