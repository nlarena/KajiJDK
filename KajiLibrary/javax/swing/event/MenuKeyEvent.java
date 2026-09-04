package javax.swing.event;

import java.awt.Component;
import java.awt.event.KeyEvent;

import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

/**
 * Se uso el teclado con un menu abierto.
 *
 * <p>El gemelo de {@link MenuDragMouseEvent} para las teclas, y con la misma razon de ser: navegar
 * un menu con las flechas necesita saber en que rama se esta, y eso lo dice el camino, no el
 * elemento suelto.
 */
public class MenuKeyEvent extends KeyEvent {

    private static final long serialVersionUID = 1L;

    private MenuElement[] path;
    private MenuSelectionManager manager;

    public MenuKeyEvent(Component source, int id, long when, int modifiers, int keyCode,
            char keyChar, MenuElement[] p, MenuSelectionManager m) {
        super(source, id, when, modifiers, keyCode, keyChar);
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
