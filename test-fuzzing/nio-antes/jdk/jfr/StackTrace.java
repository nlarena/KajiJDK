package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Whether the stack of calls is kept when the event is recorded.
 *
 * <p>It is the most useful datum and the most expensive: walking the stack costs, and keeping it
 * multiplies the size of each event. For an event that happens thousands of times per second,
 * switching it off is the difference between a usable recording and one that fills the disk.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface StackTrace {

    /** The name of the setting this annotation configures. */
    String NAME = "stackTrace";

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    boolean value() default true;
}
