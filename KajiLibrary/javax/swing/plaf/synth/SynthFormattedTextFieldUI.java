package javax.swing.plaf.synth;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * El campo con formato de Synth. Cambia el prefijo y nada mas.
 */
public class SynthFormattedTextFieldUI extends SynthTextFieldUI {

    public SynthFormattedTextFieldUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new SynthFormattedTextFieldUI();
    }

    protected String getPropertyPrefix() {
        return "FormattedTextField.";
    }
}
