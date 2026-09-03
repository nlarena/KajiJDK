package java.awt.image;

/**
 * Un {@link Raster} con parametros invalidos: un rectangulo que no cae dentro, bandas que no
 * existen, un tamano negativo.
 *
 * <p>Es de ejecucion y no verificada, y tiene sentido que lo sea: casi siempre viene de un calculo
 * de coordenadas equivocado en el llamador, no de datos que hayan llegado de afuera.
 */
public class RasterFormatException extends RuntimeException {

    private static final long serialVersionUID = 96598996116164315L;

    /** Con ese mensaje. */
    public RasterFormatException(String s) {
        super(s);
    }
}
