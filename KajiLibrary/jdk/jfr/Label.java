package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El nombre legible de un evento o de un campo.
 *
 * <p>Sirve para lo que se le muestra a una persona. El nombre del campo es {@code allocationSize} y
 * su etiqueta es "Allocation Size": el primero lo usa el codigo, el segundo la interfaz.
 *
 * @since 9
 */
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Label {

    /**
     * El valor de la anotacion.
     *
     * @return el valor
     */
    String value();
}
