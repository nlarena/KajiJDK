package javax.management;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca --o desmarca-- una interfaz como MXBean.
 *
 * <p>Existe porque la convencion del sufijo no alcanza. Sin la anotacion, una interfaz es MXBean
 * solo si su nombre simple termina en `MXBean`; con ella el autor decide explicitamente, y por eso
 * el valor es `boolean` y no un marcador pelado: `@MXBean(false)` sobre `FooMXBean` la vuelve una
 * interfaz comun, que es el unico modo de escapar de la convencion.
 *
 * <p>Es `RUNTIME` porque quien la lee es el servidor de MBeans al registrar, no el compilador.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MXBean {

    /** Si la interfaz anotada es un MXBean. */
    boolean value() default true;
}
