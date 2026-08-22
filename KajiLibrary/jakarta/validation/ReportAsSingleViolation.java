package jakarta.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// KajiLibrary's jakarta.validation.ReportAsSingleViolation — a composed constraint reports a single violation.
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReportAsSingleViolation {
}
