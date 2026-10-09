package jakarta.validation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// KajiLibrary's jakarta.validation.OverridesAttribute — in a composed constraint, overrides an
// attribute of a composing constraint.
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface OverridesAttribute {

    Class<? extends Annotation> constraint();

    String name();

    int constraintIndex() default -1;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    @Documented
    public @interface List {
        OverridesAttribute[] value();
    }
}
