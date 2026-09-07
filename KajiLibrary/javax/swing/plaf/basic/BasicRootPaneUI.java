package javax.swing.plaf.basic;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ComponentInputMapUIResource;
import javax.swing.plaf.RootPaneUI;

/**
 * El aspecto basico de un panel raiz: no dibuja nada, y ata el Enter al boton por omision.
 *
 * <h2>Lo unico que hace es teclado</h2>
 *
 * <p>Un panel raiz no tiene aspecto: es la capa que sostiene al menu, al contenido y al cristal, y
 * lo que se ve son sus hijos. Asi que este UI no instala ni un color. Lo que si instala es el
 * atajo que hace que Enter apriete el boton por omision de la ventana --el que esta marcado con un
 * borde distinto en un dialogo--, que es la unica cosa que el panel raiz tiene que saber hacer.
 *
 * <h2>El mapa vacio que no es inutil</h2>
 *
 * <p>El mapa de teclas se instala <em>siempre</em>, pero arranca vacio: las cuatro combinaciones
 * --Enter y ctrl-Enter, apretar y soltar-- se agregan cuando la ventana tiene boton por omision y
 * se sacan cuando deja de tenerlo. {@link #propertyChange} es quien lo nota. Esta medido: un panel
 * raiz recien creado tiene el mapa puesto y sin ninguna clave.
 *
 * <p>El mapa es de la clase {@code RootPaneInputMap}, que no aporta nada salvo ser reconocible:
 * {@link #updateDefaultButtonBindings} sube por la cadena de padres hasta encontrarla, y asi sabe
 * cual de todos los mapas es el suyo y cual puso el usuario.
 */
public class BasicRootPaneUI extends RootPaneUI implements PropertyChangeListener {

    private static RootPaneUI rootPaneUI = new BasicRootPaneUI();

    /** Las cuatro combinaciones; ver la nota de la clase. */
    private static final Object[] ATAJOS_DEL_BOTON_POR_OMISION = {
        "ENTER", "press",
        "released ENTER", "release",
        "ctrl ENTER", "press",
        "ctrl released ENTER", "release",
    };

    public BasicRootPaneUI() {
    }

    /** El aspecto compartido: no guarda nada del panel. */
    public static ComponentUI createUI(JComponent c) {
        return rootPaneUI;
    }

    public void installUI(JComponent c) {
        installDefaults((JRootPane) c);
        installComponents((JRootPane) c);
        installListeners((JRootPane) c);
        installKeyboardActions((JRootPane) c);
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults((JRootPane) c);
        uninstallComponents((JRootPane) c);
        uninstallListeners((JRootPane) c);
        uninstallKeyboardActions((JRootPane) c);
    }

    /** Nada: ver la nota de la clase. */
    protected void installDefaults(JRootPane c) {
    }

    /** Nada. */
    protected void uninstallDefaults(JRootPane c) {
    }

    /** Nada: los hijos los pone {@link JRootPane} en su constructor. */
    protected void installComponents(JRootPane root) {
    }

    /** Nada. */
    protected void uninstallComponents(JRootPane root) {
    }

    /** Escucha el cambio de boton por omision y el de ancestro. */
    protected void installListeners(JRootPane root) {
        root.addPropertyChangeListener(this);
    }

    protected void uninstallListeners(JRootPane root) {
        root.removePropertyChangeListener(this);
    }

    /** El mapa de teclas y las tres acciones; ver la nota de la clase. */
    protected void installKeyboardActions(JRootPane root) {
        InputMap km = new RootPaneInputMap(root);
        SwingUtilities.replaceUIInputMap(root, JComponent.WHEN_IN_FOCUSED_WINDOW, km);
        ActionMap am = crearMapaDeAcciones();
        SwingUtilities.replaceUIActionMap(root, am);
        updateDefaultButtonBindings(root);
    }

    protected void uninstallKeyboardActions(JRootPane root) {
        SwingUtilities.replaceUIInputMap(root, JComponent.WHEN_IN_FOCUSED_WINDOW, null);
        SwingUtilities.replaceUIActionMap(root, null);
    }

    private ActionMap crearMapaDeAcciones() {
        ActionMap map = new ActionMapUIResource();
        map.put("press", new AccionDelBotonPorOmision(true));
        map.put("release", new AccionDelBotonPorOmision(false));
        map.put("postPopup", new AccionDeMenuContextual());
        return map;
    }

    /**
     * Pone o saca las cuatro combinaciones segun haya boton por omision.
     *
     * <p>Sube por la cadena de padres hasta el mapa propio; ver la nota de la clase.
     */
    private void updateDefaultButtonBindings(JRootPane root) {
        InputMap km = SwingUtilities.getUIInputMap(root, JComponent.WHEN_IN_FOCUSED_WINDOW);
        while (km != null && !(km instanceof RootPaneInputMap)) {
            km = km.getParent();
        }
        if (km != null) {
            km.clear();
            if (root.getDefaultButton() != null) {
                LookAndFeel.loadKeyBindings(km, ATAJOS_DEL_BOTON_POR_OMISION);
            }
        }
    }

    /** Rearma los atajos cuando cambia el boton por omision. */
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("defaultButton")) {
            JRootPane rootpane = (JRootPane) e.getSource();
            updateDefaultButtonBindings(rootpane);
            if (rootpane.getClientProperty("temporaryDefaultButton") == null) {
                rootpane.repaint();
            }
        }
    }

    /** El mapa reconocible; ver la nota de la clase. */
    static class RootPaneInputMap extends ComponentInputMapUIResource {

        public RootPaneInputMap(JComponent c) {
            super(c);
        }
    }

    /**
     * Aprieta o suelta el boton por omision.
     *
     * <p>Aprieta el modelo en vez de llamar a {@code doClick}: asi el boton se ve hundido mientras
     * la tecla esta abajo, que es lo que hace un boton de verdad.
     */
    private static class AccionDelBotonPorOmision extends AbstractAction {

        private final boolean apretar;

        AccionDelBotonPorOmision(boolean apretar) {
            this.apretar = apretar;
        }

        public void actionPerformed(ActionEvent e) {
            JRootPane root = (JRootPane) e.getSource();
            JButton owner = root.getDefaultButton();
            if (owner != null && SwingUtilities.getRootPane(owner) == root) {
                ButtonModel model = owner.getModel();
                if (apretar) {
                    model.setArmed(true);
                    model.setPressed(true);
                } else {
                    model.setPressed(false);
                    model.setArmed(false);
                }
            }
        }

        public boolean isEnabled() {
            return true;
        }
    }

    /** Muestra el menu contextual del componente que tiene el foco, si tiene uno. */
    private static class AccionDeMenuContextual extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            JRootPane root = (JRootPane) e.getSource();
            java.awt.Component foco = java.awt.KeyboardFocusManager
                    .getCurrentKeyboardFocusManager().getFocusOwner();
            if (!(foco instanceof JComponent)) {
                return;
            }
            JPopupMenu menu = ((JComponent) foco).getComponentPopupMenu();
            if (menu != null) {
                java.awt.Point p = ((JComponent) foco).getPopupLocation(null);
                if (p == null) {
                    p = new java.awt.Point(foco.getWidth() / 2, foco.getHeight() / 2);
                }
                menu.show(foco, p.x, p.y);
            }
        }
    }
}
