package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The element is <strong>experimental</strong> and may disappear.
 *
 * <p>A tool hides it by default. It is what allows one to publish an event in order to try it out
 * without it turning into a commitment for the sole reason that somebody used it.
 *
 * @since 9
 */
@Target({ ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@Label("Experimental")
@Description("Element is not to be shown to a user by default")
public @interface Experimental {
}
