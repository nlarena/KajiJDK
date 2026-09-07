package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Si al grabar el evento se guarda la pila de llamadas.
 *
 * <p>Es el dato mas util y el mas caro: caminar la pila cuesta, y guardarla multiplica el tamano de
 * cada evento. Para un evento que ocurre miles de veces por segundo, apagarla es la diferencia
 * entre una grabacion usable y una que llena el disco.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface StackTrace {

    /** El nombre del ajuste que esta anotacion configura. */
    String NAME = "stackTrace";

    /**
     * El valor de la anotacion.
     *
     * @return el valor
     */
    boolean value() default true;
}
