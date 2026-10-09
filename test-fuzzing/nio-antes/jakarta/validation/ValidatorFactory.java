package jakarta.validation;
// KajiLibrary's jakarta.validation.ValidatorFactory — builds Validator instances; the bootstrap result.
public interface ValidatorFactory extends AutoCloseable {
    Validator getValidator();
    ValidatorContext usingContext();
    MessageInterpolator getMessageInterpolator();
    TraversableResolver getTraversableResolver();
    ConstraintValidatorFactory getConstraintValidatorFactory();
    ParameterNameProvider getParameterNameProvider();
    ClockProvider getClockProvider();
    <T> T unwrap(Class<T> type);
    void close();
}
