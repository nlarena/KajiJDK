package java.awt;

import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.text.CharacterIterator;

/**
 * Cuánto mide el texto de una {@link Font} en un dispositivo concreto.
 *
 * <p>La fuente dice cómo son las letras; las métricas dicen cuánto ocupan una vez dibujadas, que
 * depende del dispositivo: en pantalla los avances se redondean a píxeles enteros y en una impresora
 * no. Por eso las métricas se piden a través de un {@link Graphics} y no directamente a la fuente.
 *
 * <p>Casi todo acá está definido en términos de {@link #charsWidth} y {@link #getWidths}, que se
 * llaman entre sí. <strong>Una subclase tiene que redefinir por lo menos una de las dos</strong>, o
 * la primera medición se va a recursión infinita. Es el diseño del JDK y se mantiene igual: cambiar
 * cuál es la primitiva rompería a cualquiera que ya haya redefinido la otra.
 */
public abstract class FontMetrics implements Serializable {

    private static final long serialVersionUID = 1681126225205050147L;

    private static final FontRenderContext DEFAULT_FRC =
            new FontRenderContext(null, false, false);

    /** La fuente que se está midiendo. */
    protected Font font;

    /** Con la fuente dada. */
    protected FontMetrics(Font font) {
        this.font = font;
    }

    /** La fuente que se está midiendo. */
    public Font getFont() {
        return this.font;
    }

    /**
     * Las condiciones de dibujo que suponen estas métricas.
     *
     * <p>La implementación de acá devuelve unas sin transformación, sin suavizado y sin métricas
     * fraccionarias, que es lo que corresponde a una pantalla común.
     */
    public FontRenderContext getFontRenderContext() {
        return DEFAULT_FRC;
    }

    /** El aire entre el fondo de un renglón y el techo del siguiente. */
    public int getLeading() {
        return 0;
    }

    /** Cuánto sube el texto por encima de la línea de base. */
    public int getAscent() {
        return 0;
    }

    /** Cuánto baja el texto por debajo de la línea de base. */
    public int getDescent() {
        return 0;
    }

    /** La suma de las tres anteriores: de cuánto en cuánto van los renglones. */
    public int getHeight() {
        return this.getLeading() + this.getAscent() + this.getDescent();
    }

    /**
     * Lo máximo que sube cualquier carácter de la fuente.
     *
     * <p>Puede ser más que {@link #getAscent}: ese es el ascenso típico, éste el peor caso.
     */
    public int getMaxAscent() {
        return this.getAscent();
    }

    /** Lo máximo que baja cualquier carácter de la fuente. */
    public int getMaxDescent() {
        return this.getDescent();
    }

    /**
     * Lo mismo que {@link #getMaxDescent}.
     *
     * @deprecated el nombre está mal escrito. Se mantiene porque está en la API desde 1.0.
     */
    @Deprecated
    public int getMaxDecent() {
        return this.getMaxDescent();
    }

    /**
     * El avance del carácter más ancho, o -1 si no se sabe.
     *
     * <p>El -1 es una respuesta y no un error: hay fuentes en las que averiguarlo obligaría a medir
     * todos los glifos, y decir que no se sabe es más honesto que devolver una cota inventada.
     */
    public int getMaxAdvance() {
        return -1;
    }

    /**
     * El ancho de un carácter dado por su punto de código.
     *
     * <p>Un punto que no sea válido se mide como el glifo faltante, que es lo que se va a dibujar.
     */
    public int charWidth(int codePoint) {
        int cp = codePoint;
        if (!Character.isValidCodePoint(cp)) {
            cp = 0xFFFF;
        }
        if (cp < 256) {
            return this.getWidths()[cp];
        }
        char[] buffer = new char[2];
        int len = Character.toChars(cp, buffer, 0);
        return this.charsWidth(buffer, 0, len);
    }

    /**
     * El ancho de un carácter.
     *
     * <p>No sirve para los caracteres que se escriben con dos `char`; para ésos está la versión de
     * punto de código.
     */
    public int charWidth(char ch) {
        if (ch < 256) {
            return this.getWidths()[ch];
        }
        char[] data = new char[1];
        data[0] = ch;
        return this.charsWidth(data, 0, 1);
    }

    /** El ancho de una cadena. */
    public int stringWidth(String str) {
        int len = str.length();
        char[] data = new char[len];
        str.getChars(0, len, data, 0);
        return this.charsWidth(data, 0, len);
    }

    /** El ancho de un tramo de un arreglo de caracteres. */
    public int charsWidth(char[] data, int off, int len) {
        return this.stringWidth(new String(data, off, len));
    }

    /**
     * El ancho de un tramo de bytes, tomando cada uno como un carácter.
     *
     * @deprecated no traduce correctamente los bytes a caracteres en ninguna codificación que no sea
     *     Latin-1. Se mantiene porque está en la API desde 1.0.
     */
    @Deprecated
    public int bytesWidth(byte[] data, int off, int len) {
        return this.charsWidth(new String(data, off, len).toCharArray(), 0, len);
    }

    /** El ancho de los primeros 256 caracteres. */
    public int[] getWidths() {
        int[] widths = new int[256];
        for (char ch = 0; ch < 256; ch++) {
            widths[ch] = this.charWidth(ch);
        }
        return widths;
    }

    /** Si todos los caracteres de la fuente comparten las mismas medidas de renglón. */
    public boolean hasUniformLineMetrics() {
        return this.font.hasUniformLineMetrics();
    }

    /** Las medidas verticales de esa cadena. */
    public LineMetrics getLineMetrics(String str, Graphics context) {
        return this.font.getLineMetrics(str, this.myFRC(context));
    }

    /** Las medidas verticales de un tramo de esa cadena. */
    public LineMetrics getLineMetrics(String str, int beginIndex, int limit, Graphics context) {
        return this.font.getLineMetrics(str, beginIndex, limit, this.myFRC(context));
    }

    /** Las medidas verticales de un tramo de caracteres. */
    public LineMetrics getLineMetrics(char[] chars, int beginIndex, int limit, Graphics context) {
        return this.font.getLineMetrics(chars, beginIndex, limit, this.myFRC(context));
    }

    /** Las medidas verticales de un tramo de un iterador. */
    public LineMetrics getLineMetrics(CharacterIterator ci, int beginIndex, int limit,
            Graphics context) {
        return this.font.getLineMetrics(ci, beginIndex, limit, this.myFRC(context));
    }

    /** El rectángulo que ocupa esa cadena. */
    public Rectangle2D getStringBounds(String str, Graphics context) {
        return this.font.getStringBounds(str, this.myFRC(context));
    }

    /** El rectángulo que ocupa un tramo de esa cadena. */
    public Rectangle2D getStringBounds(String str, int beginIndex, int limit, Graphics context) {
        return this.font.getStringBounds(str, beginIndex, limit, this.myFRC(context));
    }

    /** El rectángulo que ocupa un tramo de caracteres. */
    public Rectangle2D getStringBounds(char[] chars, int beginIndex, int limit, Graphics context) {
        return this.font.getStringBounds(chars, beginIndex, limit, this.myFRC(context));
    }

    /** El rectángulo que ocupa un tramo de un iterador. */
    public Rectangle2D getStringBounds(CharacterIterator ci, int beginIndex, int limit,
            Graphics context) {
        return this.font.getStringBounds(ci, beginIndex, limit, this.myFRC(context));
    }

    /** El rectángulo del carácter más grande de la fuente. */
    public Rectangle2D getMaxCharBounds(Graphics context) {
        return this.font.getMaxCharBounds(this.myFRC(context));
    }

    /** Las condiciones de dibujo del contexto dado, o las de por omisión. */
    private FontRenderContext myFRC(Graphics context) {
        if (context instanceof Graphics2D) {
            return ((Graphics2D) context).getFontRenderContext();
        }
        return DEFAULT_FRC;
    }

    public String toString() {
        return this.getClass().getName() + "[font=" + this.getFont() + "ascent="
                + this.getAscent() + ", descent=" + this.getDescent() + ", height="
                + this.getHeight() + "]";
    }
}
