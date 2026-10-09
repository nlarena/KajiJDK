package jakarta.validation.valueextraction;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
// KajiLibrary's jakarta.validation.valueextraction.UnwrapByDefault — a ValueExtractor so annotated
// unwraps its value unless the constraint opts out.
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UnwrapByDefault {
}
