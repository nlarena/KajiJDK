package jakarta.validation;
import java.util.Map;
import java.util.Set;
import jakarta.validation.executable.ExecutableType;
// KajiLibrary's jakarta.validation.BootstrapConfiguration — the XML/META-INF bootstrap settings.
public interface BootstrapConfiguration {
    String getDefaultProviderClassName();
    String getConstraintValidatorFactoryClassName();
    String getMessageInterpolatorClassName();
    String getTraversableResolverClassName();
    String getParameterNameProviderClassName();
    String getClockProviderClassName();
    Set<String> getValueExtractorClassNames();
    Set<String> getConstraintMappingResourcePaths();
    boolean isExecutableValidationEnabled();
    Set<ExecutableType> getDefaultValidatedExecutableTypes();
    Map<String, String> getProperties();
}
