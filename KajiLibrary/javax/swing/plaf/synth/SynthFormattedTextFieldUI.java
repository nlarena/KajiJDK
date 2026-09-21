package javax.swing.plaf.synth;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Synth's formatted field. It changes the prefix and nothing else.
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
