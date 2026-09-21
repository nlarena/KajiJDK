package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field is a <strong>moment</strong>, not a duration.
 *
 * <p>The difference with {@link Timespan} is the one there is between "at three" and "three hours",
 * and they are shown differently: one as a date, the other as a quantity.
 *
 * @since 9
 */
@Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@ContentType
@Label("Timestamp")
@Description("A point in time")
public @interface Timestamp {

    /** Milliseconds since the 1st of January 1970 UTC. */
    String MILLISECONDS_SINCE_EPOCH = "MILLISECONDS_SINCE_EPOCH";

    /** Ticks of the clock of the machine. */
    String TICKS = "TICKS";

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    String value() default MILLISECONDS_SINCE_EPOCH;
}
