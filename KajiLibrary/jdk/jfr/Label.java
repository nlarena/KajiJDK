package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The readable name of an event or of a field.
 *
 * <p>It serves for what is shown to a person. The name of the field is {@code allocationSize} and
 * its label is "Allocation Size": the first one is used by the code, the second one by the
 * interface.
 *
 * @since 9
 */
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Label {

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    String value();
}
