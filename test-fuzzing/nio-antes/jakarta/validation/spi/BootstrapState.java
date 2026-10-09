package jakarta.validation.spi;
import jakarta.validation.ValidationProviderResolver;
// KajiLibrary's jakarta.validation.spi.BootstrapState — provider-resolver state during bootstrap.
public interface BootstrapState {
    ValidationProviderResolver getValidationProviderResolver();
    ValidationProviderResolver getDefaultValidationProviderResolver();
}
