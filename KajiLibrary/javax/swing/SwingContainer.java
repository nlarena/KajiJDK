package javax.swing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Dice si una clase de componente es un contenedor, para las herramientas visuales.
 *
 * <h2>Contenedor de Java no es contenedor de la herramienta</h2>
 *
 * <p>Casi todo componente de Swing hereda de {@code Container}, pero muy pocos aceptan que uno les
 * suelte cosas adentro: un {@link JButton} <em>es</em> un contenedor de Java y no tiene ningun
 * sentido soltarle un campo de texto. Esta anotacion es como una clase le dice a un armador de
 * pantallas cual de las dos cosas es.
 *
 * <p>{@link #delegate} existe para los que si aceptan, pero no directamente: en un
 * {@link JScrollPane} lo que se agrega va a su vista, no a el. El nombre que se pone ahi es el del
 * metodo que devuelve el contenedor de verdad.
 *
 * <p>Se conserva en tiempo de ejecucion porque quien la lee -- la herramienta -- ve la clase ya
 * compilada, no su fuente.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwingContainer {

    /** Si acepta que le agreguen cosas. */
    boolean value() default true;

    /** El metodo que devuelve el contenedor de verdad; ver la nota de la anotacion. */
    String delegate() default "";
}
