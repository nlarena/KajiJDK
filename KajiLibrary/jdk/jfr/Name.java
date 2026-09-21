package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The name under which the event or the field appears in the recording.
 *
 * <p>By default it is the complete name of the class, and that ties the format of the file to the
 * name of the package: renaming the class breaks the queries written against it. By putting an
 * explicit name the two are left independent.
 *
 * <p>The convention of the JDK is a name with dots, such as {@code jdk.ObjectAllocationSample}.
 *
 * @since 9
 */
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Name {

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    String value();
}
