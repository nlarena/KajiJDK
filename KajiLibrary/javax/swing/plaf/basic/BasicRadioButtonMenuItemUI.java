package javax.swing.plaf.basic;

import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.plaf.ComponentUI;

/**
 * El aspecto basico de un item de menu de opcion.
 *
 * <p>Igual que {@link BasicCheckBoxMenuItemUI} salvo el icono: un punto de 6 x 6 en vez de un tilde
 * de 9 x 9. La diferencia de dibujo es toda la diferencia -- un tilde dice "esto esta prendido" y
 * un punto dice "de este grupo, este" --, y quien hace cumplir el "de este grupo" no es el aspecto
 * sino el {@code ButtonGroup} que agrupa los items.
 *
 * <p>Vale la misma nota que en el marcable: el punto del basico mide 6 x 6 y el de Metal 10 x 10,
 * asi que el ancho preferido no coincide con el del JDK.
 */
public class BasicRadioButtonMenuItemUI extends BasicMenuItemUI {

    public BasicRadioButtonMenuItemUI() {
    }

    /** Uno nuevo por item: guarda el componente y sus escuchas. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicRadioButtonMenuItemUI();
    }

    protected String getPropertyPrefix() {
        return "RadioButtonMenuItem";
    }

    /** Lo de siempre mas el punto; ver la nota de la clase. */
    protected void installDefaults() {
        super.installDefaults();
        checkIcon = BasicIconFactory.getRadioButtonMenuItemIcon();
    }

    /** Igual que en el marcable: soltar encima dispara y cierra. */
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
