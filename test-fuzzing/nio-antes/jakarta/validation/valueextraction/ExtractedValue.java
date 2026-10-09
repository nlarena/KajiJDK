package jakarta.validation.valueextraction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// KajiLibrary's jakarta.validation.valueextraction.ExtractedValue — marks the extracted value's type.
// NB: the JDK's type() defaults to void.class; the frozen javac can't parse `void.class`, so the
// default is omitted here (the element descriptor is unchanged).
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExtractedValue {
    Class<?> type();
}
