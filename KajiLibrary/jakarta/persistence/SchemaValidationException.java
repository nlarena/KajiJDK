package jakarta.persistence;

// Thrown by SchemaManager.validate to report schema mismatches, carrying the individual
// validation failures. A checked exception (extends java.lang.Exception). New in 3.2.
public class SchemaValidationException extends Exception {
    private final Exception[] failures;
    public SchemaValidationException(String message, Exception... failures) {
        super(message);
        this.failures = failures;
    }
    public Exception[] getFailures() {
        return failures;
    }
}
