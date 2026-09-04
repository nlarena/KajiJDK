package javax.management;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Aplicada a un metodo de <b>otra</b> anotacion, dice bajo que clave entra su valor en el
 * {@link Descriptor} del MBean.
 *
 * <p>Es decir: es una anotacion sobre anotaciones. Sirve para que una anotacion de dominio
 * --digamos `@Unidades("ms")`-- termine como el par `unidades=ms` en el descriptor de la operacion,
 * sin que el servidor de MBeans tenga que conocer `@Unidades`. De ahi el `@Target(METHOD)`: el
 * blanco es el elemento de la anotacion, no el metodo del MBean.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DescriptorKey {

    /** La clave del descriptor. */
    String value();
}
