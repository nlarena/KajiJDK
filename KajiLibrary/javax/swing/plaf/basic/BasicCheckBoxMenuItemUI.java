package javax.swing.plaf.basic;

import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.plaf.ComponentUI;

/**
 * El aspecto basico de un item de menu marcable.
 *
 * <p>Dos cosas propias. La primera es el tilde: {@link BasicMenuItemUI} deja
 * {@link BasicMenuItemUI#checkIcon} en nulo, y aca se pone el de
 * {@link BasicIconFactory#getCheckBoxMenuItemIcon}, que dibuja solo cuando el item esta marcado.
 *
 * <p>La segunda es {@link #processMouseEvent}: soltar el boton encima de un item marcable lo
 * dispara <em>y</em> cierra el menu, igual que un item comun. Que el item tenga estado no cambia
 * eso; lo que cambia es que despues de cerrarse queda con el tilde puesto.
 *
 * <h2>El tilde no es del mismo tamano que el de Metal</h2>
 *
 * <p>El basico mide 9 x 9 y el de Metal 10 x 10, y por eso el ancho preferido de un item marcable
 * no coincide con el del JDK: son dos pixeles. Es el mismo hueco que en todo el paquete --sin tabla
 * de aspecto instalada, los iconos son los del basico y no los del aspecto de verdad--, y esta
 * dicho aca para que no parezca un error de la cuenta.
 */
public class BasicCheckBoxMenuItemUI extends BasicMenuItemUI {

    public BasicCheckBoxMenuItemUI() {
    }

    /** Uno nuevo por item: guarda el componente y sus escuchas. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicCheckBoxMenuItemUI();
    }

    protected String getPropertyPrefix() {
        return "CheckBoxMenuItem";
    }

    /** Lo de siempre mas el tilde; ver la nota de la clase. */
    protected void installDefaults() {
        super.installDefaults();
        checkIcon = BasicIconFactory.getCheckBoxMenuItemIcon();
    }

    /**
     * Dispara el item si el mouse se solto encima, y cierra el menu.
     *
     * <p>Lo llama {@code MenuSelectionManager} mientras el menu esta abierto: en ese momento el
     * item no recibe eventos propios, porque el que los reparte es el administrador.
     */
    public void processMouseEvent(JMenuItem item, MouseEvent e, MenuElement[] path,
            MenuSelectionManager manager) {
        java.awt.Point p = e.getPoint();
        if (p.x >= 0 && p.x < item.getWidth() && p.y >= 0 && p.y < item.getHeight()) {
            if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                manager.clearSelectedPath();
                item.doClick(0);
                item.setArmed(false);
            } else {
                manager.setSelectedPath(path);
            }
        } else if (item.getModel().isArmed()) {
            int c = path.length - 1;
            MenuElement[] newPath = new MenuElement[c];
            for (int i = 0; i < c; i++) {
                newPath[i] = path[i];
            }
            manager.setSelectedPath(newPath);
        }
    }
}
