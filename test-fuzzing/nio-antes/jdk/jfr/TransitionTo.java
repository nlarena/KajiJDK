package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field is the thread <strong>towards which</strong> a transition was made.
 *
 * <p>See {@link TransitionFrom}.
 *
 * @since 9
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@Label("Transition To")
public @interface TransitionTo {
}
