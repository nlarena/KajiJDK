package java.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * KajiLibrary's java.lang.SafeVarargs — the promise that a varargs method with a generic
 * parameter does nothing unsound with the array it receives. Generic arrays cannot be
 * created, so such a method draws an unchecked warning at every call site; this moves the
 * assertion to the one place that can actually judge it, the declaration.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface SafeVarargs {
}
