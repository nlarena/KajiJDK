package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field is an <strong>amount of data</strong>, in bytes or in bits.
 *
 * <p>It allows a tool to show it as "1.4 GB" instead of as a ten-figure number. The distinction
 * between bits and bytes matters where bits are really measured: a network bandwidth.
 *
 * @since 9
 */
@Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@ContentType
@Label("Data Amount")
@Description("Amount of data")
public @interface DataAmount {

    /** The value is in bits. */
    String BITS = "BITS";

    /** The value is in bytes. */
    String BYTES = "BYTES";

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    // El literal y no la constante `BYTES`, que es lo que dice lo mismo: nuestro javac **no emite el
    // `AnnotationDefault`** cuando el default es una referencia a una constante (finding #560), y sin
    // ese atributo `@DataAmount` sin valor explicito queda sin valor del todo -- `EventType` lo rechaza con
    // *the value of the member value ... is missing*. Las dos formas son el mismo constante de
    // compilacion; cuando el #560 se arregle, vuelve a ser `BYTES`.
    String value() default "BYTES";
}
