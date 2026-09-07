package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca a una anotacion como una que le da <strong>significado</strong> a un numero.
 *
 * <p>Un {@code long} en un evento puede ser nanosegundos, bytes, una direccion de memoria o un
 * conteo. El tipo Java es el mismo en los cuatro casos y no alcanza para mostrarlo bien: 1500000000
 * se lee como "1,5 s" o como "1,4 GB" segun que sea.
 *
 * <p>{@link Timespan}, {@link DataAmount}, {@link MemoryAddress}, {@link Percentage} y
 * {@link Frequency} estan marcadas con esta, y son las que aportan ese significado.
 *
 * @since 9
 */
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@Label("Content Type")
@Description("Semantic meaning of a value")
public @interface ContentType {
}
