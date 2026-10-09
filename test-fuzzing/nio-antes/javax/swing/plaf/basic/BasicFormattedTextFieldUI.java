package javax.swing.plaf.basic;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * The basic look and feel of a formatted field.
 *
 * <p>It is {@link BasicTextFieldUI} with another prefix, and nothing else. A formatted field
 * looks the same as an ordinary one -- same view, same baseline, same border --; what makes it
 * different is the formatter, which lives in {@link javax.swing.JFormattedTextField} and has
 * nothing to do with the look and feel.
 *
 * <p>It exists all the same, and it is not spare: the different prefix is what lets a look and
 * feel give formatted fields a border or a colour that ordinary ones do not have -- marking one
 * with an invalid value in red, for instance -- without touching the rest.
 */
public class BasicFormattedTextFieldUI extends BasicTextFieldUI {

    public BasicFormattedTextFieldUI() {
        super();
    }

    /** A new one per field: a text look and feel keeps the component. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicFormattedTextFieldUI();
    }

    protected String getPropertyPrefix() {
        return "FormattedTextField";
    }
}
