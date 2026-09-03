package javax.xml.validation;

/**
 * KajiLibrary's javax.xml.validation.SchemaFactoryConfigurationError -- no hay fabrica de esquemas.
 *
 * <p>Un {@link Error}, igual que {@code FactoryConfigurationError} de {@code javax.xml.parsers} y por
 * la misma razon: la implementacion nombrada en la configuracion no existe o no se pudo cargar, y no
 * hay nada que un {@code catch} local pueda hacer.
 *
 * <p>Lo que si cambio es la forma: aca la causa es un {@link Throwable} y va por el mecanismo normal
 * de {@code Throwable}, sin campo propio ni {@code getException}. Es la version limpia -- esta clase
 * llego en Java 8, catorce anos despues que la otra, cuando las causas encadenadas ya existian desde
 * hacia rato.
 */
public final class SchemaFactoryConfigurationError extends Error {

    private static final long serialVersionUID = 3531438703147750126L;

    /** Sin detalle. */
    public SchemaFactoryConfigurationError() {
        super();
    }

    /** Con un mensaje. */
    public SchemaFactoryConfigurationError(String message) {
        super(message);
    }

    /** Envolviendo lo que fallo de verdad. */
    public SchemaFactoryConfigurationError(Throwable cause) {
        super(cause);
    }

    /** Con las dos cosas. */
    public SchemaFactoryConfigurationError(String message, Throwable cause) {
        super(message, cause);
    }
}
