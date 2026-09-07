package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El campo es una <strong>direccion de memoria</strong>.
 *
 * <p>Se muestra en hexadecimal, que es la unica forma en que una direccion se puede comparar con
 * otra de un vistazo.
 *
 * @since 9
 */
@Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@ContentType
@Label("Memory Address")
@Description("Represents a physical memory address")
public @interface MemoryAddress {
}
