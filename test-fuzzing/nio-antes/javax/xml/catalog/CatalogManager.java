package javax.xml.catalog;

import java.net.URI;

/**
 * KajiLibrary's javax.xml.catalog.CatalogManager -- where catalogs and resolvers come from.
 *
 * <p>Four static methods and no public constructor: it is the package's only entrance. {@link
 * Catalog} and {@link CatalogResolver} are interfaces without a public implementation precisely so
 * that one goes through here.
 *
 * <h2>The two routes to a resolver</h2>
 *
 * <p>{@link #catalogResolver(CatalogFeatures, URI...)} builds the catalog and the resolver at once;
 * {@link #catalogResolver(Catalog)} wraps one that already exists. The second matters when the same
 * catalog is used from several resolvers: reading it once and sharing it avoids rereading the
 * files.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Reading a catalog file needs an XML parser, and this library does not come with one. What is
 * implemented --and it is most of what a program observes-- is:
 *
 * <ul>
 *   <li>the whole of {@link CatalogFeatures}, with its validation, its system properties and its
 *       default values;
 *   <li>the <b>empty</b> catalog, which matches nothing and is a perfectly valid catalog;
 *   <li>the whole resolver, with the three {@link CatalogResolver.NotFoundAction}s really
 *       implemented. Since the catalog never matches, it is exactly that path that gets exercised.
 * </ul>
 *
 * <p>Asking for a catalog <b>with files</b> throws {@link CatalogException} saying the parser is
 * missing. It is thrown when building it and not later, even if {@code DEFER} is {@code true}:
 * deferred reading exists so as not to pay for catalogs that are not used, and here the result
 * would be the same error later and further from the place that caused it.
 */
public final class CatalogManager {

    /** Not instantiated. */
    private CatalogManager() {
    }

    /**
     * A catalog with those features and those files.
     *
     * @throws NullPointerException if the features are null
     * @throws CatalogException if there are files to read; see the class note
     * @throws IllegalArgumentException if some URI is not absolute
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
     * A resolver over that catalog, with the action its {@code RESOLVE} feature says.
     *
     * @throws NullPointerException if the catalog is null
     */
    public static CatalogResolver catalogResolver(Catalog catalog) {
        if (catalog == null) {
            throw CatalogMessages.nullArgument("catalog");
        }
        return new CatalogResolverImpl(catalog, actionOf(catalog));
    }

    /**
     * Likewise, forcing the action.
     *
     * @throws NullPointerException if either of the two is null
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
     * The catalog and the resolver at once.
     *
     * @throws NullPointerException if the features are null
     * @throws CatalogException if there are files to read; see the class note
     */
    public static CatalogResolver catalogResolver(CatalogFeatures features, URI... paths) {
        return catalogResolver(catalog(features, paths));
    }

    /** The action the catalog's features asked for, or the default one. */
    private static CatalogResolver.NotFoundAction actionOf(Catalog catalog) {
        if (catalog instanceof EmptyCatalog) {
            return ((EmptyCatalog) catalog).action();
        }
        return CatalogResolver.NotFoundAction.STRICT;
    }
}
