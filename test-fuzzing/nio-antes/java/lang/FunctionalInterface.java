package java.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * KajiLibrary's java.lang.FunctionalInterface — declares that an interface is meant to have
 * exactly one abstract method, so it can be the target of a lambda. It does not *make* the
 * interface functional (any interface with one abstract method already is); it makes the
 * intent checkable, so adding a second abstract method fails at the declaration instead of
 * at every lambda that used it.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FunctionalInterface {
}
