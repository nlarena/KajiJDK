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
    String value() default BYTES;
}
