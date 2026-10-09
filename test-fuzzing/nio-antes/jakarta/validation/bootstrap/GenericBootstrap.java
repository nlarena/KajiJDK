package jakarta.validation.bootstrap;
import jakarta.validation.Configuration;
import jakarta.validation.ValidationProviderResolver;
// KajiLibrary's jakarta.validation.bootstrap.GenericBootstrap — default-provider bootstrap.
public interface GenericBootstrap {
    GenericBootstrap providerResolver(ValidationProviderResolver resolver);
    Configuration configure();
}
