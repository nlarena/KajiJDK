package javax.xml.catalog;

import java.util.stream.Stream;

/**
 * A catalog without entries.
 *
 * <p>Package access: it is not API. It is what {@link CatalogManager} returns when there are no
 * files to read, and it is a perfectly valid catalog -- an empty one matches nothing, which is
 * exactly what it says.
 *
 * <p>It keeps the features it was asked for with because the resolver needs the {@code RESOLVE} one
 * to know what to do when there is no match, which here is always.
 */
final class EmptyCatalog implements Catalog {

    /** What it was asked for with. */
    private final CatalogFeatures features;

    EmptyCatalog(CatalogFeatures features) {
        this.features = features;
    }

    /** Always null: there are no entries. */
    public String matchSystem(String systemId) {
        return null;
    }

    /** Always null. */
    public String matchPublic(String publicId) {
        return null;
    }

    /** Always null. */
    public String matchURI(String uri) {
        return null;
    }

    /** Empty: there are no chained catalogs. */
    public Stream<Catalog> catalogs() {
        return Stream.empty();
    }

    /** What to do without a match, according to the features it was asked for with. */
    CatalogResolver.NotFoundAction action() {
        String resolve = this.features.get(CatalogFeatures.Feature.RESOLVE);
        if (resolve == null) {
            return CatalogResolver.NotFoundAction.STRICT;
        }
        return CatalogResolver.NotFoundAction.getType(resolve);
    }
}
