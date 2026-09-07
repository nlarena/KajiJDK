package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.PopupMenuUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de un menu desplegable.
 *
 * <p>Como la barra, no dibuja nada propio: pone el acomodador --{@link DefaultMenuLayout} en el eje
 * vertical, para que los items queden uno debajo del otro-- y los colores, y se corre.
 *
 * <h2>Que cuenta como "boton derecho"</h2>
 *
 * <p>{@link #isPopupTrigger} le pregunta al evento y nada mas. Cual boton y cual momento --apretar
 * o soltar-- abre un menu contextual lo decide el sistema operativo, no Swing: en Windows es soltar
 * el boton derecho y en X11 es apretarlo. Contestar que si a cualquier boton derecho romperia esa
 * diferencia y el menu se abriria dos veces en uno de los dos sistemas.
 *
 * <p>{@link #installDefaults} es publico aca y protegido en casi todos los demas UI. Es una
 * inconsistencia del JDK, y se copia: una subclase de otro paquete podria estar llamandolo.
 */
public class BasicPopupMenuUI extends PopupMenuUI {

    protected JPopupMenu popupMenu = null;

    private static final ColorUIResource FONDO = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FRENTE = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.BOLD, 12);

    public BasicPopupMenuUI() {
    }

    /** Uno nuevo por menu: guarda el componente. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicPopupMenuUI();
    }

    public void installUI(JComponent c) {
        popupMenu = (JPopupMenu) c;
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
        popupMenu = null;
    }

    /** Colores, fuente y el acomodador vertical; ver la nota de la clase. */
    public void installDefaults() {
        if (popupMenu.getLayout() == null || popupMenu.getLayout() instanceof UIResource) {
            popupMenu.setLayout(new DefaultMenuLayout(popupMenu, BoxLayout.Y_AXIS));
        }
        LookAndFeel.installProperty(popupMenu, "opaque", Boolean.TRUE);
        Color fondo = popupMenu.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            popupMenu.setBackground(FONDO);
        }
        Color frente = popupMenu.getForeground();
        if (frente == null || frente instanceof UIResource) {
            popupMenu.setForeground(FRENTE);
        }
        Font fuente = popupMenu.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            popupMenu.setFont(FUENTE);
        }
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    /** No escucha nada: quien abre y cierra el menu es {@code MenuSelectionManager}. */
    protected void installListeners() {
    }

    protected void uninstallListeners() {
    }

    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    /** Lo que diga el evento; ver la nota de la clase. */
    public boolean isPopupTrigger(MouseEvent e) {
        return e.isPopupTrigger();
    }
}
