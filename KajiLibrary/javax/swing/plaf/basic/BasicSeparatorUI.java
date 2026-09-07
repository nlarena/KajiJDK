package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SeparatorUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de un separador: dos lineas de un pixel, una encima de la otra.
 *
 * <p>La de arriba con el color de frente y la de abajo con el de fondo. Ese par es lo que da la
 * ilusion de hendidura: una linea oscura y una clara pegadas se leen como un surco. Los colores
 * salen de {@code Separator.foreground} y {@code Separator.background}, medidos en Metal (JDK 25):
 * (99, 130, 191) y blanco.
 *
 * <h2>Dos campos que no se usan</h2>
 *
 * <p>{@link #shadow} y {@link #highlight} son protegidos, existen, y quedan en {@code null}: nadie
 * los escribe y {@link #paint} no los mira, porque pinta con el frente y el fondo del componente.
 * Son de una version anterior del JDK y quedaron por compatibilidad. Esta medido, y se copia tal
 * cual: una subclase que los lea tiene que ver {@code null} igual que en el JDK.
 *
 * <h2>Un objeto por separador</h2>
 *
 * <p>A diferencia de casi todos los demas, {@link #createUI} devuelve una instancia nueva cada vez.
 * Tampoco guarda nada, asi que no hace falta; es asi en el JDK y esta medido.
 *
 * <h2>Sin minimo</h2>
 *
 * <p>{@link #getMinimumSize} devuelve {@code null}, no un tamano. Es lo que hace el JDK, y quien
 * llama tiene que estar preparado: {@code JComponent.getMinimumSize} lo lee como "no tengo opinion"
 * y contesta lo que diga el acomodador.
 */
public class BasicSeparatorUI extends SeparatorUI {

    /** Sin uso; ver la nota de la clase. */
    protected Color shadow;

    /** Sin uso; ver la nota de la clase. */
    protected Color highlight;

    private static final ColorUIResource FONDO_POR_OMISION = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource FRENTE_POR_OMISION = new ColorUIResource(99, 130, 191);

    public BasicSeparatorUI() {
    }

    /** Uno nuevo cada vez; ver la nota de la clase. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicSeparatorUI();
    }

    public void installUI(JComponent c) {
        installDefaults((JSeparator) c);
        installListeners((JSeparator) c);
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults((JSeparator) c);
        uninstallListeners((JSeparator) c);
    }

    /** Los dos colores y la transparencia: un separador no es opaco. */
    protected void installDefaults(JSeparator s) {
        Color fondo = s.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            s.setBackground(FONDO_POR_OMISION);
        }
        Color frente = s.getForeground();
        if (frente == null || frente instanceof UIResource) {
            s.setForeground(FRENTE_POR_OMISION);
        }
        LookAndFeel.installProperty(s, "opaque", Boolean.FALSE);
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults(JSeparator s) {
    }

    /** No escucha nada: un separador no cambia solo. */
    protected void installListeners(JSeparator s) {
    }

    /** Idem. */
    protected void uninstallListeners(JSeparator s) {
    }

    /** Las dos lineas; ver la nota de la clase. */
    public void paint(Graphics g, JComponent c) {
        Dimension s = c.getSize();
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            g.setColor(c.getForeground());
            g.drawLine(0, 0, 0, s.height);
            g.setColor(c.getBackground());
            g.drawLine(1, 0, 1, s.height);
        } else {
            g.setColor(c.getForeground());
            g.drawLine(0, 0, s.width, 0);
            g.setColor(c.getBackground());
            g.drawLine(0, 1, s.width, 1);
        }
    }

    /** Dos pixeles de grueso y nada de largo: lo estira el acomodador. */
    public Dimension getPreferredSize(JComponent c) {
        Insets insets = c.getInsets();
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(2 + insets.left + insets.right, insets.top + insets.bottom);
        }
        return new Dimension(insets.left + insets.right, 2 + insets.top + insets.bottom);
    }

    /** {@code null}; ver la nota de la clase. */
    public Dimension getMinimumSize(JComponent c) {
        return null;
    }

    /** Todo lo largo que haga falta, dos pixeles de grueso. */
    public Dimension getMaximumSize(JComponent c) {
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(2, Short.MAX_VALUE);
        }
        return new Dimension(Short.MAX_VALUE, 2);
    }
}
