package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse del mouse arrastrado sobre un menu; ver {@link MenuDragMouseEvent}.
 */
public interface MenuDragMouseListener extends EventListener {

    /** El arrastre entro en el elemento. */
    void menuDragMouseEntered(MenuDragMouseEvent e);

    /** El arrastre salio del elemento. */
    void menuDragMouseExited(MenuDragMouseEvent e);

    /** El arrastre se movio. */
    void menuDragMouseDragged(MenuDragMouseEvent e);

    /** Se solto el boton. */
    void menuDragMouseReleased(MenuDragMouseEvent e);
}
