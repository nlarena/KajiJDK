package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El nombre con el que el evento o el campo aparece en la grabacion.
 *
 * <p>Por omision es el nombre completo de la clase, y eso ata el formato del archivo al nombre del
 * paquete: renombrar la clase rompe las consultas escritas contra ella. Poniendo un nombre
 * explicito los dos quedan independientes.
 *
 * <p>La convencion del JDK es un nombre con puntos, como {@code jdk.ObjectAllocationSample}.
 *
 * @since 9
 */
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Name {

    /**
     * El valor de la anotacion.
     *
     * @return el valor
     */
    String value();
}
