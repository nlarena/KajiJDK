package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * It marks an annotation as part of the <strong>metadata</strong> of an event.
 *
 * <p>It is the annotation of the annotations: without it, an annotation put on an event is seen by
 * the compiler and not seen by JFR. With it, JFR copies it to the recording file and a tool that
 * opens it later can read it.
 *
 * <p>That is what allows an event of one's own to carry information of its own without JFR having
 * to know it beforehand.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface MetadataDefinition {
}
