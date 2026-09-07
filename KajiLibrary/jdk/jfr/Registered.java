package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Si el evento se registra solo al cargarse la clase.
 *
 * <p>Registrar significa que JFR lo conoce y puede grabarlo. Ponerlo en {@code false} deja el
 * registro para mas tarde, con {@link FlightRecorder#register}, que es lo que hace falta cuando la
 * clase se carga antes de que este decidido si el evento se va a usar.
 *
 * <p>Es la unica anotacion de este grupo que <strong>no</strong> es {@link MetadataDefinition}: no
 * describe al evento, decide cuando aparece.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Registered {

    /**
     * El valor de la anotacion.
     *
     * @return el valor
     */
    boolean value() default true;
}
