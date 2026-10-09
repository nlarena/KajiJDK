package javax.annotation.processing;

import javax.lang.model.SourceVersion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// The latest version of the language a processor declares it supports.
// `AbstractProcessor.getSupportedSourceVersion()` reads it by reflection.
//
// WARNING — the same as `@SupportedOptions` and `@SupportedAnnotationTypes`: in this VM it cannot be
// read at run time (see `AbstractProcessor`'s header), so `getSupportedSourceVersion()` always falls
// back to its default value.
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedSourceVersion {

    /** The supported version. */
    SourceVersion value();
}
