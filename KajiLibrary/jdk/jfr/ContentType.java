package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * It marks an annotation as one that gives a number its <strong>meaning</strong>.
 *
 * <p>A {@code long} in an event may be nanoseconds, bytes, a memory address or a count. The Java
 * type is the same in the four cases and is not enough to show it properly: 1500000000 reads as
 * "1.5 s" or as "1.4 GB" depending on what it is.
 *
 * <p>{@link Timespan}, {@link DataAmount}, {@link MemoryAddress}, {@link Percentage} and
 * {@link Frequency} are marked with this one, and they are the ones that bring that meaning.
 *
 * @since 9
 */
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@Label("Content Type")
@Description("Semantic meaning of a value")
public @interface ContentType {
}
