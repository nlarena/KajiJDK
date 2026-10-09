package jakarta.validation;
import java.io.InputStream;
import jakarta.validation.valueextraction.ValueExtractor;
// KajiLibrary's jakarta.validation.Configuration — the fluent bootstrap builder. NB: the JDK type is
// self-bounded Configuration<T extends Configuration<T>>; KajiLibrary models it raw (methods return
// Configuration, matching the JDK's erased descriptors) to avoid the bounded-type-variable erasure
// bug (#100).
public interface Configuration {
    Configuration ignoreXmlConfiguration();
    Configuration messageInterpolator(MessageInterpolator interpolator);
    Configuration traversableResolver(TraversableResolver resolver);
    Configuration constraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory);
    Configuration parameterNameProvider(ParameterNameProvider parameterNameProvider);
    Configuration clockProvider(ClockProvider clockProvider);
    Configuration addValueExtractor(ValueExtractor<?> extractor);
    Configuration addMapping(InputStream stream);
    Configuration addProperty(String name, String value);
    MessageInterpolator getDefaultMessageInterpolator();
    TraversableResolver getDefaultTraversableResolver();
    ConstraintValidatorFactory getDefaultConstraintValidatorFactory();
    ParameterNameProvider getDefaultParameterNameProvider();
    ClockProvider getDefaultClockProvider();
    BootstrapConfiguration getBootstrapConfiguration();
    ValidatorFactory buildValidatorFactory();
}
