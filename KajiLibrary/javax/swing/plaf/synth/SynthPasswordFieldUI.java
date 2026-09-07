package javax.swing.plaf.synth;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * El campo de contrasena de Synth.
 *
 * <p>Cambia el prefijo del que salen sus valores y nada mas; el caracter de eco lo pone
 * {@code JPasswordField}, no el aspecto.
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
