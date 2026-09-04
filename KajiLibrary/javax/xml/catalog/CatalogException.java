package javax.xml.catalog;

/**
 * KajiLibrary's javax.xml.catalog.CatalogException -- fallo la resolucion por catalogo.
 *
 * <p>Es <b>no comprobada</b>, y esa decision explica como se usa el paquete: un catalogo mal formado o
 * una entrada que no se encuentra en modo estricto son errores de configuracion, no condiciones que un
 * programa deba manejar en cada llamada.
 *
 * <p>Aparece sobre todo con {@link CatalogResolver.NotFoundAction#STRICT}, que es el modo por omision:
 * ahi, no encontrar una entrada es un error y no un "seguí sin ella".
 */
public class CatalogException extends RuntimeException {

    private static final long serialVersionUID = 653231525876459057L;

    /** @param message que paso */
    public CatalogException(String message) {
        super(message);
    }

    /**
     * @param message que paso
     * @param cause la original, tipicamente un error de analisis o de entrada/salida
     */
    public CatalogException(String message, Throwable cause) {
        super(message, cause);
    }
}
