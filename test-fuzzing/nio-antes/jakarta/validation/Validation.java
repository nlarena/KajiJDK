package jakarta.validation;
import jakarta.validation.bootstrap.GenericBootstrap;
import jakarta.validation.bootstrap.ProviderSpecificBootstrap;
// KajiLibrary's jakarta.validation.Validation — the static bootstrap entry point. A KajiLibrary subset:
// with no bundled Bean Validation engine, the factory-building methods raise NoProviderFoundException.
public class Validation {
    private Validation() {
    }
    public static ValidatorFactory buildDefaultValidatorFactory() {
        throw new NoProviderFoundException("KajiLibrary bundles no Bean Validation engine");
    }
    public static GenericBootstrap byDefaultProvider() {
        throw new NoProviderFoundException("KajiLibrary bundles no Bean Validation engine");
    }
    public static ProviderSpecificBootstrap byProvider(Class<?> providerType) {
        throw new NoProviderFoundException("KajiLibrary bundles no Bean Validation engine");
    }
}
