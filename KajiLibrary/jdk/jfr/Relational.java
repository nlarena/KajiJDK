package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca a una anotacion como una que <strong>relaciona</strong> campos de eventos distintos.
 *
 * <p>Dos eventos que llevan el mismo identificador de transaccion se pueden cruzar, pero solo si
 * alguien dice que esos dos campos son el mismo dato. Una anotacion marcada con esta es ese
 * alguien: puesta sobre los dos campos, declara que valores iguales significan la misma cosa.
 *
 * <p>Con eso una herramienta puede armar la traza completa de una transaccion a partir de eventos
 * que no se conocen entre si.
 *
 * @since 9
 */
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@Label("Relation")
public @interface Relational {
}
