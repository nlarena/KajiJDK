package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * Un {@link BevelBorder} con las esquinas suavizadas.
 *
 * <p>La unica diferencia es que las esquinas no se cierran: las lineas se cortan un pixel antes, y
 * el relieve queda con las puntas redondeadas en vez de en angulo recto. El efecto es sutil y el
 * motivo es de estilo, no funcional.
 *
 * <p>Tiene una consecuencia real igual: <strong>deja de ser opaco</strong>. Los pixeles de las
 * esquinas quedan sin pintar, y prometer lo contrario dejaria basura justo ahi — el mismo argumento
 * que en {@link LineBorder} con las esquinas redondeadas.
 */
public class SoftBevelBorder extends BevelBorder {

    private static final long serialVersionUID = 5248789787305979975L;

    /** Con los colores derivados del fondo del componente. */
    public SoftBevelBorder(int bevelType) {
        super(bevelType);
    }

    /** Con un color claro y uno oscuro. */
    public SoftBevelBorder(int bevelType, Color highlight, Color shadow) {
        super(bevelType, highlight, shadow);
    }

    /** Con los cuatro tonos explicitos. */
    public SoftBevelBorder(int bevelType, Color highlightOuterColor, Color highlightInnerColor,
            Color shadowOuterColor, Color shadowInnerColor) {
        super(bevelType, highlightOuterColor, highlightInnerColor, shadowOuterColor,
                shadowInnerColor);
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color viejo = g.getColor();
        g.translate(x, y);

        boolean levantado = getBevelType() == RAISED;
        Color arribaAfuera = levantado ? getHighlightOuterColor(c) : getShadowInnerColor(c);
        Color arribaAdentro = levantado ? getHighlightInnerColor(c) : getShadowOuterColor(c);
        Color abajoAfuera = levantado ? getShadowOuterColor(c) : getHighlightInnerColor(c);
        Color abajoAdentro = levantado ? getShadowInnerColor(c) : getHighlightOuterColor(c);

        // Las lineas arrancan y terminan un pixel adentro respecto de `BevelBorder`: eso es todo
        // el suavizado, y es tambien por que las esquinas quedan sin pintar.
        g.setColor(arribaAfuera);
        g.drawLine(0, 0, width - 2, 0);
        g.drawLine(0, 1, 0, height - 2);

        g.setColor(arribaAdentro);
        g.drawLine(1, 1, width - 3, 1);
        g.drawLine(1, 2, 1, height - 3);

        g.setColor(abajoAfuera);
        g.drawLine(1, height - 1, width - 1, height - 1);
        g.drawLine(width - 1, 1, width - 1, height - 2);

        g.setColor(abajoAdentro);
        g.drawLine(2, height - 2, width - 2, height - 2);
        g.drawLine(width - 2, 2, width - 2, height - 3);

        g.translate(-x, -y);
        g.setColor(viejo);
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = 3;
        insets.top = 3;
        insets.right = 3;
        insets.bottom = 3;
        return insets;
    }

    /** No es opaco: las esquinas quedan sin pintar. Ver la nota de la clase. */
    public boolean isBorderOpaque() {
        return false;
    }
}
