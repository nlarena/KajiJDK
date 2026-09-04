package javax.xml.catalog;

/**
 * Los mensajes de error de este paquete.
 *
 * <p>De acceso de paquete: no es API. Los codigos {@code JAXP090200xx} son los del JDK y se conservan
 * tal cual, porque hay herramientas y pruebas que los buscan en el texto.
 */
final class CatalogMessages {

    private CatalogMessages() {
    }

    /** El de un valor invalido para una caracteristica. */
    static IllegalArgumentException invalidArgument(String value, String feature) {
        return new IllegalArgumentException("JAXP09020005: The specified argument '" + value
            + "' (case sensitive) for '" + feature + "' is not valid.");
    }

    /** El de un argumento nulo donde no se admite. */
    static NullPointerException nullArgument(String name) {
        return new NullPointerException(
            "JAXP09020006: The argument '" + name + "' can not be null.");
    }

    /** El de una entrada que no se encontro en modo estricto. */
    static CatalogException noMatch(String publicId, String systemId) {
        return new CatalogException("JAXP09040001: No match found for publicId '" + publicId
            + "' and systemId '" + systemId + "'.");
    }
}
