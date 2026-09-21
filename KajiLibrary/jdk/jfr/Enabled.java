package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Whether the event is recorded when nobody said otherwise.
 *
 * <p>Putting it at {@code false} leaves the event defined and switched off: it exists, it can be
 * switched on from a configuration, and meanwhile it costs nothing. It is what corresponds for an
 * expensive or very frequent event that is only of interest in one specific investigation.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Enabled {

    /** The name of the setting this annotation configures. */
    String NAME = "enabled";

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    boolean value() default true;
}
