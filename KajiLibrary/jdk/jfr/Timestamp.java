package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El campo es un <strong>momento</strong>, no una duracion.
 *
 * <p>La diferencia con {@link Timespan} es la que hay entre "a las tres" y "tres horas", y se
 * muestran distinto: uno como fecha, el otro como cantidad.
 *
 * @since 9
 */
@Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@ContentType
@Label("Timestamp")
@Description("A point in time")
public @interface Timestamp {

    /** Milisegundos desde el 1 de enero de 1970 UTC. */
    String MILLISECONDS_SINCE_EPOCH = "MILLISECONDS_SINCE_EPOCH";

    /** Pulsos del reloj de la maquina. */
    String TICKS = "TICKS";

    /**
     * El valor de la anotacion.
     *
     * @return el valor
     */
    String value() default MILLISECONDS_SINCE_EPOCH;
}
