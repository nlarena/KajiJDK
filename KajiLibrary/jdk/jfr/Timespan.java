package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field is a <strong>duration</strong>, with its unit.
 *
 * <p>Without this, a {@code long} of value 1500000000 is shown as it is. With this, and according
 * to the unit, it is shown as "1.5 s".
 *
 * <p>{@link #TICKS} is the unit of the clock of the machine and not of real time: the one who reads
 * has to convert it with the frequency of the clock, and that is why it is the only one that cannot
 * be shown without more context.
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

    /** Ticks of the clock of the machine; they have to be converted with its frequency. */
    String TICKS = "TICKS";

    /** Seconds. */
    String SECONDS = "SECONDS";

    /** Milliseconds. */
    String MILLISECONDS = "MILLISECONDS";

    /** Nanoseconds. */
    String NANOSECONDS = "NANOSECONDS";

    /** Microseconds. */
    String MICROSECONDS = "MICROSECONDS";

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    // El literal y no la constante `NANOSECONDS`, que es lo que dice lo mismo: nuestro javac **no emite el
    // `AnnotationDefault`** cuando el default es una referencia a una constante (finding #560), y sin
    // ese atributo `@Timespan` sin valor explicito queda sin valor del todo -- `EventType` lo rechaza con
    // *the value of the member value ... is missing*. Las dos formas son el mismo constante de
    // compilacion; cuando el #560 se arregle, vuelve a ser `NANOSECONDS`.
    String value() default "NANOSECONDS";
}
