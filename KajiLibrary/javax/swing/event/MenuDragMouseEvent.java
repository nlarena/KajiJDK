package javax.swing.event;

import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

/**
 * El mouse se arrastro sobre un menu abierto.
 *
 * <p>Es un {@link MouseEvent} <strong>mas el camino</strong>. Sin el camino, un item de menu que
 * recibe un arrastre no sabria de que submenu viene el mouse, y eso es lo que decide que cerrar al
 * pasar de una rama a otra. Ver {@link MenuElement}.
 */
public class MenuDragMouseEvent extends MouseEvent {

    private static final long serialVersionUID = 1L;

    private MenuElement[] path;
    private MenuSelectionManager manager;

    /** Sin cantidad de clics ni boton, que en un arrastre no aportan. */
    public MenuDragMouseEvent(Component source, int id, long when, int modifiers, int x, int y,
            int clickCount, boolean popupTrigger, MenuElement[] p, MenuSelectionManager m) {
        super(source, id, when, modifiers, x, y, clickCount, popupTrigger);
        this.path = p;
        this.manager = m;
    }

    /** Con posicion absoluta y boton. */
    public MenuDragMouseEvent(Component source, int id, long when, int modifiers, int x, int y,
            int xAbs, int yAbs, int clickCount, boolean popupTrigger, MenuElement[] p,
            MenuSelectionManager m) {
        super(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount, popupTrigger,
                MouseEvent.NOBUTTON);
        this.path = p;
        this.manager = m;
    }

    /** El camino desde la barra hasta el elemento. */
    public MenuElement[] getPath() {
        return this.path;
    }

    /** Quien administra la seleccion del menu. */
    public MenuSelectionManager getMenuSelectionManager() {
        return this.manager;
    }
}
