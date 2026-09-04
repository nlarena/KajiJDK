package java.awt.font;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Una tira de glifos ya colocados, lista para dibujar.
 *
 * <p>Es el resultado de haber resuelto todo lo que un texto tiene de difícil: qué glifo le
 * corresponde a cada carácter, cuáles se juntan en una ligadura, en qué orden van si el renglón
 * mezcla direcciones, y dónde cae cada uno. Después de eso ya no hay caracteres, hay dibujos con
 * coordenadas.
 *
 * <p>Un glifo no es un carácter, y por eso hay {@link #getGlyphCharIndex}: una ligadura es un glifo
 * para dos caracteres, un acento suelto puede ser un glifo para ninguno, y en un renglón
 * bidireccional el orden de los glifos no es el de los caracteres. La correspondencia se guarda
 * porque hace falta para poner el cursor donde el usuario cree que hizo clic.
 *
 * <p>Hay dos rectángulos por glifo y conviene no confundirlos. El **lógico** es el lugar que ocupa a
 * los efectos de la selección y del renglón; el **visual**, dónde cae la tinta. El primero incluye
 * el aire de los costados y el segundo no.
 */
public abstract class GlyphVector implements Cloneable {

    /** Algún glifo tiene una transformación propia. */
    public static final int FLAG_HAS_TRANSFORMS = 1;

    /** Alguna posición se corrigió respecto de la que daría el avance. */
    public static final int FLAG_HAS_POSITION_ADJUSTMENTS = 2;

    /** La tira va de derecha a izquierda. */
    public static final int FLAG_RUN_RTL = 4;

    /** La correspondencia entre glifos y caracteres no es uno a uno en orden. */
    public static final int FLAG_COMPLEX_GLYPHS = 8;

    /** Los bits que usan las banderas anteriores. */
    public static final int FLAG_MASK = FLAG_HAS_TRANSFORMS | FLAG_HAS_POSITION_ADJUSTMENTS
            | FLAG_RUN_RTL | FLAG_COMPLEX_GLYPHS;

    /** Para las subclases. */
    protected GlyphVector() {
    }

    /** La fuente de la que salieron los glifos. */
    public abstract Font getFont();

    /** Las condiciones en las que se armó. */
    public abstract FontRenderContext getFontRenderContext();

    /** Vuelve a colocar los glifos en sus posiciones por omisión. */
    public abstract void performDefaultLayout();

    /** Cuántos glifos hay. */
    public abstract int getNumGlyphs();

    /** El código de ese glifo dentro de su fuente. */
    public abstract int getGlyphCode(int glyphIndex);

    /** Los códigos de un tramo de glifos. */
    public abstract int[] getGlyphCodes(int beginGlyphIndex, int numEntries, int[] codeReturn);

    /**
     * Qué carácter le dio origen a ese glifo.
     *
     * <p>La implementación de acá supone la correspondencia trivial —el glifo `i` viene del carácter
     * `i`—, que es la correcta mientras no haya ligaduras ni reordenamiento. Una subclase que arme
     * texto complejo tiene que redefinirla.
     */
    public int getGlyphCharIndex(int glyphIndex) {
        return glyphIndex;
    }

    /**
     * Lo mismo para un tramo.
     *
     * @throws IllegalArgumentException si `numEntries` es negativo
     */
    public int[] getGlyphCharIndices(int beginGlyphIndex, int numEntries, int[] codeReturn) {
        if (numEntries < 0) {
            throw new IllegalArgumentException("numEntries must be >= 0");
        }
        int[] out = codeReturn;
        if (out == null) {
            out = new int[numEntries];
        }
        for (int i = 0; i < numEntries; i++) {
            out[i] = this.getGlyphCharIndex(beginGlyphIndex + i);
        }
        return out;
    }

    /** El rectángulo que ocupa la tira a efectos de renglón y selección. */
    public abstract Rectangle2D getLogicalBounds();

    /** El rectángulo donde cae la tinta. */
    public abstract Rectangle2D getVisualBounds();

    /**
     * Los píxeles que va a tocar la tira dibujada en `(x, y)`.
     *
     * <p>Es el rectángulo visual redondeado hacia afuera: un píxel tocado a medias es un píxel
     * tocado.
     */
    public Rectangle getPixelBounds(FontRenderContext renderFRC, float x, float y) {
        return redondearAfuera(this.getVisualBounds(), x, y);
    }

    /** El rectángulo entero de píxeles que cubre un rectángulo continuo corrido `(x, y)`. */
    private static Rectangle redondearAfuera(Rectangle2D rect, float x, float y) {
        int l = (int) Math.floor(rect.getX() + x);
        int t = (int) Math.floor(rect.getY() + y);
        int r = (int) Math.ceil(rect.getMaxX() + x);
        int b = (int) Math.ceil(rect.getMaxY() + y);
        return new Rectangle(l, t, r - l, b - t);
    }

    /** El contorno de toda la tira. */
    public abstract Shape getOutline();

    /** El contorno de toda la tira, corrido a `(x, y)`. */
    public abstract Shape getOutline(float x, float y);

    /** El contorno de un glifo. */
    public abstract Shape getGlyphOutline(int glyphIndex);

    /** El contorno de un glifo, corrido a `(x, y)`. */
    public Shape getGlyphOutline(int glyphIndex, float x, float y) {
        Shape s = this.getGlyphOutline(glyphIndex);
        AffineTransform at = AffineTransform.getTranslateInstance(x, y);
        return at.createTransformedShape(s);
    }

    /**
     * Dónde está ese glifo.
     *
     * <p>Se admite el índice igual a la cantidad de glifos: ésa es la posición donde iría el
     * siguiente, o sea el final de la tira.
     */
    public abstract Point2D getGlyphPosition(int glyphIndex);

    /** Mueve un glifo. */
    public abstract void setGlyphPosition(int glyphIndex, Point2D newPos);

    /** La transformación propia de ese glifo, o `null` si es la identidad. */
    public abstract AffineTransform getGlyphTransform(int glyphIndex);

    /** Le pone una transformación propia a un glifo. */
    public abstract void setGlyphTransform(int glyphIndex, AffineTransform newTX);

    /**
     * Las banderas que describen a la tira.
     *
     * <p>La implementación de acá devuelve 0, que es lo cierto para una tira simple. Una subclase
     * que admita transformaciones por glifo o texto complejo tiene que redefinirla.
     */
    public int getLayoutFlags() {
        return 0;
    }

    /** Las posiciones de un tramo de glifos, como pares. */
    public abstract float[] getGlyphPositions(int beginGlyphIndex, int numEntries,
            float[] positionReturn);

    /** El lugar que ocupa un glifo a efectos de selección. */
    public abstract Shape getGlyphLogicalBounds(int glyphIndex);

    /** Dónde cae la tinta de un glifo. */
    public abstract Shape getGlyphVisualBounds(int glyphIndex);

    /** Los píxeles que va a tocar un glifo dibujado en `(x, y)`. */
    public Rectangle getGlyphPixelBounds(int index, FontRenderContext renderFRC, float x,
            float y) {
        return redondearAfuera(this.getGlyphVisualBounds(index).getBounds2D(), x, y);
    }

    /** Las medidas de un glifo. */
    public abstract GlyphMetrics getGlyphMetrics(int glyphIndex);

    /** Cómo se estira o se encoge un glifo al justificar. */
    public abstract GlyphJustificationInfo getGlyphJustificationInfo(int glyphIndex);

    /** Igualdad por fuente, condiciones, códigos y posiciones. */
    public abstract boolean equals(GlyphVector set);
}
