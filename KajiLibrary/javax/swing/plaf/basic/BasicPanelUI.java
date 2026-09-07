package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.PanelUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de un panel: colores, fuente, y nada mas.
 *
 * <p>Un panel no dibuja nada propio -- {@code ComponentUI.update} le rellena el fondo si es opaco y
 * los hijos se pintan solos --, asi que este UI no tiene {@code paint}. Lo unico que hace es poner
 * los valores que en el JDK vienen de {@code UIManager} bajo {@code Panel.*}, medidos en Metal
 * (JDK 25): fondo (238, 238, 238), frente (51, 51, 51), Dialog 12, y el panel opaco.
 *
 * <p>Un solo objeto para todos los paneles: {@link #createUI} devuelve siempre el mismo, y puede
 * porque no guarda nada de ninguno.
 *
 * <h2>Linea de base</h2>
 *
 * <p>Un panel no tiene texto, asi que no tiene linea de base: {@link #getBaseline} devuelve -1 y el
 * comportamiento al cambiar de tamano es {@code OTHER}. Igual valida los argumentos --tamano
 * negativo tira, componente nulo revienta--, que es lo que hace {@link ComponentUI}.
 */
public class BasicPanelUI extends PanelUI {

    private static PanelUI panelUI = new BasicPanelUI();

    private static final ColorUIResource FONDO_POR_OMISION = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FRENTE_POR_OMISION = new ColorUIResource(51, 51, 51);
    private static final Font FUENTE_POR_OMISION = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicPanelUI() {
    }

    /** El aspecto compartido; ver la nota de la clase. */
    public static ComponentUI createUI(JComponent c) {
        return panelUI;
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        installDefaults((JPanel) c);
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults((JPanel) c);
        super.uninstallUI(c);
    }

    /**
     * Colores, fuente y opacidad, solo donde el usuario no puso los suyos; ver {@link UIResource}.
     */
    protected void installDefaults(JPanel p) {
        Color fondo = p.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            p.setBackground(FONDO_POR_OMISION);
        }
        Color frente = p.getForeground();
        if (frente == null || frente instanceof UIResource) {
            p.setForeground(FRENTE_POR_OMISION);
        }
        Font fuente = p.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            p.setFont(FUENTE_POR_OMISION);
        }
        LookAndFeel.installProperty(p, "opaque", Boolean.TRUE);
    }

    /** No saca nada: lo instalado es {@link UIResource} y lo pisa el aspecto que venga. */
    protected void uninstallDefaults(JPanel p) {
    }

    /**
     * -1: un panel no tiene texto y por lo tanto no tiene linea de base.
     *
     * @throws NullPointerException si el componente es nulo
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        return -1;
    }

    /**
     * {@code OTHER}: sin linea de base no hay nada que se mueva con ella.
     *
     * @throws NullPointerException si el componente es nulo
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.OTHER;
    }
}
