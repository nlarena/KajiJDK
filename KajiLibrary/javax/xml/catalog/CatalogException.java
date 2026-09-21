package javax.xml.catalog;

/**
 * KajiLibrary's javax.xml.catalog.CatalogException -- resolution through a catalog failed.
 *
 * <p>It is <b>unchecked</b>, and that decision explains how the package is used: a malformed
 * catalog or an entry not found in strict mode are configuration errors, not conditions a program
 * should handle on every call.
 *
 * <p>It shows up above all with {@link CatalogResolver.NotFoundAction#STRICT}, which is the default
 * mode: there, not finding an entry is an error and not a "carry on without it".
 */
public class CatalogException extends RuntimeException {

    private static final long serialVersionUID = 653231525876459057L;

    /** @param message what happened */
    public CatalogException(String message) {
        super(message);
    }

    /**
     * @param message what happened
     * @param cause the original, typically a parse or input/output error
     */
    public CatalogException(String message, Throwable cause) {
        super(message, cause);
    }
}
