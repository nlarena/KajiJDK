package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.ViewportUI;

/**
 * El aspecto basico de una ventana de desplazamiento: le pone colores y fuente, y nada mas.
 *
 * <p>No pinta: {@code ComponentUI.update} rellena el fondo si la ventana es opaca —lo es desde su
 * constructor— y el contenido lo pinta la vista. Los valores son los de {@code Viewport.*} medidos
 * en Metal (JDK 25): fondo (238, 238, 238), frente (51, 51, 51) y Dialog 12.
 */
public class BasicViewportUI extends ViewportUI {

    private static ViewportUI viewportUI = new BasicViewportUI();

    private static final ColorUIResource FONDO_POR_OMISION = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FRENTE_POR_OMISION = new ColorUIResource(51, 51, 51);
    private static final Font FUENTE_POR_OMISION = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicViewportUI() {
    }

    /** El aspecto compartido: no guarda nada de la ventana. */
    public static ComponentUI createUI(JComponent c) {
        return viewportUI;
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        installDefaults(c);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        uninstallDefaults(c);
    }

    /** Colores y fuente, solo donde el usuario no puso los suyos; ver {@link UIResource}. */
    protected void installDefaults(JComponent c) {
        Color fondo = c.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            c.setBackground(FONDO_POR_OMISION);
        }
        Color frente = c.getForeground();
        if (frente == null || frente instanceof UIResource) {
            c.setForeground(FRENTE_POR_OMISION);
        }
        Font fuente = c.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            c.setFont(FUENTE_POR_OMISION);
        }
    }

    /** Lo instalado queda en el componente; el JDK tampoco lo borra. */
    protected void uninstallDefaults(JComponent c) {
    }
}
