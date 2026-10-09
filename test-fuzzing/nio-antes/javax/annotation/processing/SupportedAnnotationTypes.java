package javax.annotation.processing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// The annotation types a processor declares it wants to see.
// `AbstractProcessor.getSupportedAnnotationTypes()` reads it by reflection.
//
// WARNING — in this VM this annotation CANNOT be read at run time, because of a bug in our javac (it
// does not emit `RuntimeVisibleAnnotations` when the annotation's type comes from the classpath). See
// `AbstractProcessor`'s header for the detail and the consequence.
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedAnnotationTypes {

    /** The annotation types' full names; `"*"` is all of them. */
    String[] value();
}
