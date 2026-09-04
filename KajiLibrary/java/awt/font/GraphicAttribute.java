package java.awt.font;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Un dibujo que ocupa el lugar de un carácter dentro de un renglón de texto.
 *
 * <p>Es cómo se mete una imagen, una viñeta o una figura en medio de una frase y sigue siendo texto:
 * el objeto declara cuánto sube, cuánto baja y cuánto avanza, y el armador de renglones lo trata
 * como a un glifo más.
 *
 * <p>Lo único que no se comporta como un glifo es la **alineación**. Un glifo se apoya siempre en su
 * línea de base; un dibujo puede además alinearse con el techo o con el piso del renglón
 * ({@link #TOP_ALIGNMENT}, {@link #BOTTOM_ALIGNMENT}), que es lo que hace falta para que una imagen
 * alta no cuelgue de la línea de base como si fuera una letra.
 */
public abstract class GraphicAttribute {

    /** Se alinea con el techo del renglón. */
    public static final int TOP_ALIGNMENT = -1;

    /** Se alinea con el piso del renglón. */
    public static final int BOTTOM_ALIGNMENT = -2;

    /** Se apoya en la línea de base romana. */
    public static final int ROMAN_BASELINE = Font.ROMAN_BASELINE;

    /** Se centra en la línea de base de las escrituras ideográficas. */
    public static final int CENTER_BASELINE = Font.CENTER_BASELINE;

    /** Cuelga de la línea de base de las escrituras índicas. */
    public static final int HANGING_BASELINE = Font.HANGING_BASELINE;

    private final int alignment;

    /**
     * Con la alineación dada.
     *
     * @throws IllegalArgumentException si no es una de las cinco
     */
    protected GraphicAttribute(int alignment) {
        if (alignment < BOTTOM_ALIGNMENT || alignment > HANGING_BASELINE) {
            throw new IllegalArgumentException("bad alignment");
        }
        this.alignment = alignment;
    }

    /** Cuánto sube por encima de la línea de base. */
    public abstract float getAscent();

    /** Cuánto baja por debajo de la línea de base. */
    public abstract float getDescent();

    /** Cuánto avanza el renglón después de dibujarlo. */
    public abstract float getAdvance();

    /**
     * Dónde cae la tinta, relativo al punto de origen.
     *
     * <p>La implementación de acá supone que el dibujo llena exactamente su caja de medidas. Una
     * subclase cuya tinta se salga —una figura con un trazo grueso, por ejemplo— tiene que
     * redefinirla, o el renglón va a calcular mal qué hay que repintar.
     */
    public Rectangle2D getBounds() {
        float ascent = this.getAscent();
        return new Rectangle2D.Float(0, -ascent, this.getAdvance(), ascent + this.getDescent());
    }

    /**
     * El contorno del dibujo, transformado.
     *
     * <p>La implementación de acá devuelve la caja de {@link #getBounds}, que es una aproximación
     * honesta: un dibujo cualquiera no tiene por qué tener un contorno más preciso que su caja.
     */
    public Shape getOutline(AffineTransform tx) {
        Shape b = this.getBounds();
        if (tx != null) {
            b = tx.createTransformedShape(b);
        }
        return b;
    }

    /** Dibuja, con el origen en `(x, y)`. */
    public abstract void draw(Graphics2D graphics, float x, float y);

    /** Con qué se alinea dentro del renglón. */
    public final int getAlignment() {
        return this.alignment;
    }

    /**
     * Cómo se estira o se encoge al justificar.
     *
     * <p>Por omisión se comporta como espacio entre letras: se puede estirar hasta un tercio de su
     * avance de cada lado y no se puede encoger. Es la respuesta prudente para un dibujo del que no
     * se sabe nada.
     */
    public GlyphJustificationInfo getJustificationInfo() {
        float advance = this.getAdvance();
        return new GlyphJustificationInfo(advance, false,
                GlyphJustificationInfo.PRIORITY_INTERCHAR, advance / 3, advance / 3, false,
                GlyphJustificationInfo.PRIORITY_INTERCHAR, 0, 0);
    }
}
