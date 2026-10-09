package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field is a <strong>frequency</strong>, in hertz.
 *
 * <p>It carries no configurable unit because there is only one.
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
