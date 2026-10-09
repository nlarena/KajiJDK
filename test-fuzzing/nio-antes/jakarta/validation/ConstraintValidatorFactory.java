package jakarta.validation;

// KajiLibrary's jakarta.validation.ConstraintValidatorFactory — instantiates ConstraintValidator
// implementations.
public interface ConstraintValidatorFactory {

    <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key);

    void releaseInstance(ConstraintValidator<?, ?> instance);
}
