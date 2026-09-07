package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El campo es una <strong>bandera</strong>: se muestra como si o no.
 *
 * <p>Sirve para un entero que en realidad solo vale cero o distinto de cero, que es como llegan
 * muchas banderas desde la VM.
 *
 * @since 9
 */
@Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@ContentType
@Label("Flag")
public @interface BooleanFlag {
}
