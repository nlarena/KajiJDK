package javax.annotation.processing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// The options (`-Akey=value`) a processor declares it understands.
// `AbstractProcessor.getSupportedOptions()` reads it by reflection.
//
// WARNING — in this VM this annotation CANNOT be read at run time. Our javac does not emit
// `RuntimeVisibleAnnotations` when the annotation's type resolves from the **classpath** (see
// `AbstractProcessor`'s header), which is always the case for a user's processor. The declaration is
// correct and serves as documentation and for the real javac; `getSupportedOptions()` will return the
// empty set until the compiler is fixed.
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedOptions {

    /** The options' names. */
    String[] value();
}
