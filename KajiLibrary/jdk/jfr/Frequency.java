package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El campo es una <strong>frecuencia</strong>, en hercios.
 *
 * <p>No lleva unidad configurable porque solo hay una.
 *
 * @since 9
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@ContentType
@Label("Frequency")
@Description("Measure of how often something occurs, in Hertz")
public @interface Frequency {
}
