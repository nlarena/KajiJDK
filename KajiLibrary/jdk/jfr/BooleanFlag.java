package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field is a <strong>flag</strong>: it is shown as yes or no.
 *
 * <p>It serves for an integer that in reality is only worth zero or different from zero, which is
 * how many flags arrive from the VM.
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
