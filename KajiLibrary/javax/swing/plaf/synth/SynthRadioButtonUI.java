package javax.swing.plaf.synth;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * El boton de opcion de Synth.
 *
 * <p>Un escalon mas abajo del conmutador, con el mismo cambio: el prefijo. La cadena es
 * {@code Button -> ToggleButton -> RadioButton -> CheckBox}, y cada eslabon existe nada mas que
 * para que el archivo de estilos pueda darle valores propios sin repetir el dibujo.
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
