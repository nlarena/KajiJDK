package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * Una linea del mismo grosor en los cuatro lados.
 *
 * <p>El borde mas simple que dibuja algo. Los dos {@code create*} devuelven instancias
 * <strong>compartidas</strong>: un borde no guarda nada del componente que lo usa, asi que el mismo
 * objeto sirve para todos los que quieran una linea negra de un pixel.
 */
public class LineBorder extends AbstractBorder {

    private static final long serialVersionUID = -787563427772288970L;

    private static Border lineaNegra;
    private static Border lineaGris;

    protected int thickness;
    protected Color lineColor;
    protected boolean roundedCorners;

    /** Una linea negra de un pixel, compartida. */
    public static Border createBlackLineBorder() {
        if (lineaNegra == null) {
            lineaNegra = new LineBorder(Color.black, 1);
        }
        return lineaNegra;
    }

    /** Una linea gris de un pixel, compartida. */
    public static Border createGrayLineBorder() {
        if (lineaGris == null) {
            lineaGris = new LineBorder(Color.gray, 1);
        }
        return lineaGris;
    }

    /** Una linea de un pixel del color dado. */
    public LineBorder(Color color) {
        this(color, 1, false);
    }

    /** Una linea del color y grosor dados. */
    public LineBorder(Color color, int thickness) {
        this(color, thickness, false);
    }

    /** Igual, eligiendo si las esquinas van redondeadas. */
    public LineBorder(Color color, int thickness, boolean roundedCorners) {
        this.lineColor = color;
        this.thickness = thickness;
        this.roundedCorners = roundedCorners;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if (this.thickness <= 0) {
            return;
        }
        Color viejo = g.getColor();
        g.setColor(this.lineColor);
        // Un rectangulo por cada pixel de grosor, encogiendo hacia adentro. El menos uno es porque
        // drawRect dibuja inclusive: un rectangulo de ancho w ocupa desde x hasta x+w.
        for (int i = 0; i < this.thickness; i++) {
            if (this.roundedCorners) {
                g.drawRoundRect(x + i, y + i, width - i - i - 1, height - i - i - 1,
                        this.thickness, this.thickness);
            } else {
                g.drawRect(x + i, y + i, width - i - i - 1, height - i - i - 1);
            }
        }
        g.setColor(viejo);
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = this.thickness;
        insets.top = this.thickness;
        insets.right = this.thickness;
        insets.bottom = this.thickness;
        return insets;
    }

    /** El color de la linea. */
    public Color getLineColor() {
        return this.lineColor;
    }

    /** El grosor en pixeles. */
    public int getThickness() {
        return this.thickness;
    }

    /** Si las esquinas van redondeadas. */
    public boolean getRoundedCorners() {
        return this.roundedCorners;
    }

    /**
     * Opaco solo si las esquinas son rectas.
     *
     * <p>Con esquinas redondeadas quedan cuatro pedacitos sin pintar, asi que prometer opacidad
     * dejaria basura justo ahi. Es el ejemplo mas claro de para que sirve ese metodo.
     */
    public boolean isBorderOpaque() {
        return !this.roundedCorners;
    }
}
