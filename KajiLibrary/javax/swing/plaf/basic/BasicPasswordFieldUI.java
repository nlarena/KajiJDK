package javax.swing.plaf.basic;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Element;
import javax.swing.text.PasswordView;
import javax.swing.text.View;

/**
 * El aspecto basico de un campo de contrasena.
 *
 * <p>Dos cosas propias, y las dos son la misma idea: que no se lea lo escrito. La vista es una
 * {@link PasswordView}, que dibuja un caracter fijo en lugar de cada letra, y el caracter se
 * instala aca -- {@code PasswordField.echoChar} --.
 *
 * <p>El valor medido en Metal (JDK 25) es el punto grueso {@code U+2022}, no el asterisco: el
 * asterisco es lo que trae {@link javax.swing.JPasswordField} de fabrica y lo que el aspecto pisa
 * apenas se instala.
 */
public class BasicPasswordFieldUI extends BasicTextFieldUI {

    private static final char ECO_POR_OMISION = '•';

    public BasicPasswordFieldUI() {
        super();
    }

    /** Uno nuevo por campo: un UI de texto guarda el componente. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicPasswordFieldUI();
    }

    protected String getPropertyPrefix() {
        return "PasswordField";
    }

    /** Lo de siempre mas el caracter de eco; ver la nota de la clase. */
    protected void installDefaults() {
        super.installDefaults();
        LookAndFeel.installProperty(getComponent(), "echoChar",
                Character.valueOf(ECO_POR_OMISION));
    }

    /** Una {@link PasswordView}; ver la nota de la clase. */
    public View create(Element elem) {
        return new PasswordView(elem);
    }
}
