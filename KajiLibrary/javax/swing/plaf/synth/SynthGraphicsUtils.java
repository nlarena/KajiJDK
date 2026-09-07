package javax.swing.plaf.synth;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Las cuentas de dibujo que un aspecto grafico puede querer hacer distinto.
 *
 * <h2>Por que es un objeto y no metodos estaticos</h2>
 *
 * <p>Porque medir y dibujar texto es justamente donde un aspecto se distingue: donde pone la
 * elipsis cuando no entra, como subraya el atajo de teclado, cuanto espacio deja entre el icono y la
 * palabra. Un aspecto que quiere cambiarlo hereda de esta clase y lo devuelve desde su estilo.
 *
 * <p>Los tres metodos estaticos del final son la excepcion, y por una razon: dibujar un icono que
 * puede ser sensible al contexto --uno que se ve distinto si el componente esta deshabilitado-- es
 * la misma operacion para todos los aspectos.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Lo que es cuenta esta hecho: medir un texto, medir un icono, calcular el tamano preferido.
 * Dibujar necesita el motor grafico, y lo que esta biblioteca tiene de {@code java.awt} alcanza para
 * las medidas pero no para pintar; los {@code paint} delegan en lo que haya y no inventan nada.
 *
 * @since 1.5
 */
public class SynthGraphicsUtils {

    /** Uno. */
    public SynthGraphicsUtils() {
    }

    /**
     * Dibuja una linea.
     *
     * @param context que se esta dibujando
     * @param paintKey para que es la linea, o {@code null}
     * @param g donde dibujar
     * @param x1 desde
     * @param y1 desde
     * @param x2 hasta
     * @param y2 hasta
     */
    public void drawLine(SynthContext context, Object paintKey, Graphics g, int x1, int y1,
            int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
    }

    /**
     * Dibuja una linea con un estilo.
     *
     * <p>El estilo --punteada, por ejemplo-- lo interpreta cada aspecto. Esta version no lo mira, que
     * es lo que hace la de base del JDK: dibuja la linea entera.
     *
     * @param context que se esta dibujando
     * @param paintKey para que es la linea, o {@code null}
     * @param g donde dibujar
     * @param x1 desde
     * @param y1 desde
     * @param x2 hasta
     * @param y2 hasta
     * @param styleKey el estilo, o {@code null}
     */
    public void drawLine(SynthContext context, Object paintKey, Graphics g, int x1, int y1,
            int x2, int y2, Object styleKey) {
        drawLine(context, paintKey, g, x1, y1, x2, y2);
    }

    /**
     * Reparte el texto y el icono dentro del rectangulo disponible.
     *
     * @param ss que se esta dibujando
     * @param fm las medidas de la tipografia
     * @param text el texto, o {@code null}
     * @param icon el icono, o {@code null}
     * @param hAlign la alineacion horizontal
     * @param vAlign la alineacion vertical
     * @param hTextPosition donde va el texto respecto del icono, horizontalmente
     * @param vTextPosition donde va el texto respecto del icono, verticalmente
     * @param viewR el rectangulo disponible
     * @param iconR se llena con donde va el icono
     * @param textR se llena con donde va el texto
     * @param iconTextGap cuanto espacio dejar entre los dos
     * @return el texto tal como se va a dibujar, acortado si no entraba
     */
    public String layoutText(SynthContext ss, FontMetrics fm, String text, Icon icon, int hAlign,
            int vAlign, int hTextPosition, int vTextPosition, Rectangle viewR, Rectangle iconR,
            Rectangle textR, int iconTextGap) {
        return SwingUtilities.layoutCompoundLabel(ss.getComponent(), fm, text, icon, vAlign,
                hAlign, vTextPosition, hTextPosition, viewR, iconR, textR, iconTextGap);
    }

    /**
     * Cuanto mide ese texto.
     *
     * @param ss que se esta dibujando
     * @param font la tipografia
     * @param metrics sus medidas
     * @param text el texto, o {@code null}
     * @return el ancho en pixeles
     */
    public int computeStringWidth(SynthContext ss, Font font, FontMetrics metrics, String text) {
        return text == null ? 0 : metrics.stringWidth(text);
    }

    /**
     * El tamano mas chico con el que entra el texto y el icono.
     *
     * @param ss que se esta dibujando
     * @param font la tipografia
     * @param text el texto, o {@code null}
     * @param icon el icono, o {@code null}
     * @param hAlign la alineacion horizontal
     * @param vAlign la alineacion vertical
     * @param hTextPosition donde va el texto respecto del icono, horizontalmente
     * @param vTextPosition donde va el texto respecto del icono, verticalmente
     * @param iconTextGap cuanto espacio dejar entre los dos
     * @param mnemonicIndex la letra del atajo, o -1
     * @return el tamano
     */
    public Dimension getMinimumSize(SynthContext ss, Font font, String text, Icon icon,
            int hAlign, int vAlign, int hTextPosition, int vTextPosition, int iconTextGap,
            int mnemonicIndex) {
        return getPreferredSize(ss, font, text, icon, hAlign, vAlign, hTextPosition, vTextPosition,
                iconTextGap, mnemonicIndex);
    }

    /**
     * El tamano mas grande que acepta.
     *
     * @param ss que se esta dibujando
     * @param font la tipografia
     * @param text el texto, o {@code null}
     * @param icon el icono, o {@code null}
     * @param hAlign la alineacion horizontal
     * @param vAlign la alineacion vertical
     * @param hTextPosition donde va el texto respecto del icono, horizontalmente
     * @param vTextPosition donde va el texto respecto del icono, verticalmente
     * @param iconTextGap cuanto espacio dejar entre los dos
     * @param mnemonicIndex la letra del atajo, o -1
     * @return el tamano
     */
    public Dimension getMaximumSize(SynthContext ss, Font font, String text, Icon icon,
            int hAlign, int vAlign, int hTextPosition, int vTextPosition, int iconTextGap,
            int mnemonicIndex) {
        return getPreferredSize(ss, font, text, icon, hAlign, vAlign, hTextPosition, vTextPosition,
                iconTextGap, mnemonicIndex);
    }

    /**
     * Cuanto ocupa de alto la letra mas alta.
     *
     * @param context que se esta dibujando
     * @return el alto en pixeles
     */
    public int getMaximumCharHeight(SynthContext context) {
        final Font f = context.getStyle().getFont(context);
        final FontMetrics fm = context.getComponent().getFontMetrics(f);
        return fm == null ? 0 : fm.getHeight();
    }

    /**
     * El tamano que preferiria tener.
     *
     * @param ss que se esta dibujando
     * @param font la tipografia
     * @param text el texto, o {@code null}
     * @param icon el icono, o {@code null}
     * @param hAlign la alineacion horizontal
     * @param vAlign la alineacion vertical
     * @param hTextPosition donde va el texto respecto del icono, horizontalmente
     * @param vTextPosition donde va el texto respecto del icono, verticalmente
     * @param iconTextGap cuanto espacio dejar entre los dos
     * @param mnemonicIndex la letra del atajo, o -1
     * @return el tamano
     */
    public Dimension getPreferredSize(SynthContext ss, Font font, String text, Icon icon,
            int hAlign, int vAlign, int hTextPosition, int vTextPosition, int iconTextGap,
            int mnemonicIndex) {
        final FontMetrics fm = ss.getComponent().getFontMetrics(font);
        final int anchoTexto = text == null || fm == null ? 0 : fm.stringWidth(text);
        final int altoTexto = text == null || fm == null ? 0 : fm.getHeight();
        final int anchoIcono = icon == null ? 0 : icon.getIconWidth();
        final int altoIcono = icon == null ? 0 : icon.getIconHeight();
        if (icon == null) {
            return new Dimension(anchoTexto, altoTexto);
        }
        if (text == null) {
            return new Dimension(anchoIcono, altoIcono);
        }
        // Uno al lado del otro o uno encima del otro, segun donde vaya el texto. El hueco solo
        // cuenta en la direccion en que estan separados.
        if (hTextPosition == SwingConstants.CENTER) {
            return new Dimension(Math.max(anchoTexto, anchoIcono),
                    altoTexto + altoIcono + iconTextGap);
        }
        return new Dimension(anchoTexto + anchoIcono + iconTextGap,
                Math.max(altoTexto, altoIcono));
    }

    /**
     * Dibuja el texto dentro de ese rectangulo.
     *
     * @param ss que se esta dibujando
     * @param g donde dibujar
     * @param text el texto, o {@code null}
     * @param bounds el rectangulo
     * @param mnemonicIndex la letra del atajo, o -1
     */
    public void paintText(SynthContext ss, Graphics g, String text, Rectangle bounds,
            int mnemonicIndex) {
        if (text != null) {
            paintText(ss, g, text, bounds.x, bounds.y, mnemonicIndex);
        }
    }

    /**
     * Dibuja el texto en esa posicion.
     *
     * @param ss que se esta dibujando
     * @param g donde dibujar
     * @param text el texto, o {@code null}
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param mnemonicIndex la letra del atajo, o -1
     */
    public void paintText(SynthContext ss, Graphics g, String text, int x, int y,
            int mnemonicIndex) {
        if (text == null) {
            return;
        }
        final FontMetrics fm = g.getFontMetrics();
        // La `y` que llega es la de arriba del rectangulo y `drawString` quiere la de la linea de
        // base: sin sumar el ascenso, el texto sale por encima de donde tiene que ir.
        g.drawString(text, x, y + (fm == null ? 0 : fm.getAscent()));
    }

    /**
     * Dibuja el texto y el icono juntos.
     *
     * @param ss que se esta dibujando
     * @param g donde dibujar
     * @param text el texto, o {@code null}
     * @param icon el icono, o {@code null}
     * @param hAlign la alineacion horizontal
     * @param vAlign la alineacion vertical
     * @param hTextPosition donde va el texto respecto del icono, horizontalmente
     * @param vTextPosition donde va el texto respecto del icono, verticalmente
     * @param iconTextGap cuanto espacio dejar entre los dos
     * @param mnemonicIndex la letra del atajo, o -1
     * @param textOffset cuanto correr el texto, para el efecto de apretado
     */
    public void paintText(SynthContext ss, Graphics g, String text, Icon icon, int hAlign,
            int vAlign, int hTextPosition, int vTextPosition, int iconTextGap, int mnemonicIndex,
            int textOffset) {
        final Rectangle disponible = new Rectangle(ss.getComponent().getSize());
        final Rectangle rIcono = new Rectangle();
        final Rectangle rTexto = new Rectangle();
        final FontMetrics fm = g.getFontMetrics();
        final String recortado = layoutText(ss, fm, text, icon, hAlign, vAlign, hTextPosition,
                vTextPosition, disponible, rIcono, rTexto, iconTextGap);
        if (icon != null) {
            paintIcon(icon, ss, g, rIcono.x + textOffset, rIcono.y + textOffset,
                    rIcono.width, rIcono.height);
        }
        paintText(ss, g, recortado, rTexto.x + textOffset, rTexto.y + textOffset, mnemonicIndex);
    }

    /**
     * El ancho de un icono, que puede depender del contexto.
     *
     * @param icon el icono, o {@code null}
     * @param context que se esta dibujando
     * @return el ancho, o cero
     */
    public static int getIconWidth(Icon icon, SynthContext context) {
        return icon == null ? 0 : icon.getIconWidth();
    }

    /**
     * El alto de un icono, que puede depender del contexto.
     *
     * @param icon el icono, o {@code null}
     * @param context que se esta dibujando
     * @return el alto, o cero
     */
    public static int getIconHeight(Icon icon, SynthContext context) {
        return icon == null ? 0 : icon.getIconHeight();
    }

    /**
     * Dibuja un icono, que puede depender del contexto.
     *
     * @param icon el icono, o {@code null}
     * @param context que se esta dibujando
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public static void paintIcon(Icon icon, SynthContext context, Graphics g, int x, int y,
            int w, int h) {
        if (icon != null) {
            icon.paintIcon(context.getComponent(), g, x, y);
        }
    }
}
