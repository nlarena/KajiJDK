package javax.swing.plaf.synth;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Synth's toggle button, one step below {@link SynthButtonUI} in the chain.
 */
public class SynthToggleButtonUI extends SynthButtonUI {

    public SynthToggleButtonUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new SynthToggleButtonUI();
    }

    protected String getPropertyPrefix() {
        return "ToggleButton.";
    }

    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        super.paintBorder(context, g, x, y, w, h);
    }
}
