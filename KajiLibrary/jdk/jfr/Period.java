package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cada cuanto se emite un evento periodico.
 *
 * <p>Un evento periodico no lo dispara nada que pase en el programa: lo emite JFR solo, cada tanto,
 * y sirve para tomar el estado de algo —cuanta memoria hay, cuantos hilos— en vez de para registrar
 * un hecho.
 *
 * <p>{@code "everyChunk"}, el valor por omision, lo emite una vez por bloque del archivo, que es lo
 * que garantiza que el dato este en cualquier trozo de la grabacion que alguien mire.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Period {

    /** El nombre del ajuste que esta anotacion configura. */
    String NAME = "period";

    /**
     * El valor de la anotacion.
     *
     * @return el valor
     */
    String value() default "everyChunk";
}
