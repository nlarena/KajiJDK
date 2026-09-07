package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cuanto tiene que durar el evento para que valga la pena grabarlo.
 *
 * <p>Los eventos cortos son los que mas hay y los que menos dicen. Un umbral de {@code "20 ms"}
 * descarta el ruido y deja las esperas que un humano notaria.
 *
 * <p>El valor por omision, {@code "0 ns"}, graba todo.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Threshold {

    /** El nombre del ajuste que esta anotacion configura. */
    String NAME = "threshold";

    /**
     * El valor de la anotacion.
     *
     * @return el valor
     */
    String value() default "0 ns";
}
