package javax.swing.plaf.synth;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Synth's password field.
 *
 * <p>It changes the prefix its values come from and nothing else; the echo character is set by
 * {@code JPasswordField}, not by the look and feel.
 */
public class SynthPasswordFieldUI extends SynthTextFieldUI {

    public SynthPasswordFieldUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new SynthPasswordFieldUI();
    }

    protected String getPropertyPrefix() {
        return "PasswordField.";
    }
}
