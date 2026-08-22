package jakarta.validation.valueextraction;
import jakarta.validation.ValidationException;
// KajiLibrary's jakarta.validation.valueextraction.ValueExtractorDefinitionException.
public class ValueExtractorDefinitionException extends ValidationException {
    public ValueExtractorDefinitionException() {
    }
    public ValueExtractorDefinitionException(String message) {
        super(message);
    }
    public ValueExtractorDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
    public ValueExtractorDefinitionException(Throwable cause) {
        super(cause);
    }
}
