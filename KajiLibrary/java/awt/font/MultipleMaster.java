package java.awt.font;

import java.awt.Font;

/**
 * Una fuente Multiple Master: la que se puede **interpolar** en vez de elegirse de una lista.
 *
 * <p>Una familia común trae la fina, la regular y la negrita como archivos separados. Una Multiple
 * Master trae los extremos y una regla para generar todo lo del medio: se pide grosor 1,37 y sale
 * una cara que no existía como archivo.
 *
 * <p>Los ejes son las magnitudes que se pueden pedir —grosor, ancho, tamaño óptico— y cada uno tiene
 * su rango. {@link #deriveMMFont(float[])} pide una cara por sus valores de eje.
 */
public interface MultipleMaster {

    /** Cuántos ejes tiene la fuente. */
    int getNumDesignAxes();

    /** Los pares mínimo y máximo de cada eje, en orden. */
    float[] getDesignAxisRanges();

    /** El valor de cada eje en la cara por omisión. */
    float[] getDesignAxisDefaults();

    /** El nombre de cada eje. */
    String[] getDesignAxisNames();

    /**
     * Una cara con esos valores de eje.
     *
     * @throws IllegalArgumentException si algún valor cae fuera de su rango
     */
    Font deriveMMFont(float[] axes);

    /**
     * Una cara pedida por magnitudes y no por ejes.
     *
     * <p>Es el atajo para cuando no se conocen los ejes de la fuente: se declara qué se quiere y la
     * fuente traduce a los ejes que tenga.
     */
    Font deriveMMFont(float[] glyphWidths, float avgStemWidth, float typicalCapHeight,
            float typicalXHeight, float italicAngle);
}
