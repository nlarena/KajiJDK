package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * El borde de dos pixeles que simula relieve: como si el componente estuviera levantado o hundido.
 *
 * <h2>Como se finge relieve con cuatro colores</h2>
 *
 * <p>La ilusion es vieja y sencilla: si la luz viene de arriba a la izquierda, los bordes que miran
 * hacia esa luz se ven mas claros y los opuestos mas oscuros. Levantado y hundido son
 * <strong>el mismo dibujo con los colores intercambiados</strong> — de ahi que
 * {@link #paintRaisedBevel} y {@link #paintLoweredBevel} sean casi el mismo codigo.
 *
 * <p>Los cuatro colores son dos por lado porque el borde tiene dos pixeles de grosor y cada uno
 * lleva su tono: el de afuera mas extremo, el de adentro mas suave. Todos son opcionales, y cuando
 * faltan se derivan del fondo del componente con {@link Color#brighter} y {@link Color#darker} —
 * asi el mismo borde funciona sobre cualquier color sin configurarlo.
 */
public class BevelBorder extends AbstractBorder {

    private static final long serialVersionUID = -1034942243356299676L;

    /** El componente se ve levantado. */
    public static final int RAISED = 0;
    /** El componente se ve hundido. */
    public static final int LOWERED = 1;

    protected int bevelType;
    protected Color highlightOuter;
    protected Color highlightInner;
    protected Color shadowInner;
    protected Color shadowOuter;

    /** Con los colores derivados del fondo del componente. */
    public BevelBorder(int bevelType) {
        this.bevelType = bevelType;
    }

    /**
     * Con un color claro y uno oscuro.
     *
     * <p>Los cuatro tonos salen de esos dos: el par de afuera se aclara u oscurece un paso mas.
     */
    public BevelBorder(int bevelType, Color highlight, Color shadow) {
        this(bevelType, highlight.brighter(), highlight, shadow, shadow.brighter());
    }

    /** Con los cuatro tonos explicitos. */
    public BevelBorder(int bevelType, Color highlightOuterColor, Color highlightInnerColor,
            Color shadowOuterColor, Color shadowInnerColor) {
        this(bevelType);
        this.highlightOuter = highlightOuterColor;
        this.highlightInner = highlightInnerColor;
        this.shadowOuter = shadowOuterColor;
        this.shadowInner = shadowInnerColor;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if (this.bevelType == RAISED) {
            paintRaisedBevel(c, g, x, y, width, height);
        } else if (this.bevelType == LOWERED) {
            paintLoweredBevel(c, g, x, y, width, height);
        }
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = 2;
        insets.top = 2;
        insets.right = 2;
        insets.bottom = 2;
        return insets;
    }

    /** El tono claro de afuera; derivado del fondo de {@code c} si no se fijo uno. */
    public Color getHighlightOuterColor(Component c) {
        Color propio = getHighlightOuterColor();
        if (propio != null) {
            return propio;
        }
        return c.getBackground().brighter().brighter();
    }

    /** El tono claro de adentro. */
    public Color getHighlightInnerColor(Component c) {
        Color propio = getHighlightInnerColor();
        if (propio != null) {
            return propio;
        }
        return c.getBackground().brighter();
    }

    /** El tono oscuro de adentro. */
    public Color getShadowInnerColor(Component c) {
        Color propio = getShadowInnerColor();
        if (propio != null) {
            return propio;
        }
        return c.getBackground().darker();
    }

    /** El tono oscuro de afuera. */
    public Color getShadowOuterColor(Component c) {
        Color propio = getShadowOuterColor();
        if (propio != null) {
            return propio;
        }
        return c.getBackground().darker().darker();
    }

    /** El tono claro de afuera que se fijo, o {@code null}. */
    public Color getHighlightOuterColor() {
        return this.highlightOuter;
    }

    /** El tono claro de adentro que se fijo, o {@code null}. */
    public Color getHighlightInnerColor() {
        return this.highlightInner;
    }

    /** El tono oscuro de adentro que se fijo, o {@code null}. */
    public Color getShadowInnerColor() {
        return this.shadowInner;
    }

    /** El tono oscuro de afuera que se fijo, o {@code null}. */
    public Color getShadowOuterColor() {
        return this.shadowOuter;
    }

    /** {@link #RAISED} o {@link #LOWERED}. */
    public int getBevelType() {
        return this.bevelType;
    }

    /** Opaco: los dos pixeles de cada lado se pintan enteros. */
    public boolean isBorderOpaque() {
        return true;
    }

    /** Dibuja el relieve levantado: claro arriba y a la izquierda. */
    protected void paintRaisedBevel(Component c, Graphics g, int x, int y, int width, int height) {
        pintar(g, x, y, width, height,
                getHighlightOuterColor(c), getHighlightInnerColor(c),
                getShadowOuterColor(c), getShadowInnerColor(c));
    }

    /**
     * Dibuja el relieve hundido.
     *
     * <p>Es el mismo dibujo que {@link #paintRaisedBevel} con los pares intercambiados: lo oscuro
     * pasa arriba y a la izquierda. Toda la ilusion esta en ese cambio.
     */
    protected void paintLoweredBevel(Component c, Graphics g, int x, int y, int width, int height) {
        pintar(g, x, y, width, height,
                getShadowInnerColor(c), getShadowOuterColor(c),
                getHighlightInnerColor(c), getHighlightOuterColor(c));
    }

    /**
     * El dibujo, parametrizado por los cuatro tonos.
     *
     * <p>Existe para que las dos formas de relieve no sean dos copias del mismo trazado: una copia
     * es donde se corrige un bug una sola vez de dos.
     */
    private void pintar(Graphics g, int x, int y, int width, int height,
            Color arribaAfuera, Color arribaAdentro, Color abajoAfuera, Color abajoAdentro) {
        Color viejoColor = g.getColor();
        int h = height;
        int w = width;

        g.translate(x, y);

        g.setColor(arribaAfuera);
        g.drawLine(0, 0, 0, h - 2);
        g.drawLine(1, 0, w - 2, 0);

        g.setColor(arribaAdentro);
        g.drawLine(1, 1, 1, h - 3);
        g.drawLine(2, 1, w - 3, 1);

        g.setColor(abajoAfuera);
        g.drawLine(0, h - 1, w - 1, h - 1);
        g.drawLine(w - 1, 0, w - 1, h - 2);

        g.setColor(abajoAdentro);
        g.drawLine(1, h - 2, w - 2, h - 2);
        g.drawLine(w - 2, 1, w - 2, h - 3);

        g.translate(-x, -y);
        g.setColor(viejoColor);
    }
}
