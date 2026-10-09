package jakarta.validation.spi;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.TraversableResolver;
import jakarta.validation.valueextraction.ValueExtractor;
// KajiLibrary's jakarta.validation.spi.ConfigurationState — the resolved configuration a provider
// builds a ValidatorFactory from.
public interface ConfigurationState {
    boolean isIgnoreXmlConfiguration();
    MessageInterpolator getMessageInterpolator();
    Set<InputStream> getMappingStreams();
    Set<ValueExtractor<?>> getValueExtractors();
    ConstraintValidatorFactory getConstraintValidatorFactory();
    TraversableResolver getTraversableResolver();
    ParameterNameProvider getParameterNameProvider();
    ClockProvider getClockProvider();
    Map<String, String> getProperties();
}
