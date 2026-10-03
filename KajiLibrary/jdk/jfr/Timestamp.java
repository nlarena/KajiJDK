package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field is a <strong>moment</strong>, not a duration.
 *
 * <p>The difference with {@link Timespan} is the one there is between "at three" and "three hours",
 * and they are shown differently: one as a date, the other as a quantity.
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

    /** Milliseconds since the 1st of January 1970 UTC. */
    String MILLISECONDS_SINCE_EPOCH = "MILLISECONDS_SINCE_EPOCH";

    /** Ticks of the clock of the machine. */
    String TICKS = "TICKS";

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    // El literal y no la constante `MILLISECONDS_SINCE_EPOCH`, que es lo que dice lo mismo: nuestro javac **no emite el
    // `AnnotationDefault`** cuando el default es una referencia a una constante (finding #560), y sin
    // ese atributo `@Timestamp` sin valor explicito queda sin valor del todo -- `EventType` lo rechaza con
    // *the value of the member value ... is missing*. Las dos formas son el mismo constante de
    // compilacion; cuando el #560 se arregle, vuelve a ser `MILLISECONDS_SINCE_EPOCH`.
    String value() default "MILLISECONDS_SINCE_EPOCH";
}
