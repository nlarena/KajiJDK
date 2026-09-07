package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El campo es un <strong>porcentaje</strong>, expresado entre 0 y 1.
 *
 * <p>Entre 0 y 1 y no entre 0 y 100: es lo que evita tener que adivinar si un 50 es la mitad o la
 * mitad de un uno por ciento. La herramienta multiplica al mostrarlo.
 *
 * @since 9
 */
@Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@ContentType
@Label("Percentage")
@Description("Percentage, represented as a number between 0 and 1")
public @interface Percentage {
}
