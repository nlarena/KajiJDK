package pq;

/**
 * Hereda de {@link Base} y no declara constructor.
 *
 * <p>El javac real rechaza esto: el constructor por omision que habria que generar llama a
 * {@code super()}, y {@link Base} no tiene ninguno sin argumentos.
 *
 * <pre>
 * javac real:
 *   error: constructor Base in class Base cannot be applied to given types;
 *     required: Object
 *     found:    no arguments
 *
 * el nuestro:
 *   javac: escrito Hija.class   (y adentro un invokespecial a Base."&lt;init&gt;":()V que no existe)
 * </pre>
 */
public abstract class Hija extends Base {

    public abstract int f();
}
