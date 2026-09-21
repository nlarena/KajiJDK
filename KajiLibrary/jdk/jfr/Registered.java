package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Whether the event registers itself when the class is loaded.
 *
 * <p>Registering means that JFR knows it and can record it. Putting it at {@code false} leaves the
 * registration for later, with {@link FlightRecorder#register}, which is what is needed when the
 * class is loaded before it is decided whether the event is going to be used.
 *
 * <p>It is the only annotation of this group that is <strong>not</strong>
 * {@link MetadataDefinition}: it does not describe the event, it decides when it appears.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Registered {

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    boolean value() default true;
}
