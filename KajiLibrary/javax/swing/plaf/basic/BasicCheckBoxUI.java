package javax.swing.plaf.basic;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalIconFactory;

/**
 * El aspecto basico de una casilla: {@link BasicRadioButtonUI} con otro prefijo, y por lo tanto
 * otro icono por omision, el cuadrado con tilde de {@link MetalIconFactory#getCheckBoxIcon}.
 */
public class BasicCheckBoxUI extends BasicRadioButtonUI {

    private static final BasicCheckBoxUI checkboxUI = new BasicCheckBoxUI();

    private static final String propertyPrefix = "CheckBox.";

    public BasicCheckBoxUI() {
    }

    /** El aspecto compartido. */
    public static ComponentUI createUI(JComponent b) {
        return checkboxUI;
    }

    public String getPropertyPrefix() {
        return propertyPrefix;
    }

    Icon iconoPorOmision() {
        return MetalIconFactory.getCheckBoxIcon();
    }
}
