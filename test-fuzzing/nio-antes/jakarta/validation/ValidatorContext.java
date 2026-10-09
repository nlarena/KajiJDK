package jakarta.validation;
import jakarta.validation.valueextraction.ValueExtractor;
// KajiLibrary's jakarta.validation.ValidatorContext — a fluent builder for a customized Validator.
public interface ValidatorContext {
    ValidatorContext messageInterpolator(MessageInterpolator messageInterpolator);
    ValidatorContext traversableResolver(TraversableResolver traversableResolver);
    ValidatorContext constraintValidatorFactory(ConstraintValidatorFactory factory);
    ValidatorContext parameterNameProvider(ParameterNameProvider parameterNameProvider);
    ValidatorContext clockProvider(ClockProvider clockProvider);
    ValidatorContext addValueExtractor(ValueExtractor<?> extractor);
    Validator getValidator();
}
