package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * How long the event has to last for it to be worth recording.
 *
 * <p>The short events are the ones there are most of and the ones that say least. A threshold of
 * {@code "20 ms"} discards the noise and leaves the waits a human would notice.
 *
 * <p>The default value, {@code "0 ns"}, records everything.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Threshold {

    /** The name of the setting this annotation configures. */
    String NAME = "threshold";

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    String value() default "0 ns";
}
