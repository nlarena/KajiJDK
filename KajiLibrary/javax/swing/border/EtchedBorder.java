package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * Una linea grabada: dos pixeles, uno claro y uno oscuro, que fingen un surco o una cresta.
 *
 * <p>La misma ilusion de luz que {@link BevelBorder} pero con dos colores en vez de cuatro, y con
 * otro proposito: un bisel hace que el <em>componente</em> parezca levantado, y esto hace que
 * parezca que hay una <em>linea tallada</em> alrededor. Se usa para agrupar, no para sugerir que
 * algo se puede apretar.
 *
 * <p>Grabado hacia adentro y hacia afuera son —otra vez— el mismo dibujo con los dos colores
 * intercambiados.
 */
public class EtchedBorder extends AbstractBorder {

    private static final long serialVersionUID = 4001244046866360638L;

    /** La linea parece sobresalir. */
    public static final int RAISED = 0;
    /** La linea parece un surco. */
    public static final int LOWERED = 1;

    protected int etchType;
    protected Color highlight;
    protected Color shadow;

    /** Grabado hacia adentro, con los colores derivados del fondo. */
    public EtchedBorder() {
        this(LOWERED);
    }

    /** Del tipo dado, con los colores derivados del fondo. */
    public EtchedBorder(int etchType) {
        this.etchType = etchType;
    }

    /** Grabado hacia adentro, con los dos colores dados. */
    public EtchedBorder(Color highlight, Color shadow) {
        this(LOWERED, highlight, shadow);
    }

    /** Del tipo y los colores dados. */
    public EtchedBorder(int etchType, Color highlight, Color shadow) {
        this(etchType);
        this.highlight = highlight;
        this.shadow = shadow;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        int w = width;
        int h = height;
        Color viejo = g.getColor();

        g.translate(x, y);

        // El rectangulo de afuera lleva el color de "sombra" cuando esta hundido y el de "brillo"
        // cuando sobresale; el de adentro, al reves. Ese intercambio es toda la diferencia entre
        // los dos tipos.
        if (this.etchType == LOWERED) {
            g.setColor(getShadowColor(c));
        } else {
            g.setColor(getHighlightColor(c));
        }
        g.drawRect(0, 0, w - 2, h - 2);

        if (this.etchType == LOWERED) {
            g.setColor(getHighlightColor(c));
        } else {
            g.setColor(getShadowColor(c));
        }
        g.drawLine(1, h - 3, 1, 1);
        g.drawLine(1, 1, w - 3, 1);
        g.drawLine(0, h - 1, w - 1, h - 1);
        g.drawLine(w - 1, h - 1, w - 1, 0);

        g.translate(-x, -y);
        g.setColor(viejo);
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = 2;
        insets.top = 2;
        insets.right = 2;
        insets.bottom = 2;
        return insets;
    }

    /** Opaco: los dos pixeles de cada lado se pintan enteros. */
    public boolean isBorderOpaque() {
        return true;
    }

    /** {@link #RAISED} o {@link #LOWERED}. */
    public int getEtchType() {
        return this.etchType;
    }

    /** El color claro; derivado del fondo de {@code c} si no se fijo uno. */
    public Color getHighlightColor(Component c) {
        if (this.highlight != null) {
            return this.highlight;
        }
        return c.getBackground().brighter();
    }

    /** El color claro que se fijo, o {@code null}. */
    public Color getHighlightColor() {
        return this.highlight;
    }

    /** El color oscuro; derivado del fondo de {@code c} si no se fijo uno. */
    public Color getShadowColor(Component c) {
        if (this.shadow != null) {
            return this.shadow;
        }
        return c.getBackground().darker();
    }

    /** El color oscuro que se fijo, o {@code null}. */
    public Color getShadowColor() {
        return this.shadow;
    }
}
