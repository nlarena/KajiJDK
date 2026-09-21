package javax.xml.validation;

/**
 * KajiLibrary's javax.xml.validation.SchemaFactoryConfigurationError -- there is no schema factory.
 *
 * <p>An {@link Error}, like {@code javax.xml.parsers}' {@code FactoryConfigurationError} and for
 * the same reason: the implementation named in the configuration does not exist or could not be
 * loaded, and there is nothing a local {@code catch} can do.
 *
 * <p>What did change is the form: here the cause is a {@link Throwable} and goes through {@code
 * Throwable}'s normal mechanism, without a field of its own nor {@code getException}. It is the
 * clean version -- this class arrived in Java 8, fourteen years after the other, when chained
 * causes had long existed.
 */
public final class SchemaFactoryConfigurationError extends Error {

    private static final long serialVersionUID = 3531438703147750126L;

    /** Without detail. */
    public SchemaFactoryConfigurationError() {
        super();
    }

    /** With a message. */
    public SchemaFactoryConfigurationError(String message) {
        super(message);
    }

    /** Wrapping what really failed. */
    public SchemaFactoryConfigurationError(Throwable cause) {
        super(cause);
    }

    /** With both things. */
    public SchemaFactoryConfigurationError(String message, Throwable cause) {
        super(message, cause);
    }
}
