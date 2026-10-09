package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field is the thread <strong>from which</strong> a transition was made.
 *
 * <p>Together with {@link TransitionTo} it allows one to rebuild which thread handed work to which,
 * which is what makes a trace readable in a program with executors.
 *
 * @since 9
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@Label("Transition From")
public @interface TransitionFrom {
}
