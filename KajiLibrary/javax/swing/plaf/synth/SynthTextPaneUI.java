package javax.swing.plaf.synth;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * El panel de texto con estilos de Synth. Cambia el prefijo y nada mas.
 */
public class SynthTextPaneUI extends SynthEditorPaneUI {

    public SynthTextPaneUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new SynthTextPaneUI();
    }

    protected String getPropertyPrefix() {
        return "TextPane.";
    }
}
