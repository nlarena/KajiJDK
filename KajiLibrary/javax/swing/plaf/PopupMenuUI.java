package javax.swing.plaf;

import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.Popup;

/**
 * El aspecto de un {@link JPopupMenu}.
 *
 * <h2>Dos preguntas que no son geometria</h2>
 *
 * <p>Cual es el gesto que abre un menu contextual depende del sistema: en Windows es soltar el
 * boton derecho y en otros es apretarlo. Eso lo sabe el aspecto, no el menu, y por eso
 * {@link #isPopupTrigger} esta aca.
 *
 * <p>{@link #getPopup} arma la ventanita. Tampoco es del menu: la decision de dibujarla adentro de
 * la ventana o en una propia depende de si entra, y eso lo mide el aspecto.
 *
 * <p>Los dos tienen cuerpo, a diferencia de los demas aspectos: hay una respuesta razonable por
 * omision y obligar a escribirla en cada aspecto seria repetirla.
 */
public abstract class PopupMenuUI extends ComponentUI {

    protected PopupMenuUI() {
    }

    /** Si ese evento es el gesto que abre un menu contextual. */
    public boolean isPopupTrigger(MouseEvent e) {
        return e.isPopupTrigger();
    }

    /** La ventanita para ese menu, en ese punto de la pantalla. */
    public Popup getPopup(JPopupMenu popup, int x, int y) {
        javax.swing.PopupFactory f = javax.swing.PopupFactory.getSharedInstance();
        return f.getPopup(popup.getInvoker(), popup, x, y);
    }
}
