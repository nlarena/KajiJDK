package jakarta.validation.bootstrap;
import jakarta.validation.Configuration;
import jakarta.validation.ValidationProviderResolver;
// KajiLibrary's jakarta.validation.bootstrap.ProviderSpecificBootstrap — provider-specific bootstrap.
// Modelled raw (JDK: <T extends Configuration<T>>) to avoid the #100 erasure bug.
public interface ProviderSpecificBootstrap {
    ProviderSpecificBootstrap providerResolver(ValidationProviderResolver resolver);
    Configuration configure();
}
