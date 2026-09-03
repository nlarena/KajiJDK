package java.awt.image.renderable;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.util.Vector;

/**
 * KajiLibrary's java.awt.image.renderable.RenderableImage -- una imagen sin resolucion.
 *
 * <p>El contraste con {@code RenderedImage} es todo el punto del paquete, y esta en los tipos: alla
 * {@code getWidth()} devuelve un {@code int} porque son pixeles, y aca devuelve un {@code float}
 * porque son <b>coordenadas de usuario</b>. Una imagen renderizable describe algo --una elipse
 * desenfocada, un mapa de un pais-- sin comprometerse a un tamano.
 *
 * <p>La resolucion se elige al pedir la renderizacion, y por eso hay tres formas de pedirla:
 * {@link #createScaledRendering} para un tamano en pixeles, {@link #createDefaultRendering} para el
 * que la imagen considere natural, y {@link #createRendering} para el control completo con un
 * {@link RenderContext}.
 *
 * <p>{@link #getSources} devuelve las imagenes de las que esta se calcula: una imagen renderizable es
 * normalmente el nodo de un arbol de operaciones, no un dato suelto.
 *
 * <p>{@link #HINTS_OBSERVED} es el nombre de una propiedad, no una bandera: si la renderizacion la
 * tiene, su valor dice cuales de las preferencias que se pidieron se respetaron de verdad. Existe
 * porque las preferencias son <b>preferencias</b> y una implementacion puede ignorarlas todas sin
 * avisar.
 */
public interface RenderableImage {

    /** El nombre de la propiedad que dice que preferencias se respetaron. */
    static final String HINTS_OBSERVED = "HINTS_OBSERVED";

    /** Las imagenes de las que esta se calcula; vacio si es una fuente. */
    Vector<RenderableImage> getSources();

    /**
     * Una propiedad de la imagen.
     *
     * @return {@code java.awt.Image.UndefinedProperty} si no la tiene
     */
    Object getProperty(String name);

    /** Los nombres de las propiedades, o null. */
    String[] getPropertyNames();

    /** Si dos renderizaciones iguales pueden dar resultados distintos. */
    boolean isDynamic();

    /** El ancho en coordenadas de usuario. Ver la nota de la clase sobre por que no es un entero. */
    float getWidth();

    /** El alto en coordenadas de usuario. */
    float getHeight();

    /** El borde izquierdo en coordenadas de usuario. */
    float getMinX();

    /** El borde superior en coordenadas de usuario. */
    float getMinY();

    /**
     * Renderiza a un tamano en pixeles.
     *
     * <p>Uno de los dos puede ser 0 para decir "el que salga manteniendo la proporcion". Los dos en
     * 0 no significa nada.
     */
    RenderedImage createScaledRendering(int w, int h, RenderingHints hints);

    /** Renderiza al tamano que la imagen considere natural. */
    RenderedImage createDefaultRendering();

    /** Renderiza con control completo. */
    RenderedImage createRendering(RenderContext renderContext);
}
