package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El elemento es <strong>experimental</strong> y puede desaparecer.
 *
 * <p>Una herramienta lo esconde por omision. Es lo que permite publicar un evento para probarlo sin
 * que quede convertido en compromiso por el solo hecho de que alguien lo haya usado.
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
