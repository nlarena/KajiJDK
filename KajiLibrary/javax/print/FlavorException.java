package javax.print;

/**
 * KajiLibrary's javax.print.FlavorException -- el fallo fue por el formato del documento.
 *
 * <p>Una interfaz; ver la nota de {@link PrintException}. La implementa la excepcion que se lanza
 * cuando el {@link DocFlavor} del documento no esta entre los que la impresora acepta.
 */
public interface FlavorException {

    /** Los formatos que no acepta. */
    DocFlavor[] getUnsupportedFlavors();
}
