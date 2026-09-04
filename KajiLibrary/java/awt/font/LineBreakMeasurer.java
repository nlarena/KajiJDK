package java.awt.font;

import java.text.AttributedCharacterIterator;
import java.text.BreakIterator;

/**
 * Parte un párrafo en renglones que entren en un ancho dado.
 *
 * <p>Se usa como un iterador con estado: cada {@link #nextLayout(float)} devuelve el renglón
 * siguiente y avanza la posición. Que el ancho se pase **en cada llamada** y no en el constructor no
 * es un descuido: es lo que permite maquetar alrededor de una figura, donde cada renglón tiene un
 * ancho distinto porque el hueco cambia de forma.
 *
 * <p>Dónde se puede cortar lo decide un {@link BreakIterator}, y por eso se puede dar el propio: en
 * la mayoría de los idiomas se corta en los espacios, pero en tailandés o en japonés no hay espacios
 * entre palabras y el corte necesita saber del idioma.
 *
 * <p><strong>No se puede construir.</strong> Para saber qué entra en un ancho hay que medir, y medir
 * necesita un motor tipográfico que esta biblioteca no trae. Es la misma frontera de
 * {@link TextLayout}, {@link TextMeasurer} y {@link java.awt.Font}.
 */
public final class LineBreakMeasurer {

    /** El mensaje único de todo lo que necesita medir glifos. */
    private static UnsupportedOperationException sinMotor(String metodo) {
        return new UnsupportedOperationException(metodo + " requiere medir los glifos de la "
                + "fuente; esta biblioteca no trae motor tipográfico");
    }

    /**
     * Con el párrafo y las condiciones de dibujo, cortando por palabras.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public LineBreakMeasurer(AttributedCharacterIterator text, FontRenderContext frc) {
        throw sinMotor("LineBreakMeasurer");
    }

    /**
     * Con el criterio de corte dado.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public LineBreakMeasurer(AttributedCharacterIterator text, BreakIterator breakIter,
            FontRenderContext frc) {
        throw sinMotor("LineBreakMeasurer");
    }

    /**
     * Hasta dónde llegaría el renglón siguiente, sin consumirlo.
     *
     * @throws UnsupportedOperationException siempre
     */
    public int nextOffset(float wrappingWidth) {
        throw sinMotor("nextOffset");
    }

    /**
     * Lo mismo, con un límite y con la opción de cortar en cualquier lado.
     *
     * @throws UnsupportedOperationException siempre
     */
    public int nextOffset(float wrappingWidth, int offsetLimit, boolean requireNextWord) {
        throw sinMotor("nextOffset");
    }

    /**
     * El renglón siguiente, y avanza la posición.
     *
     * @throws UnsupportedOperationException siempre
     */
    public TextLayout nextLayout(float wrappingWidth) {
        throw sinMotor("nextLayout");
    }

    /**
     * Lo mismo, con un límite y con la opción de cortar en cualquier lado.
     *
     * @throws UnsupportedOperationException siempre
     */
    public TextLayout nextLayout(float wrappingWidth, int offsetLimit, boolean requireNextWord) {
        throw sinMotor("nextLayout");
    }

    /**
     * Por dónde va.
     *
     * @throws UnsupportedOperationException siempre
     */
    public int getPosition() {
        throw sinMotor("getPosition");
    }

    /**
     * Mueve la posición.
     *
     * @throws UnsupportedOperationException siempre
     */
    public void setPosition(int newPosition) {
        throw sinMotor("setPosition");
    }

    /**
     * Avisa que se insertó un carácter en esa posición.
     *
     * @throws UnsupportedOperationException siempre
     */
    public void insertChar(AttributedCharacterIterator newParagraph, int insertPos) {
        throw sinMotor("insertChar");
    }

    /**
     * Avisa que se borró un carácter en esa posición.
     *
     * @throws UnsupportedOperationException siempre
     */
    public void deleteChar(AttributedCharacterIterator newParagraph, int deletePos) {
        throw sinMotor("deleteChar");
    }
}
