package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca a una anotacion como parte de los <strong>metadatos</strong> de un evento.
 *
 * <p>Es la anotacion de las anotaciones: sin ella, una anotacion puesta sobre un evento la ve el
 * compilador y no la ve JFR. Con ella, JFR la copia al archivo de grabacion y una herramienta que
 * lo abra despues puede leerla.
 *
 * <p>Eso es lo que permite que un evento propio lleve informacion propia sin que JFR tenga que
 * conocerla de antemano.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface MetadataDefinition {
}
