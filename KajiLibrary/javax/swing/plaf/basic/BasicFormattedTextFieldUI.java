package javax.swing.plaf.basic;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * El aspecto basico de un campo con formato.
 *
 * <p>Es {@link BasicTextFieldUI} con otro prefijo, y nada mas. Un campo con formato se ve igual que
 * uno comun --misma vista, misma linea de base, mismo borde--; lo que lo hace distinto es el
 * formateador, que vive en {@link javax.swing.JFormattedTextField} y no tiene nada que ver con el
 * aspecto.
 *
 * <p>Existe igual, y no es de mas: el prefijo distinto es lo que deja que un aspecto le ponga a los
 * campos con formato un borde o un color que no tengan los comunes --marcar en rojo uno con un
 * valor invalido, por ejemplo-- sin tocar los demas.
 */
public class BasicFormattedTextFieldUI extends BasicTextFieldUI {

    public BasicFormattedTextFieldUI() {
        super();
    }

    /** Uno nuevo por campo: un UI de texto guarda el componente. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicFormattedTextFieldUI();
    }

    protected String getPropertyPrefix() {
        return "FormattedTextField";
    }
}
