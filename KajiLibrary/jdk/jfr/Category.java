package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Donde ubicar el evento en el arbol de categorias de una herramienta.
 *
 * <p>Es un arreglo porque el arbol tiene niveles: {@code {"Java Application", "Statistics"}} pone
 * al evento bajo "Statistics", que cuelga de "Java Application".
 *
 * <p>Sin esto, un evento propio queda suelto entre cientos, que es la diferencia entre una lista
 * navegable y uno que nadie encuentra.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Category {

    /**
     * El valor de la anotacion.
     *
     * @return el valor
     */
    String[] value();
}
