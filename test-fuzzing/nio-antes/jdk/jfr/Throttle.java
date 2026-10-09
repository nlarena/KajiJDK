package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A cap on how many events per unit of time are recorded.
 *
 * <p>It is written as {@code "100/s"}. Unlike {@link Threshold}, which discards by what the event
 * <strong>is</strong>, this discards by how many there were: when the cap is passed, JFR samples
 * instead of recording everything.
 *
 * <p>It is the tool for an event whose volume cannot be predicted, where a threshold is not enough
 * because the problem is not that each event is expensive but that there are too many of them.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Throttle {

    /** The name of the setting this annotation configures. */
    String NAME = "throttle";

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    String value() default "off";
}
