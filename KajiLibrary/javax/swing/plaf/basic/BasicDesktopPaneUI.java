package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultDesktopManager;
import javax.swing.DesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DesktopPaneUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de un escritorio de ventanas internas.
 *
 * <h2>No dibuja nada, y no es poco lo que hace</h2>
 *
 * <p>Lo que se ve de un escritorio son sus ventanas internas; el escritorio mismo es un fondo liso.
 * Lo que este UI aporta son dos cosas que no se ven: el {@link DesktopManager} --quien decide que
 * pasa al mover, maximizar, iconizar o cerrar una ventana-- y la tabla de acciones de teclado, que
 * son diecinueve.
 *
 * <h2>Diecinueve acciones y ninguna tecla</h2>
 *
 * <p>Las acciones estan todas --moverse entre ventanas, mover y redimensionar con las flechas,
 * cerrar, minimizar, maximizar, restaurar-- y el mapa de teclas esta <em>vacio</em>. No es un
 * olvido: que tecla dispara cual accion lo decide la tabla del aspecto, y sin tabla no hay ninguna
 * atada. Esta medido: el JDK tampoco tiene ninguna clave en el mapa del escritorio.
 *
 * <h2>Los cinco campos de tecla que quedaron en nulo</h2>
 *
 * <p>{@link #closeKey}, {@link #navigateKey}, {@link #navigateKey2}, {@link #minimizeKey} y
 * {@link #maximizeKey} son protegidos, existen, y nadie los escribe. Vienen de cuando el UI ataba
 * las teclas a mano; ahora las ata la tabla y quedaron por compatibilidad, igual que
 * {@code shadow} y {@code highlight} en {@link BasicSeparatorUI}. Medido: nulos los cinco.
 *
 * <h2>Sin tamano preferido</h2>
 *
 * <p>{@link #getPreferredSize} devuelve {@code null}. Un escritorio ocupa lo que le den: no hay un
 * tamano que "prefiera", y contestar la union de sus ventanas seria peor -- el escritorio crece
 * cada vez que alguien arrastra una ventana hacia el borde.
 */
public class BasicDesktopPaneUI extends DesktopPaneUI {

    protected JDesktopPane desktop;
    protected DesktopManager desktopManager;

    /** Sin uso; ver la nota de la clase. */
    @Deprecated
    protected KeyStroke minimizeKey;

    /** Sin uso; ver la nota de la clase. */
    @Deprecated
    protected KeyStroke maximizeKey;

    /** Sin uso; ver la nota de la clase. */
    @Deprecated
    protected KeyStroke closeKey;

    /** Sin uso; ver la nota de la clase. */
    @Deprecated
    protected KeyStroke navigateKey;

    /** Sin uso; ver la nota de la clase. */
    @Deprecated
    protected KeyStroke navigateKey2;

    private PropertyChangeListener pcl;

    private static final ColorUIResource FONDO = new ColorUIResource(255, 255, 255);

    public BasicDesktopPaneUI() {
    }

    /** Uno nuevo por escritorio: guarda el componente y su administrador. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicDesktopPaneUI();
    }

    public void installUI(JComponent c) {
        desktop = (JDesktopPane) c;
        installDefaults();
        installDesktopManager();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallDesktopManager();
        uninstallDefaults();
        desktop = null;
    }

    /** Fondo y opacidad; el valor es el de {@code Desktop.background} en Metal. */
    protected void installDefaults() {
        Color fondo = desktop.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            desktop.setBackground(FONDO);
        }
        LookAndFeel.installProperty(desktop, "opaque", Boolean.TRUE);
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    /** Pone el administrador, salvo que el programa haya puesto el suyo. */
    protected void installDesktopManager() {
        desktopManager = desktop.getDesktopManager();
        if (desktopManager == null) {
            desktopManager = new BasicDesktopManager();
            desktop.setDesktopManager(desktopManager);
        }
    }

    /** Y lo saca solo si es el que puso este UI. */
    protected void uninstallDesktopManager() {
        if (desktop.getDesktopManager() instanceof UIResource) {
            desktop.setDesktopManager(null);
        }
        desktopManager = null;
    }

    protected void installListeners() {
        pcl = createPropertyChangeListener();
        desktop.addPropertyChangeListener(pcl);
    }

    protected void uninstallListeners() {
        desktop.removePropertyChangeListener(pcl);
        pcl = null;
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler();
    }

    /** Las diecinueve acciones; ver la nota de la clase. */
    protected void installKeyboardActions() {
        registerKeyboardActions();
        SwingUtilities.replaceUIActionMap(desktop, crearMapaDeAcciones());
    }

    protected void uninstallKeyboardActions() {
        unregisterKeyboardActions();
        SwingUtilities.replaceUIActionMap(desktop, null);
    }

    /** El gancho para que una subclase ate teclas propias; el basico no ata ninguna. */
    protected void registerKeyboardActions() {
    }

    protected void unregisterKeyboardActions() {
    }

    private ActionMap crearMapaDeAcciones() {
        ActionMap map = new ActionMapUIResource();
        String[] nombres = {
            "restore", "close", "move", "resize",
            "right", "shrinkRight", "left", "shrinkLeft",
            "up", "shrinkUp", "down", "shrinkDown",
            "escape", "minimize", "maximize",
            "selectNextFrame", "selectPreviousFrame",
            "navigateNext", "navigatePrevious",
        };
        for (int i = 0; i < nombres.length; i++) {
            map.put(nombres[i], new AccionDeEscritorio(nombres[i]));
        }
        return map;
    }

    /** Nada: el fondo lo rellena {@code ComponentUI.update} y las ventanas se pintan solas. */
    public void paint(Graphics g, JComponent c) {
    }

    /** {@code null}; ver la nota de la clase. */
    public Dimension getPreferredSize(JComponent c) {
        return null;
    }

    /** Cero: un escritorio se puede achicar hasta desaparecer. */
    public Dimension getMinimumSize(JComponent c) {
        return new Dimension(0, 0);
    }

    /** Sin tope. */
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * El administrador que pone este UI.
     *
     * <p>Es un {@link DefaultDesktopManager} marcado como del aspecto; la marca es lo unico que
     * agrega, y es lo que deja que {@link #uninstallDesktopManager} sepa si lo puso el o el
     * programa.
     */
    private static class BasicDesktopManager extends DefaultDesktopManager implements UIResource {
    }

    /** Cada accion del teclado; el nombre dice cual. */
    private class AccionDeEscritorio extends AbstractAction {

        private final String que;

        AccionDeEscritorio(String que) {
            super(que);
            this.que = que;
        }

        public void actionPerformed(ActionEvent e) {
            JInternalFrame f = desktop.getSelectedFrame();
            if ("selectNextFrame".equals(que) || "navigateNext".equals(que)) {
                mover(1);
            } else if ("selectPreviousFrame".equals(que) || "navigatePrevious".equals(que)) {
                mover(-1);
            } else if (f == null) {
                return;
            } else if ("close".equals(que)) {
                if (f.isClosable()) {
                    f.doDefaultCloseAction();
                }
            } else if ("minimize".equals(que)) {
                if (f.isIconifiable() && !f.isIcon()) {
                    intentar(f, "icon", true);
                }
            } else if ("maximize".equals(que)) {
                if (f.isMaximizable() && !f.isMaximum()) {
                    intentar(f, "maximum", true);
                }
            } else if ("restore".equals(que)) {
                if (f.isIcon()) {
                    intentar(f, "icon", false);
                } else if (f.isMaximum()) {
                    intentar(f, "maximum", false);
                }
            } else {
                // move, resize, escape y las eeis de flecha son gestos que necesitan teclado y
                // pantalla: sin ninguno de los dos no hay nada que hacer. Ver la nota de la clase.
                return;
            }
        }

        /** Pasa a la ventana siguiente o a la anterior, en el orden en que estan. */
        private void mover(int paso) {
            JInternalFrame[] marcos = desktop.getAllFrames();
            if (marcos.length == 0) {
                return;
            }
            JInternalFrame actual = desktop.getSelectedFrame();
            int i = 0;
            for (int k = 0; k < marcos.length; k++) {
                if (marcos[k] == actual) {
                    i = k;
                    break;
                }
            }
            int siguiente = ((i + paso) % marcos.length + marcos.length) % marcos.length;
            intentar(marcos[siguiente], "selected", true);
        }

        /** Los cambios de estado de una ventana interna pueden ser vetados. */
        private void intentar(JInternalFrame f, String propiedad, boolean valor) {
            try {
                if ("icon".equals(propiedad)) {
                    f.setIcon(valor);
                } else if ("maximum".equals(propiedad)) {
                    f.setMaximum(valor);
                } else {
                    f.setSelected(valor);
                }
            } catch (java.beans.PropertyVetoException ex) {
                // Alguien dijo que no. Es una respuesta valida, no un error.
            }
        }
    }

    /** Rearma el administrador cuando el escritorio cambia de aspecto. */
    private class Handler implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent e) {
            if ("desktopManager".equals(e.getPropertyName())) {
                desktopManager = desktop.getDesktopManager();
            }
        }
    }
}
