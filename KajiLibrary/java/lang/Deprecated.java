package java.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * KajiLibrary's java.lang.Deprecated — marks an element that should no longer be used.
 * Unlike @Override it is RUNTIME-visible, because tools that never see the source (an IDE
 * reading a jar, a linker) have to be able to warn about it too.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.LOCAL_VARIABLE,
         ElementType.METHOD, ElementType.PACKAGE, ElementType.MODULE, ElementType.PARAMETER,
         ElementType.TYPE})
public @interface Deprecated {

    // The release the element was deprecated in, as a version string, or "" if unsaid.
    String since() default "";

    // Whether the element is meant to be REMOVED in a future release. The difference matters
    // to a caller: "there is a better way" is advice, "this will stop existing" is a
    // deadline, and tools raise a stronger warning for the second.
    boolean forRemoval() default false;
}
