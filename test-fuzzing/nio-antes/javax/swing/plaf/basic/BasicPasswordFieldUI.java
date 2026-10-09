package javax.swing.plaf.basic;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Element;
import javax.swing.text.PasswordView;
import javax.swing.text.View;

/**
 * The basic look and feel of a password field.
 *
 * <p>Two things of its own, and both are the same idea: that what is typed should not be read.
 * The view is a {@link PasswordView}, which draws a fixed character in place of each letter, and
 * the character is installed here -- {@code PasswordField.echoChar} --.
 *
 * <p>The value measured in Metal (JDK 25) is the bullet {@code U+2022}, not the asterisk: the
 * asterisk is what {@link javax.swing.JPasswordField} brings from the factory and what the look
 * and feel overwrites as soon as it is installed.
 */
public class BasicPasswordFieldUI extends BasicTextFieldUI {

    private static final char DEFAULT_ECHO = '•';

    public BasicPasswordFieldUI() {
        super();
    }

    /** A new one per field: a text look and feel keeps the component. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicPasswordFieldUI();
    }

    protected String getPropertyPrefix() {
        return "PasswordField";
    }

    /** The usual plus the echo character; see the class note. */
    protected void installDefaults() {
        super.installDefaults();
        LookAndFeel.installProperty(getComponent(), "echoChar",
                Character.valueOf(DEFAULT_ECHO));
    }

    /** A {@link PasswordView}; see the class note. */
    public View create(Element elem) {
        return new PasswordView(elem);
    }
}
