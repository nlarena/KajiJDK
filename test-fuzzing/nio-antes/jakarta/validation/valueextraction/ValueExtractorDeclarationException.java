package jakarta.validation.valueextraction;
import jakarta.validation.ValidationException;
// KajiLibrary's jakarta.validation.valueextraction.ValueExtractorDeclarationException.
public class ValueExtractorDeclarationException extends ValidationException {
    public ValueExtractorDeclarationException() {
    }
    public ValueExtractorDeclarationException(String message) {
        super(message);
    }
    public ValueExtractorDeclarationException(String message, Throwable cause) {
        super(message, cause);
    }
    public ValueExtractorDeclarationException(Throwable cause) {
        super(cause);
    }
}
