package jakarta.validation.constraintvalidation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
// KajiLibrary's jakarta.validation.constraintvalidation.SupportedValidationTarget — the targets a
// ConstraintValidator supports.
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface SupportedValidationTarget {
    ValidationTarget[] value();
}
