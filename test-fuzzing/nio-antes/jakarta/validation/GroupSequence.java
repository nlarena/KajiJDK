package jakarta.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// KajiLibrary's jakarta.validation.GroupSequence — defines an ordered sequence of validation groups.
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GroupSequence {

    Class<?>[] value();
}
