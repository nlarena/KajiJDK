package jakarta.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// KajiLibrary's jakarta.validation.Constraint — marks an annotation as a validation constraint and
// names the ConstraintValidator implementations that enforce it.
@Documented
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Constraint {

    Class<? extends ConstraintValidator<?, ?>>[] validatedBy();
}
