package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El campo es una <strong>duracion</strong>, con su unidad.
 *
 * <p>Sin esto, un {@code long} de valor 1500000000 se muestra tal cual. Con esto, y segun la
 * unidad, se muestra como "1,5 s".
 *
 * <p>{@link #TICKS} es la unidad del reloj de la maquina y no de tiempo real: el que lee tiene que
 * convertirla con la frecuencia del reloj, y por eso es la unica que no se puede mostrar sin mas
 * contexto.
 *
 * @since 9
 */
@Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@ContentType
@Label("Timespan")
@Description("A duration, measured in nanoseconds by default")
public @interface Timespan {

    /** Pulsos del reloj de la maquina; hay que convertirlos con su frecuencia. */
    String TICKS = "TICKS";

    /** Segundos. */
    String SECONDS = "SECONDS";

    /** Milisegundos. */
    String MILLISECONDS = "MILLISECONDS";

    /** Nanosegundos. */
    String NANOSECONDS = "NANOSECONDS";

    /** Microsegundos. */
    String MICROSECONDS = "MICROSECONDS";

    /**
     * El valor de la anotacion.
     *
     * @return el valor
     */
    String value() default NANOSECONDS;
}
