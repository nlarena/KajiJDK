package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El campo es el hilo <strong>hacia el cual</strong> se hizo una transicion.
 *
 * <p>Ver {@link TransitionFrom}.
 *
 * @since 9
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@Label("Transition To")
public @interface TransitionTo {
}
