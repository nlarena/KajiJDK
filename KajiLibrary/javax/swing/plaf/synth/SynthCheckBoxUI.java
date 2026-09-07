package javax.swing.plaf.synth;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * La casilla de Synth, el ultimo eslabon de la cadena que arranca en {@link SynthButtonUI}.
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
