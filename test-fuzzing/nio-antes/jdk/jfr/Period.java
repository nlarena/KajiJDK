package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * How often a periodic event is emitted.
 *
 * <p>A periodic event is not triggered by anything that happens in the program: JFR emits it by
 * itself, every so often, and it serves for taking the state of something --how much memory there
 * is, how many threads-- instead of for recording a fact.
 *
 * <p>{@code "everyChunk"}, the default value, emits it once per block of the file, which is what
 * guarantees that the datum is in whatever piece of the recording somebody looks at.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Period {

    /** The name of the setting this annotation configures. */
    String NAME = "period";

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    String value() default "everyChunk";
}
