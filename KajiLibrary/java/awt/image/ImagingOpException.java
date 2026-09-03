package java.awt.image;

/**
 * Una operacion sobre una imagen no se pudo aplicar.
 *
 * <p>La distincion con {@link RasterFormatException} es de **quien** esta mal: alli los parametros
 * del raster, aca la operacion misma -- un filtro que no sabe tratar ese tipo de imagen, una
 * transformacion que no se puede invertir.
 */
public class ImagingOpException extends RuntimeException {

    private static final long serialVersionUID = 8026288481846276658L;

    /** Con ese mensaje. */
    public ImagingOpException(String s) {
        super(s);
    }
}
