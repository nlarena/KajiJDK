package java.awt.font;

import java.text.AttributedCharacterIterator;

/**
 * Mide un párrafo por tramos, guardando el trabajo entre consultas.
 *
 * <p>Es la máquina de abajo de {@link LineBreakMeasurer}, y existe por una razón de costo: partir un
 * párrafo en renglones exige medirlo muchas veces —una por cada corte que se prueba— y volver a
 * medir desde cero cada vez sería cuadrático.
 *
 * <p>De ahí {@link #insertChar} y {@link #deleteChar}, que son la parte interesante. Cuando se
 * edita un párrafo, casi todo lo medido sigue valiendo: sólo cambia el tramo alrededor del cambio.
 * Estos dos métodos le avisan qué se tocó para que tire lo que dejó de valer y conserve el resto,
 * que es lo que hace que escribir en un texto largo no se ponga lento.
 *
 * <p><strong>No se puede construir.</strong> Medir un tramo es medir sus glifos, y eso necesita un
 * motor tipográfico que esta biblioteca no trae. Es la misma frontera de {@link TextLayout} y de
 * {@link java.awt.Font}.
 */
public final class TextMeasurer implements Cloneable {

    /** El mensaje único de todo lo que necesita medir glifos. */
    private static UnsupportedOperationException sinMotor(String metodo) {
        return new UnsupportedOperationException(metodo + " requiere medir los glifos de la "
                + "fuente; esta biblioteca no trae motor tipográfico");
    }

    /**
     * Con el párrafo y las condiciones de dibujo.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public TextMeasurer(AttributedCharacterIterator text, FontRenderContext frc) {
        throw sinMotor("TextMeasurer");
    }

    /**
     * Una copia con el trabajo ya hecho.
     *
     * @throws UnsupportedOperationException siempre
     */
    protected Object clone() {
        throw sinMotor("clone");
    }

    /**
     * Hasta dónde llega un renglón que empiece ahí y no pase de ese ancho.
     *
     * @throws UnsupportedOperationException siempre
     */
    public int getLineBreakIndex(int start, float maxAdvance) {
        throw sinMotor("getLineBreakIndex");
    }

    /**
     * Cuánto mide ese tramo.
     *
     * @throws UnsupportedOperationException siempre
     */
    public float getAdvanceBetween(int start, int limit) {
        throw sinMotor("getAdvanceBetween");
    }

    /**
     * El renglón armado de ese tramo.
     *
     * @throws UnsupportedOperationException siempre
     */
    public TextLayout getLayout(int start, int limit) {
        throw sinMotor("getLayout");
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
