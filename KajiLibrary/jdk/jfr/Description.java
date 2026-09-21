package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The long explanation of an event or of a field.
 *
 * <p>It is what {@link Label} cannot say in two words. It goes in the recording file, so a tool can
 * show the help of an event it did not know.
 *
 * @since 9
 */
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Description {

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    String value();
}
