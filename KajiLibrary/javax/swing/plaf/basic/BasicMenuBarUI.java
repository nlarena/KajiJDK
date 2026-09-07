package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.LookAndFeel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.MenuBarUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de una barra de menu.
 *
 * <h2>Lo unico propio es el acomodador</h2>
 *
 * <p>Una barra de menu no dibuja nada: lo que se ve son sus menus. Lo que si hace es ponerle
 * {@link DefaultMenuLayout} en el eje horizontal, y eso no es un detalle -- es lo que hace que los
 * menus queden pegados a la izquierda uno atras del otro y no repartidos, que es lo que haria un
 * {@code FlowLayout}.
 *
 * <h2>Los tres tamanos son {@code null}</h2>
 *
 * <p>Preferido, minimo y maximo: los tres. La barra deja que conteste el acomodador, que es el
 * unico que sabe cuanto miden los menus que tiene adentro. Esta medido, y es distinto de casi todos
 * los demas UI, que al menos contestan el preferido.
 */
public class BasicMenuBarUI extends MenuBarUI {

    protected JMenuBar menuBar = null;
    protected ContainerListener containerListener;
    protected ChangeListener changeListener;

    private static final ColorUIResource FONDO = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FRENTE = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.BOLD, 12);

    public BasicMenuBarUI() {
    }

    /** Uno nuevo por barra: guarda el componente y sus escuchas. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicMenuBarUI();
    }

    public void installUI(JComponent c) {
        menuBar = (JMenuBar) c;
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
        menuBar = null;
    }

    /** Colores, fuente y el acomodador; ver la nota de la clase. */
    protected void installDefaults() {
        if (menuBar.getLayout() == null || menuBar.getLayout() instanceof UIResource) {
            menuBar.setLayout(new DefaultMenuLayout(menuBar, BoxLayout.LINE_AXIS));
        }
        Color fondo = menuBar.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            menuBar.setBackground(FONDO);
        }
        Color frente = menuBar.getForeground();
        if (frente == null || frente instanceof UIResource) {
            menuBar.setForeground(FRENTE);
        }
        Font fuente = menuBar.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            menuBar.setFont(FUENTE);
        }
        if (menuBar.getBorder() == null || menuBar.getBorder() instanceof UIResource) {
            // Dos pixeles abajo y nada mas: la linea que separa la barra del contenido.
            menuBar.setBorder(new BasicBorders.MenuBarBorder(FRENTE, FONDO));
        }
        LookAndFeel.installProperty(menuBar, "opaque", Boolean.TRUE);
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        containerListener = createContainerListener();
        changeListener = createChangeListener();
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            javax.swing.JMenu menu = menuBar.getMenu(i);
            if (menu != null) {
                menu.getModel().addChangeListener(changeListener);
            }
        }
        menuBar.addContainerListener(containerListener);
    }

    protected void uninstallListeners() {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            javax.swing.JMenu menu = menuBar.getMenu(i);
            if (menu != null) {
                menu.getModel().removeChangeListener(changeListener);
            }
        }
        menuBar.removeContainerListener(containerListener);
        containerListener = null;
        changeListener = null;
    }

    /** Sin atajos propios: la letra subrayada de cada menu la maneja el menu. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    /** El que engancha y desengancha el escucha de cambio de los menus que entran y salen. */
    protected ContainerListener createContainerListener() {
        return new EscuchaDeContenedor();
    }

    protected ChangeListener createChangeListener() {
        return new EscuchaDeCambio();
    }

    /** {@code null}; ver la nota de la clase. */
    public Dimension getMinimumSize(JComponent c) {
        return null;
    }

    /** {@code null}; ver la nota de la clase. */
    public Dimension getMaximumSize(JComponent c) {
        return null;
    }

    private class EscuchaDeContenedor implements ContainerListener {

        public void componentAdded(ContainerEvent e) {
            java.awt.Component c = e.getChild();
            if (c instanceof javax.swing.JMenu) {
                ((javax.swing.JMenu) c).getModel().addChangeListener(changeListener);
            }
        }

        public void componentRemoved(ContainerEvent e) {
            java.awt.Component c = e.getChild();
            if (c instanceof javax.swing.JMenu) {
                ((javax.swing.JMenu) c).getModel().removeChangeListener(changeListener);
            }
        }
    }

    private class EscuchaDeCambio implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            if (menuBar != null) {
                menuBar.repaint();
            }
        }
    }
}
