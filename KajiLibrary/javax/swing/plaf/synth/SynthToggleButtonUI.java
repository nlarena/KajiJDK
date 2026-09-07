package javax.swing.plaf.synth;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * El conmutador de Synth.
 *
 * <p>Hereda todo de {@link SynthButtonUI} y cambia el prefijo, que es de donde salen sus valores en
 * el archivo de estilos. Un conmutador se dibuja igual que un boton; lo que lo distingue es que se
 * queda elegido, y eso ya lo dice el estado del contexto.
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
