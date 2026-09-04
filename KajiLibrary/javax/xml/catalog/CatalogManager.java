package javax.xml.catalog;

import java.net.URI;

/**
 * KajiLibrary's javax.xml.catalog.CatalogManager -- de donde salen los catalogos y los resolutores.
 *
 * <p>Cuatro metodos estaticos y ningun constructor publico: es la unica puerta de entrada del paquete.
 * {@link Catalog} y {@link CatalogResolver} son interfaces sin implementacion publica justamente para
 * que se pase por aca.
 *
 * <h2>Los dos caminos hacia un resolutor</h2>
 *
 * <p>{@link #catalogResolver(CatalogFeatures, URI...)} arma el catalogo y el resolutor de una;
 * {@link #catalogResolver(Catalog)} envuelve uno que ya existe. El segundo importa cuando el mismo
 * catalogo se usa desde varios resolutores: leerlo una vez y compartirlo evita releer los archivos.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Leer un archivo de catalogo pide un analizador XML, y esta biblioteca no trae uno. Lo que si esta
 * implementado --y es la mayor parte de lo que un programa observa-- es:
 *
 * <ul>
 *   <li>{@link CatalogFeatures} entero, con su validacion, sus propiedades del sistema y sus valores
 *       de omision;
 *   <li>el catalogo <b>vacio</b>, que no coincide con nada y es un catalogo perfectamente valido;
 *   <li>el resolutor entero, con las tres {@link CatalogResolver.NotFoundAction} implementadas de
 *       verdad. Como el catalogo nunca coincide, es exactamente ese camino el que se ejercita.
 * </ul>
 *
 * <p>Pedir un catalogo <b>con archivos</b> lanza {@link CatalogException} diciendo que falta el
 * analizador. Se lanza al momento de armarlo y no despues, aunque {@code DEFER} sea {@code true}: la
 * lectura diferida existe para no pagar por catalogos que no se usan, y aca el resultado seria el
 * mismo error mas tarde y mas lejos del sitio que lo causo.
 */
public final class CatalogManager {

    /** No se instancia. */
    private CatalogManager() {
    }

    /**
     * Un catalogo con esas caracteristicas y esos archivos.
     *
     * @throws NullPointerException si las caracteristicas son null
     * @throws CatalogException si hay archivos que leer; ver la nota de la clase
     * @throws IllegalArgumentException si algun URI no es absoluto
     */
    public static Catalog catalog(CatalogFeatures features, URI... paths) {
        if (features == null) {
            throw CatalogMessages.nullArgument("CatalogFeatures");
        }
        int count = 0;
        if (paths != null) {
            int i = 0;
            while (i < paths.length) {
                if (paths[i] == null) {
                    throw CatalogMessages.nullArgument("URI");
                }
                if (!paths[i].isAbsolute()) {
                    throw new IllegalArgumentException(
                        "JAXP09030001: The URI '" + paths[i] + "' is not absolute.");
                }
                count = count + 1;
                i = i + 1;
            }
        }
        String files = features.get(CatalogFeatures.Feature.FILES);
        if (files != null && files.trim().length() > 0) {
            count = count + 1;
        }
        if (count > 0) {
            throw new CatalogException(
                "no XML catalog parser in this library: cannot read catalog files");
        }
        return new EmptyCatalog(features);
    }

    /**
     * Un resolutor sobre ese catalogo, con la accion que diga su caracteristica {@code RESOLVE}.
     *
     * @throws NullPointerException si el catalogo es null
     */
    public static CatalogResolver catalogResolver(Catalog catalog) {
        if (catalog == null) {
            throw CatalogMessages.nullArgument("catalog");
        }
        return new CatalogResolverImpl(catalog, actionOf(catalog));
    }

    /**
     * Idem, forzando la accion.
     *
     * @throws NullPointerException si alguno de los dos es null
     */
    public static CatalogResolver catalogResolver(Catalog catalog,
                                                  CatalogResolver.NotFoundAction action) {
        if (catalog == null) {
            throw CatalogMessages.nullArgument("catalog");
        }
        if (action == null) {
            throw CatalogMessages.nullArgument("action");
        }
        return new CatalogResolverImpl(catalog, action);
    }

    /**
     * El catalogo y el resolutor de una.
     *
     * @throws NullPointerException si las caracteristicas son null
     * @throws CatalogException si hay archivos que leer; ver la nota de la clase
     */
    public static CatalogResolver catalogResolver(CatalogFeatures features, URI... paths) {
        return catalogResolver(catalog(features, paths));
    }

    /** La accion que pidieron las caracteristicas del catalogo, o la de omision. */
    private static CatalogResolver.NotFoundAction actionOf(Catalog catalog) {
        if (catalog instanceof EmptyCatalog) {
            return ((EmptyCatalog) catalog).action();
        }
        return CatalogResolver.NotFoundAction.STRICT;
    }
}
