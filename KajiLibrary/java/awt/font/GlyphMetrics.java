package java.awt.font;

import java.awt.geom.Rectangle2D;

/**
 * Las medidas de **un** glifo.
 *
 * <p>Hay dos medidas distintas y conviene no confundirlas. El **avance** es cuánto hay que correrse
 * para dibujar el glifo siguiente; el **rectángulo** es dónde cae la tinta. No coinciden: un espacio
 * tiene avance y no tiene tinta, y una letra inclinada puede pintar más allá de su avance.
 *
 * <p>De esa diferencia salen los dos márgenes. El izquierdo ({@link #getLSB}) es el aire entre el
 * origen y donde empieza la tinta, y el derecho ({@link #getRSB}), el que queda entre donde termina
 * y el avance. Cualquiera de los dos puede ser **negativo**, y eso es lo que permite que una letra
 * se meta debajo de la anterior.
 *
 * <p>El tipo dice qué es el glifo respecto de los caracteres: uno normal es un carácter, una
 * ligadura son varios en un solo dibujo, y un combinante —un acento suelto— es un glifo que se apoya
 * sobre otro sin avanzar.
 */
public final class GlyphMetrics {

    /** Un glifo por carácter. */
    public static final byte STANDARD = 0;

    /** Un glifo que dibuja varios caracteres juntos. */
    public static final byte LIGATURE = 1;

    /** Un glifo que se apoya sobre otro, como un acento. */
    public static final byte COMBINING = 2;

    /** Un glifo que es parte de otro y no tiene carácter propio. */
    public static final byte COMPONENT = 3;

    /** Un glifo sin tinta, que sólo avanza. */
    public static final byte WHITESPACE = 4;

    private final boolean horizontal;
    private final float advanceX;
    private final float advanceY;
    private final Rectangle2D.Float bounds;
    private final byte glyphType;

    /** Con avance horizontal. */
    public GlyphMetrics(float advance, Rectangle2D bounds, byte glyphType) {
        this.horizontal = true;
        this.advanceX = advance;
        this.advanceY = 0;
        this.bounds = new Rectangle2D.Float();
        this.bounds.setRect(bounds);
        this.glyphType = glyphType;
    }

    /** Con avance en la dirección que se indique. */
    public GlyphMetrics(boolean horizontal, float advanceX, float advanceY, Rectangle2D bounds,
            byte glyphType) {
        this.horizontal = horizontal;
        this.advanceX = advanceX;
        this.advanceY = advanceY;
        this.bounds = new Rectangle2D.Float();
        this.bounds.setRect(bounds);
        this.glyphType = glyphType;
    }

    /** El avance en la dirección del texto. */
    public float getAdvance() {
        if (this.horizontal) {
            return this.advanceX;
        }
        return this.advanceY;
    }

    /** El avance horizontal. */
    public float getAdvanceX() {
        return this.advanceX;
    }

    /** El avance vertical. */
    public float getAdvanceY() {
        return this.advanceY;
    }

    /** Dónde cae la tinta, relativo al origen del glifo. */
    public Rectangle2D getBounds2D() {
        return new Rectangle2D.Float(this.bounds.x, this.bounds.y, this.bounds.width,
                this.bounds.height);
    }

    /** El aire antes de la tinta; puede ser negativo. */
    public float getLSB() {
        if (this.horizontal) {
            return this.bounds.x;
        }
        return this.bounds.y;
    }

    /** El aire después de la tinta; puede ser negativo. */
    public float getRSB() {
        if (this.horizontal) {
            return this.advanceX - this.bounds.x - this.bounds.width;
        }
        return this.advanceY - this.bounds.y - this.bounds.height;
    }

    /** El tipo crudo, con los bits de clase y el de espacio en blanco juntos. */
    public int getType() {
        return this.glyphType;
    }

    /** Si es un glifo por carácter. */
    public boolean isStandard() {
        return (this.glyphType & 0x3) == STANDARD;
    }

    /** Si dibuja varios caracteres juntos. */
    public boolean isLigature() {
        return (this.glyphType & 0x3) == LIGATURE;
    }

    /** Si se apoya sobre otro glifo. */
    public boolean isCombining() {
        return (this.glyphType & 0x3) == COMBINING;
    }

    /** Si es parte de otro glifo. */
    public boolean isComponent() {
        return (this.glyphType & 0x3) == COMPONENT;
    }

    /**
     * Si no tiene tinta.
     *
     * <p>Es independiente de las otras cuatro: el espacio en blanco vive en su propio bit, así que
     * un glifo puede ser a la vez estándar y blanco.
     */
    public boolean isWhitespace() {
        return (this.glyphType & 0x4) == WHITESPACE;
    }
}
