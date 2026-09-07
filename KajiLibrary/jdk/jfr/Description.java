package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * La explicacion larga de un evento o de un campo.
 *
 * <p>Es lo que {@link Label} no puede decir en dos palabras. Va en el archivo de grabacion, asi que
 * una herramienta puede mostrar la ayuda de un evento que no conocia.
 *
 * @since 9
 */
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Description {

    /**
     * El valor de la anotacion.
     *
     * @return el valor
     */
    String value();
}
