package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El campo es el hilo <strong>desde el cual</strong> se hizo una transicion.
 *
 * <p>Junto con {@link TransitionTo} permite reconstruir que hilo le paso trabajo a cual, que es lo
 * que hace legible una traza en un programa con ejecutores.
 *
 * @since 9
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@Label("Transition From")
public @interface TransitionFrom {
}
