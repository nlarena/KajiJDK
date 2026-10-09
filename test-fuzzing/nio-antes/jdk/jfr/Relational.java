package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * It marks an annotation as one that <strong>relates</strong> fields of different events.
 *
 * <p>Two events that carry the same transaction identifier can be crossed, but only if somebody
 * says that those two fields are the same datum. An annotation marked with this one is that
 * somebody: put on the two fields, it declares that equal values mean the same thing.
 *
 * <p>With that a tool can put together the complete trace of a transaction from events that do not
 * know each other.
 *
 * @since 9
 */
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@Label("Relation")
public @interface Relational {
}
