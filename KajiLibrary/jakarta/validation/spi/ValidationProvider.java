package jakarta.validation.spi;
import jakarta.validation.Configuration;
import jakarta.validation.ValidatorFactory;
// KajiLibrary's jakarta.validation.spi.ValidationProvider — a Bean Validation provider. Modelled raw
// (JDK: ValidationProvider<T extends Configuration<T>>) to avoid the #100 erasure bug.
public interface ValidationProvider {
    Configuration createSpecializedConfiguration(BootstrapState state);
    Configuration createGenericConfiguration(BootstrapState state);
    ValidatorFactory buildValidatorFactory(ConfigurationState configurationState);
}
