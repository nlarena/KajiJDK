package javax.xml.catalog;

import java.util.stream.Stream;

/**
 * Un catalogo sin entradas.
 *
 * <p>De acceso de paquete: no es API. Es lo que {@link CatalogManager} devuelve cuando no hay archivos
 * que leer, y es un catalogo perfectamente valido -- uno vacio no coincide con nada, que es
 * exactamente lo que dice.
 *
 * <p>Guarda las caracteristicas con las que se pidio porque el resolutor necesita la de
 * {@code RESOLVE} para saber que hacer cuando no hay coincidencia, que aca es siempre.
 */
final class EmptyCatalog implements Catalog {

    /** Con que se pidio. */
    private final CatalogFeatures features;

    EmptyCatalog(CatalogFeatures features) {
        this.features = features;
    }

    /** Siempre null: no hay entradas. */
    public String matchSystem(String systemId) {
        return null;
    }

    /** Siempre null. */
    public String matchPublic(String publicId) {
        return null;
    }

    /** Siempre null. */
    public String matchURI(String uri) {
        return null;
    }

    /** Vacio: no hay catalogos encadenados. */
    public Stream<Catalog> catalogs() {
        return Stream.empty();
    }

    /** Que hacer sin coincidencia, segun las caracteristicas con las que se pidio. */
    CatalogResolver.NotFoundAction action() {
        String resolve = this.features.get(CatalogFeatures.Feature.RESOLVE);
        if (resolve == null) {
            return CatalogResolver.NotFoundAction.STRICT;
        }
        return CatalogResolver.NotFoundAction.getType(resolve);
    }
}
