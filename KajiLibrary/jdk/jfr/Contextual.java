package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field is <strong>context</strong>: it does not describe the event, it says in what situation
 * it happened.
 *
 * <p>An identifier of a request or of a transaction is context. The distinction serves so that a
 * tool can group by it, which is different from showing it as one more datum.
 *
 * @since 9
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@Label("Context")
public @interface Contextual {
}
