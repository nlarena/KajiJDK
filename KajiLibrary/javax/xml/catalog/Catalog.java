package javax.xml.catalog;

import java.util.stream.Stream;

/**
 * KajiLibrary's javax.xml.catalog.Catalog -- una tabla que traduce identificadores en direcciones
 * locales.
 *
 * <p>Resuelve el problema clasico de XML: un documento dice que su DTD esta en
 * {@code http://ejemplo.com/foo.dtd}, y no se quiere que cada analisis salga a internet a buscarla --ni
 * que falle cuando no hay red, ni que un tercero decida que hay del otro lado--. Un catalogo dice
 * "ese identificador significa este archivo de aca".
 *
 * <h2>Los tres tipos de coincidencia</h2>
 *
 * <ul>
 *   <li>{@link #matchSystem} por identificador de <b>sistema</b>: el URL literal que trae el
 *       documento;
 *   <li>{@link #matchPublic} por identificador <b>publico</b>: el nombre formal, del estilo
 *       {@code -//W3C//DTD XHTML 1.0//EN};
 *   <li>{@link #matchURI} para referencias por URI, que es lo que usan XSLT y los esquemas.
 * </ul>
 *
 * <p>Cual gana cuando los dos coinciden lo decide la caracteristica {@code PREFER}; ver
 * {@link CatalogFeatures}.
 *
 * <p>Los tres devuelven <b>null</b> cuando no hay coincidencia. Es el {@link CatalogResolver} el que
 * decide que hacer con eso, no el catalogo.
 *
 * <p>{@link #catalogs} devuelve los catalogos alternativos y los siguientes -- un catalogo puede
 * apuntar a otros, y asi se arma una cadena.
 */
public interface Catalog {

    /** La direccion local para ese identificador de sistema, o null. */
    String matchSystem(String systemId);

    /** La direccion local para ese identificador publico, o null. */
    String matchPublic(String publicId);

    /** La direccion local para ese URI, o null. */
    String matchURI(String uri);

    /** Los catalogos encadenados a este. */
    Stream<Catalog> catalogs();
}
