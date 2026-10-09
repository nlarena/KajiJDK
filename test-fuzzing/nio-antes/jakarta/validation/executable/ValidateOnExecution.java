package jakarta.validation.executable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
// KajiLibrary's jakarta.validation.executable.ValidateOnExecution — enables/disables executable validation.
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidateOnExecution {
    ExecutableType[] type() default {ExecutableType.IMPLICIT};
}
