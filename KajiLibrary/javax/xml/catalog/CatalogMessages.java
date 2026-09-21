package javax.xml.catalog;

/**
 * The error messages of this package.
 *
 * <p>Package access: it is not API. The {@code JAXP090200xx} codes are the JDK's and are kept as
 * they are, because there are tools and tests that look for them in the text.
 */
final class CatalogMessages {

    private CatalogMessages() {
    }

    /** The one for an invalid value of a feature. */
    static IllegalArgumentException invalidArgument(String value, String feature) {
        return new IllegalArgumentException("JAXP09020005: The specified argument '" + value
            + "' (case sensitive) for '" + feature + "' is not valid.");
    }

    /** The one for a null argument where it is not admitted. */
    static NullPointerException nullArgument(String name) {
        return new NullPointerException(
            "JAXP09020006: The argument '" + name + "' can not be null.");
    }

    /** The one for an entry not found in strict mode. */
    static CatalogException noMatch(String publicId, String systemId) {
        return new CatalogException("JAXP09040001: No match found for publicId '" + publicId
            + "' and systemId '" + systemId + "'.");
    }
}
