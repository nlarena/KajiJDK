package jakarta.validation;
import java.util.List;
import jakarta.validation.spi.ValidationProvider;
// KajiLibrary's jakarta.validation.ValidationProviderResolver — discovers the available providers.
public interface ValidationProviderResolver {
    List<ValidationProvider> getValidationProviders();
}
