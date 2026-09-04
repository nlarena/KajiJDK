package java.awt.font;

/**
 * Las medidas verticales de una línea de texto.
 *
 * <p>Todo lo que hace falta para apilar renglones sin que se pisen: cuánto sube el texto sobre la
 * línea de base, cuánto baja, y cuánto aire va entre un renglón y el siguiente. La suma de las tres
 * es {@link #getHeight}.
 *
 * <p>Las líneas de base en plural no son un capricho: un texto que mezcla alfabetos las necesita.
 * Un carácter latino se apoya sobre la base romana, uno devanagari cuelga de una barra superior, y
 * uno ideográfico se centra; {@link #getBaselineOffsets} da la distancia entre ellas para que los
 * tres queden alineados en el mismo renglón.
 */
public abstract class LineMetrics {

    /** Para las subclases. */
    protected LineMetrics() {
    }

    /** Cuántos caracteres se midieron. */
    public abstract int getNumChars();

    /** Cuánto sube el texto por encima de la línea de base. */
    public abstract float getAscent();

    /** Cuánto baja el texto por debajo de la línea de base. */
    public abstract float getDescent();

    /** El aire entre el fondo de un renglón y el techo del siguiente. */
    public abstract float getLeading();

    /** La suma de las tres anteriores. */
    public abstract float getHeight();

    /** Cuál de las líneas de base usa este texto. */
    public abstract int getBaselineIndex();

    /** La distancia de cada línea de base a la que usa este texto. */
    public abstract float[] getBaselineOffsets();

    /** A qué altura va la línea de tachado. */
    public abstract float getStrikethroughOffset();

    /** Qué grosor tiene la línea de tachado. */
    public abstract float getStrikethroughThickness();

    /** A qué altura va el subrayado. */
    public abstract float getUnderlineOffset();

    /** Qué grosor tiene el subrayado. */
    public abstract float getUnderlineThickness();
}
