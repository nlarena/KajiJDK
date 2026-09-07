package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Si el evento se graba cuando nadie dijo lo contrario.
 *
 * <p>Ponerlo en {@code false} deja al evento definido y apagado: existe, se puede prender desde una
 * configuracion, y mientras tanto no cuesta nada. Es lo que corresponde para un evento caro o muy
 * frecuente que solo interesa en una investigacion puntual.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Enabled {

    /** El nombre del ajuste que esta anotacion configura. */
    String NAME = "enabled";

    /**
     * El valor de la anotacion.
     *
     * @return el valor
     */
    boolean value() default true;
}
