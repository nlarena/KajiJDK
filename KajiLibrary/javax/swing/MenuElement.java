package javax.swing;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Cualquier pieza de un menu: la barra, un menu, un item, un separador.
 *
 * <h2>Por que los eventos llegan con el camino</h2>
 *
 * <p>{@link #processMouseEvent} y {@link #processKeyEvent} reciben un arreglo con
 * <strong>todo el camino</strong> desde la barra hasta el elemento, y no solo el elemento. La razon
 * es que un menu abierto es una jerarquia viva: mover el mouse de un submenu a otro tiene que cerrar
 * lo que quedo atras, y para eso hay que saber por donde se venia.
 *
 * <p>Quien reparte esos eventos es {@link MenuSelectionManager}, que es tambien el que arma el
 * camino.
 *
 * <p>{@link #getComponent} existe porque un elemento de menu <em>tambien</em> es un componente que
 * se dibuja; la interfaz lo separa para que el mecanismo de menu no dependa de que sea un
 * {@link JComponent} en particular.
 */
public interface MenuElement {

    /** Atiende un evento de mouse, con el camino completo. */
    void processMouseEvent(MouseEvent event, MenuElement[] path, MenuSelectionManager manager);

    /** Atiende un evento de teclado, con el camino completo. */
    void processKeyEvent(KeyEvent event, MenuElement[] path, MenuSelectionManager manager);

    /** Aviso de que este elemento entro o salio de la seleccion. */
    void menuSelectionChanged(boolean isIncluded);

    /** Los sub-elementos, en orden. */
    MenuElement[] getSubElements();

    /** El componente que dibuja este elemento. */
    Component getComponent();
}
