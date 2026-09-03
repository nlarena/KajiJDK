package java.awt.image.renderable;

import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

/**
 * KajiLibrary's java.awt.image.renderable.ContextualRenderedImageFactory -- una operacion que sabe
 * cuanto necesita de sus fuentes.
 *
 * <p>Agrega a {@link RenderedImageFactory} lo que hace falta para que una cadena de operaciones se
 * pueda evaluar <b>por partes</b>. La pieza clave es {@link #mapRenderContext}: dado lo que se
 * quiere de la salida, dice que hace falta de la entrada.
 *
 * <p>Sin eso, pedir un recorte de mil pixeles del final de una cadena de diez filtros obligaria a
 * calcular las diez imagenes enteras. Con eso, cada operacion traduce el pedido hacia atras y solo
 * se calcula la region que de verdad se usa. Es la diferencia entre poder trabajar con una imagen de
 * gigapixeles y no poder.
 *
 * <p>La traduccion casi nunca es la identidad. Un desenfoque de radio cinco necesita cinco pixeles
 * <b>de mas</b> en cada borde para que el borde del recorte no salga mal, y una rotacion necesita un
 * cuadrilatero y no un rectangulo. Por eso el metodo devuelve un {@link RenderContext} nuevo y no un
 * rectangulo.
 *
 * <p>{@link #isDynamic} avisa si la operacion puede dar resultados distintos con los mismos
 * argumentos --porque lee de una fuente viva, por ejemplo--. Es lo que le dice al sistema si puede
 * guardarse el resultado en cache.
 */
public interface ContextualRenderedImageFactory extends RenderedImageFactory {

    /**
     * Que hace falta de una fuente para poder producir lo que se pide.
     *
     * @param i cual de las fuentes
     * @param renderContext lo que se quiere de la salida
     * @return lo que hay que pedirle a esa fuente; ver la nota de la clase
     */
    RenderContext mapRenderContext(int i, RenderContext renderContext, ParameterBlock paramBlock,
                                   RenderableImage image);

    /** La imagen concreta para ese contexto. */
    RenderedImage create(RenderContext renderContext, ParameterBlock paramBlock);

    /**
     * El rectangulo que ocupa la salida, en coordenadas <b>reales</b>.
     *
     * <p>Reales y no enteras porque una imagen renderizable no tiene resolucion; ver
     * {@link RenderedImageFactory}.
     */
    Rectangle2D getBounds2D(ParameterBlock paramBlock);

    /**
     * Una propiedad de la salida, calculada sin renderizar.
     *
     * @return {@code java.awt.Image.UndefinedProperty} si no la tiene
     */
    Object getProperty(ParameterBlock paramBlock, String name);

    /** Los nombres de las propiedades que sabe contestar, o null si no tiene ninguna. */
    String[] getPropertyNames();

    /** Si dos renderizaciones iguales pueden dar resultados distintos. Ver la nota de la clase. */
    boolean isDynamic();
}
