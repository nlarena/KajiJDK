package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuDragMouseListener;
import javax.swing.event.MenuKeyListener;
import javax.swing.event.MenuListener;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;

/**
 * El aspecto basico de un menu.
 *
 * <p>Un {@link JMenu} <em>es</em> un item de menu --hereda de {@code JMenuItem}--, asi que casi todo
 * viene de {@link BasicMenuItemUI}. Lo propio son tres cosas.
 *
 * <h2>El menu de barra no se estira</h2>
 *
 * <p>{@link #getMaximumSize} devuelve el ancho preferido y un alto infinito, pero <em>solo</em> para
 * un menu que cuelga de la barra. Sin eso, el acomodador de la barra le daria todo el ancho
 * sobrante al primer menu y "Archivo" ocuparia media pantalla. Un menu de adentro devuelve
 * {@code null} como cualquier item, porque ahi lo reparte {@code DefaultMenuLayout}.
 *
 * <h2>La demora antes de abrir</h2>
 *
 * <p>{@link #setupPostTimer} arma el reloj que abre el submenu cuando el mouse se queda quieto
 * encima. La demora sale de {@code Menu.delay}, y esta en 200 milisegundos. La demora existe para
 * que pasar el mouse por arriba de camino a otro lado no abra tres submenus.
 *
 * <h2>Avisar que se abrio</h2>
 *
 * <p>El {@link MenuListener} es lo que hace que {@code menuSelected} llegue antes de que el submenu
 * se muestre: es el momento en que un programa puede armar el contenido del menu segun el estado,
 * en vez de tener que mantenerlo al dia todo el tiempo.
 */
public class BasicMenuUI extends BasicMenuItemUI {

    protected ChangeListener changeListener;
    protected MenuListener menuListener;

    public BasicMenuUI() {
    }

    /** Uno nuevo por menu: guarda el componente y sus escuchas. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicMenuUI();
    }

    protected String getPropertyPrefix() {
        return "Menu";
    }

    protected void installDefaults() {
        super.installDefaults();
        // El menu tiene su propia flecha: la del item apunta a un submenu que cuelga al costado,
        // y la del menu tambien, pero un aspecto puede querer dibujarlas distinto.
        arrowIcon = BasicIconFactory.getMenuArrowIcon();
        ((JMenu) menuItem).setDelay(200);
    }

    protected void uninstallDefaults() {
        super.uninstallDefaults();
    }

    protected void installListeners() {
        super.installListeners();
        changeListener = createChangeListener(menuItem);
        if (changeListener != null) {
            menuItem.addChangeListener(changeListener);
        }
        menuListener = createMenuListener(menuItem);
        if (menuListener != null) {
            ((JMenu) menuItem).addMenuListener(menuListener);
        }
    }

    protected void uninstallListeners() {
        super.uninstallListeners();
        if (changeListener != null) {
            menuItem.removeChangeListener(changeListener);
        }
        if (menuListener != null) {
            ((JMenu) menuItem).removeMenuListener(menuListener);
        }
        changeListener = null;
        menuListener = null;
    }

    /** Sin atajos propios; la letra subrayada la maneja la barra. */
    protected void installKeyboardActions() {
        super.installKeyboardActions();
    }

    protected void uninstallKeyboardActions() {
        super.uninstallKeyboardActions();
    }

    protected MouseInputListener createMouseInputListener(JComponent c) {
        return super.createMouseInputListener(c);
    }

    protected MenuDragMouseListener createMenuDragMouseListener(JComponent c) {
        return super.createMenuDragMouseListener(c);
    }

    /** Ninguno, como en {@link BasicMenuItemUI}. */
    protected MenuKeyListener createMenuKeyListener(JComponent c) {
        return null;
    }

    protected PropertyChangeListener createPropertyChangeListener(JComponent c) {
        return super.createPropertyChangeListener(c);
    }

    /**
     * Ninguno.
     *
     * <p>Los dos --este y {@link #createMenuListener}-- devuelven {@code null}, y los dos campos
     * quedan en nulo despues de instalar. No es un olvido: quien sigue el estado del menu es el
     * mismo objeto que sigue el mouse, y engancharlo dos veces lo haria reaccionar dos veces. Esta
     * medido.
     */
    protected ChangeListener createChangeListener(JComponent c) {
        return null;
    }

    /** Ninguno; ver {@link #createChangeListener}. */
    protected MenuListener createMenuListener(JComponent c) {
        return null;
    }

    /** Infinito solo para el menu de barra; ver la nota de la clase. */
    public Dimension getMaximumSize(JComponent c) {
        if (((JMenu) menuItem).isTopLevelMenu()) {
            Dimension d = c.getPreferredSize();
            return new Dimension(d.width, Short.MAX_VALUE);
        }
        return null;
    }

    /** {@code null}, como en cualquier item. */
    public Dimension getMinimumSize(JComponent c) {
        return null;
    }

    /**
     * Arma el reloj que abre el submenu; ver la nota de la clase.
     *
     * <p>Un solo disparo: el reloj abre el menu y se apaga.
     */
    protected void setupPostTimer(JMenu menu) {
        Timer timer = new Timer(menu.getDelay(), new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                MenuSelectionManager.defaultManager().setSelectedPath(getPath());
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
}
