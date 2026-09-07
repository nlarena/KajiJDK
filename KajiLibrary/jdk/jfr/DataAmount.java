package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El campo es una <strong>cantidad de datos</strong>, en bytes o en bits.
 *
 * <p>Permite que una herramienta lo muestre como "1,4 GB" en vez de como un numero de diez cifras.
 * La distincion entre bits y bytes importa donde de verdad se miden bits: un ancho de banda de red.
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

    /** El valor esta en bits. */
    String BITS = "BITS";

    /** El valor esta en bytes. */
    String BYTES = "BYTES";

    /**
     * El valor de la anotacion.
     *
     * @return el valor
     */
    String value() default BYTES;
}
