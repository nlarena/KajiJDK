package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca a un metodo como el que define un <strong>ajuste</strong> del evento.
 *
 * <p>El metodo toma un {@link SettingControl} y devuelve {@code boolean}: es el filtro que decide,
 * en cada emision, si el evento se graba. Es lo que permite que un evento tenga un ajuste propio
 * —no solo los que JFR trae— configurable desde afuera igual que {@code threshold} o
 * {@code stackTrace}.
 *
 * @since 9
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SettingDefinition {
}
