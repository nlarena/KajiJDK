package jakarta.validation;

import java.lang.annotation.Annotation;

// KajiLibrary's jakarta.validation.ConstraintValidator — the logic validating a given constraint A for
// a value of type T. initialize() receives the annotation instance; isValid() performs the check.
public interface ConstraintValidator<A extends Annotation, T> {

    default void initialize(A constraintAnnotation) {
    }

    boolean isValid(T value, ConstraintValidatorContext context);
}
