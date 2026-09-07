package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Un tope de cuantos eventos por unidad de tiempo se graban.
 *
 * <p>Se escribe como {@code "100/s"}. A diferencia de {@link Threshold}, que descarta por lo que el
 * evento <strong>es</strong>, esto descarta por cuantos hubo: cuando se pasa del tope, JFR muestrea
 * en lugar de grabar todo.
 *
 * <p>Es la herramienta para un evento cuyo volumen no se puede predecir, donde un umbral no alcanza
 * porque el problema no es que cada evento sea caro sino que son demasiados.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Throttle {

    /** El nombre del ajuste que esta anotacion configura. */
    String NAME = "throttle";

    /**
     * El valor de la anotacion.
     *
     * @return el valor
     */
    String value() default "off";
}
