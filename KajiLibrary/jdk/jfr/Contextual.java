package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El campo es <strong>contexto</strong>: no describe al evento, dice en que situacion ocurrio.
 *
 * <p>Un identificador de peticion o de transaccion es contexto. La distincion sirve para que una
 * herramienta pueda agrupar por el, que es distinto de mostrarlo como un dato mas.
 *
 * @since 9
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@Label("Context")
public @interface Contextual {
}
