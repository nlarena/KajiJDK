package java.awt.font;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * Un renglón de texto ya armado: los glifos elegidos, ordenados y colocados.
 *
 * <p>Es la clase que resuelve todo lo que un renglón de texto tiene de difícil, y que no se nota
 * hasta que el texto no es inglés. Qué glifo le corresponde a cada carácter y cuáles se funden en
 * una ligadura; en qué orden van si el renglón mezcla direcciones; dónde cae el cursor cuando la
 * frontera entre dos caracteres cae en dos lugares distintos de la pantalla; qué se resalta cuando
 * se selecciona un tramo que en pantalla no es contiguo.
 *
 * <p>Esa última familia de métodos —{@code getCaretShapes}, {@code getVisualHighlightShape},
 * {@code getLogicalRangesForVisualSelection}— existe entera por el texto bidireccional. En un
 * renglón de una sola dirección serían triviales; en uno que mezcla árabe con latín, seleccionar
 * tres caracteres consecutivos puede pintar **dos** rectángulos separados, y arrastrar el cursor una
 * posición puede moverlo para el otro lado.
 *
 * <p><strong>No se puede construir.</strong> Armar un renglón exige medir cada glifo, y medir un
 * glifo exige leer el archivo de la fuente. Esta biblioteca no trae un motor tipográfico —la misma
 * frontera que parte a {@link Font} en dos mitades— así que los tres constructores tiran
 * `UnsupportedOperationException` con ese motivo. La clase está declarada entera para que compile lo
 * que la nombra, y no contesta nada que no pueda saber: un miembro que falta es un subconjunto
 * legal; uno que miente, no.
 */
public final class TextLayout implements Cloneable {

    /**
     * Cuál de los dos cursores posibles es el fuerte cuando una posición cae entre dos direcciones.
     *
     * <p>En un renglón bidireccional, una misma posición del texto tiene **dos** lugares en pantalla
     * donde podría ir el cursor. La política decide cuál se dibuja lleno y cuál se dibuja como
     * cursor débil, o no se dibuja.
     */
    public static class CaretPolicy {

        /** Uno que elige el cursor del tramo de mayor nivel de anidamiento bidireccional. */
        public CaretPolicy() {
        }

        /**
         * Cuál de los dos cursores es el fuerte.
         *
         * @throws UnsupportedOperationException siempre: la respuesta depende de los niveles
         *     bidireccionales del renglón, que sólo existen si el renglón se pudo armar
         */
        public TextHitInfo getStrongCaret(TextHitInfo hit1, TextHitInfo hit2,
                TextLayout layout) {
            throw sinMotor("getStrongCaret");
        }
    }

    /** La política que se usa si no se dice otra cosa. */
    public static final CaretPolicy DEFAULT_CARET_POLICY = new CaretPolicy();

    /** El mensaje único de todo lo que necesita medir glifos. */
    private static UnsupportedOperationException sinMotor(String metodo) {
        return new UnsupportedOperationException(metodo + " requiere armar el renglón, y armarlo "
                + "requiere medir los glifos de la fuente; esta biblioteca no trae motor "
                + "tipográfico");
    }

    /**
     * Un renglón con una sola fuente.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public TextLayout(String string, Font font, FontRenderContext frc) {
        throw sinMotor("TextLayout");
    }

    /**
     * Un renglón con los atributos dados.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public TextLayout(String string,
            Map<? extends AttributedCharacterIterator.Attribute, ?> attributes,
            FontRenderContext frc) {
        throw sinMotor("TextLayout");
    }

    /**
     * Un renglón a partir de un texto con atributos por tramo.
     *
     * @throws UnsupportedOperationException siempre: hace falta medir los glifos
     */
    public TextLayout(AttributedCharacterIterator text, FontRenderContext frc) {
        throw sinMotor("TextLayout");
    }

    /**
     * Una copia.
     *
     * @throws UnsupportedOperationException siempre
     */
    protected Object clone() {
        throw sinMotor("clone");
    }

    /**
     * El mismo renglón estirado a ese ancho.
     *
     * @throws UnsupportedOperationException siempre
     */
    public TextLayout getJustifiedLayout(float justificationWidth) {
        throw sinMotor("getJustifiedLayout");
    }

    /**
     * Reparte el sobrante entre los glifos del renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    protected void handleJustify(float justificationWidth) {
        throw sinMotor("handleJustify");
    }

    /**
     * Sobre qué línea de base se apoya el renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public byte getBaseline() {
        throw sinMotor("getBaseline");
    }

    /**
     * La distancia de cada línea de base a la del renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public float[] getBaselineOffsets() {
        throw sinMotor("getBaselineOffsets");
    }

    /**
     * Cuánto avanza el renglón entero.
     *
     * @throws UnsupportedOperationException siempre
     */
    public float getAdvance() {
        throw sinMotor("getAdvance");
    }

    /**
     * Cuánto avanza sin contar los espacios finales.
     *
     * @throws UnsupportedOperationException siempre
     */
    public float getVisibleAdvance() {
        throw sinMotor("getVisibleAdvance");
    }

    /**
     * Cuánto sube el renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public float getAscent() {
        throw sinMotor("getAscent");
    }

    /**
     * Cuánto baja el renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public float getDescent() {
        throw sinMotor("getDescent");
    }

    /**
     * El aire hasta el renglón siguiente.
     *
     * @throws UnsupportedOperationException siempre
     */
    public float getLeading() {
        throw sinMotor("getLeading");
    }

    /**
     * Dónde cae la tinta del renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Rectangle2D getBounds() {
        throw sinMotor("getBounds");
    }

    /**
     * Los píxeles que toca el renglón dibujado en `(x, y)`.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Rectangle getPixelBounds(FontRenderContext frc, float x, float y) {
        throw sinMotor("getPixelBounds");
    }

    /**
     * Si la dirección base del renglón es de izquierda a derecha.
     *
     * @throws UnsupportedOperationException siempre
     */
    public boolean isLeftToRight() {
        throw sinMotor("isLeftToRight");
    }

    /**
     * Si el renglón corre en vertical.
     *
     * @throws UnsupportedOperationException siempre
     */
    public boolean isVertical() {
        throw sinMotor("isVertical");
    }

    /**
     * Cuántos caracteres tiene el renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public int getCharacterCount() {
        throw sinMotor("getCharacterCount");
    }

    /**
     * Dónde y cómo dibujar el cursor en esa posición.
     *
     * @throws UnsupportedOperationException siempre
     */
    public float[] getCaretInfo(TextHitInfo hit, Rectangle2D bounds) {
        throw sinMotor("getCaretInfo");
    }

    /**
     * Lo mismo, con los límites del renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public float[] getCaretInfo(TextHitInfo hit) {
        throw sinMotor("getCaretInfo");
    }

    /**
     * La posición que queda a la derecha en pantalla.
     *
     * @throws UnsupportedOperationException siempre
     */
    public TextHitInfo getNextRightHit(TextHitInfo hit) {
        throw sinMotor("getNextRightHit");
    }

    /**
     * Lo mismo, con la política de cursor dada.
     *
     * @throws UnsupportedOperationException siempre
     */
    public TextHitInfo getNextRightHit(int offset, CaretPolicy policy) {
        throw sinMotor("getNextRightHit");
    }

    /**
     * Lo mismo, desde una posición de inserción.
     *
     * @throws UnsupportedOperationException siempre
     */
    public TextHitInfo getNextRightHit(int offset) {
        throw sinMotor("getNextRightHit");
    }

    /**
     * La posición que queda a la izquierda en pantalla.
     *
     * @throws UnsupportedOperationException siempre
     */
    public TextHitInfo getNextLeftHit(TextHitInfo hit) {
        throw sinMotor("getNextLeftHit");
    }

    /**
     * Lo mismo, con la política de cursor dada.
     *
     * @throws UnsupportedOperationException siempre
     */
    public TextHitInfo getNextLeftHit(int offset, CaretPolicy policy) {
        throw sinMotor("getNextLeftHit");
    }

    /**
     * Lo mismo, desde una posición de inserción.
     *
     * @throws UnsupportedOperationException siempre
     */
    public TextHitInfo getNextLeftHit(int offset) {
        throw sinMotor("getNextLeftHit");
    }

    /**
     * La otra manera de nombrar la misma frontera, en coordenadas de pantalla.
     *
     * @throws UnsupportedOperationException siempre
     */
    public TextHitInfo getVisualOtherHit(TextHitInfo hit) {
        throw sinMotor("getVisualOtherHit");
    }

    /**
     * La figura del cursor en esa posición.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Shape getCaretShape(TextHitInfo hit, Rectangle2D bounds) {
        throw sinMotor("getCaretShape");
    }

    /**
     * Lo mismo, con los límites del renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Shape getCaretShape(TextHitInfo hit) {
        throw sinMotor("getCaretShape");
    }

    /**
     * El nivel de anidamiento bidireccional de ese carácter.
     *
     * @throws UnsupportedOperationException siempre
     */
    public byte getCharacterLevel(int index) {
        throw sinMotor("getCharacterLevel");
    }

    /**
     * Las figuras de los dos cursores posibles en esa posición.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Shape[] getCaretShapes(int offset, Rectangle2D bounds, CaretPolicy policy) {
        throw sinMotor("getCaretShapes");
    }

    /**
     * Lo mismo, con la política por omisión.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Shape[] getCaretShapes(int offset, Rectangle2D bounds) {
        throw sinMotor("getCaretShapes");
    }

    /**
     * Lo mismo, con los límites del renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Shape[] getCaretShapes(int offset) {
        throw sinMotor("getCaretShapes");
    }

    /**
     * Qué tramos del texto quedan seleccionados por una selección hecha en pantalla.
     *
     * <p>Devuelve varios pares porque en un renglón bidireccional una selección contigua en pantalla
     * puede corresponder a tramos separados del texto.
     *
     * @throws UnsupportedOperationException siempre
     */
    public int[] getLogicalRangesForVisualSelection(TextHitInfo firstEndpoint,
            TextHitInfo secondEndpoint) {
        throw sinMotor("getLogicalRangesForVisualSelection");
    }

    /**
     * La figura a resaltar para una selección hecha en pantalla.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Shape getVisualHighlightShape(TextHitInfo firstEndpoint, TextHitInfo secondEndpoint,
            Rectangle2D bounds) {
        throw sinMotor("getVisualHighlightShape");
    }

    /**
     * Lo mismo, con los límites del renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Shape getVisualHighlightShape(TextHitInfo firstEndpoint, TextHitInfo secondEndpoint) {
        throw sinMotor("getVisualHighlightShape");
    }

    /**
     * La figura a resaltar para un tramo del texto.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Shape getLogicalHighlightShape(int firstEndpoint, int secondEndpoint,
            Rectangle2D bounds) {
        throw sinMotor("getLogicalHighlightShape");
    }

    /**
     * Lo mismo, con los límites del renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Shape getLogicalHighlightShape(int firstEndpoint, int secondEndpoint) {
        throw sinMotor("getLogicalHighlightShape");
    }

    /**
     * La tinta de un tramo del texto.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Shape getBlackBoxBounds(int firstEndpoint, int secondEndpoint) {
        throw sinMotor("getBlackBoxBounds");
    }

    /**
     * Qué carácter cae en ese punto de la pantalla.
     *
     * @throws UnsupportedOperationException siempre
     */
    public TextHitInfo hitTestChar(float x, float y, Rectangle2D bounds) {
        throw sinMotor("hitTestChar");
    }

    /**
     * Lo mismo, con los límites del renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public TextHitInfo hitTestChar(float x, float y) {
        throw sinMotor("hitTestChar");
    }

    /**
     * Igualdad con otro renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public boolean equals(TextLayout rhs) {
        throw sinMotor("equals");
    }

    public String toString() {
        return "java.awt.font.TextLayout[sin motor tipográfico]";
    }

    /**
     * Dibuja el renglón con el comienzo de la línea de base en `(x, y)`.
     *
     * @throws UnsupportedOperationException siempre
     */
    public void draw(Graphics2D g2, float x, float y) {
        throw sinMotor("draw");
    }

    /**
     * El contorno del renglón entero.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Shape getOutline(AffineTransform tx) {
        throw sinMotor("getOutline");
    }

    /**
     * El camino sobre el que se apoya el renglón.
     *
     * @throws UnsupportedOperationException siempre
     */
    public LayoutPath getLayoutPath() {
        throw sinMotor("getLayoutPath");
    }

    /**
     * Dónde cae en pantalla esa posición del texto.
     *
     * @throws UnsupportedOperationException siempre
     */
    public void hitToPoint(TextHitInfo hit, Point2D point) {
        throw sinMotor("hitToPoint");
    }
}
