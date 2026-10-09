package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field is a <strong>percentage</strong>, expressed between 0 and 1.
 *
 * <p>Between 0 and 1 and not between 0 and 100: it is what avoids having to guess whether a 50 is a
 * half or half of one per cent. The tool multiplies when showing it.
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
