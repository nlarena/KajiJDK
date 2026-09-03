package java.awt.image.renderable;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;

/**
 * KajiLibrary's java.awt.image.renderable.RenderedImageFactory -- fabrica una imagen concreta a
 * partir de una operacion.
 *
 * <p>Un metodo, y con el se entiende el paquete entero. Una imagen <b>renderizable</b> no tiene
 * pixeles: es una descripcion de que hacer --rotar esto, mezclarlo con aquello-- y no tiene siquiera
 * un tamano en pixeles, porque sus coordenadas son reales y no enteras. Esta interfaz es el punto
 * donde esa descripcion se convierte en pixeles de verdad.
 *
 * <p>De ahi sale la ventaja del modelo: la misma cadena de operaciones se puede renderizar chica
 * para una vista previa y enorme para imprimir, sin recalcular nada, porque hasta que alguien llama
 * a {@code create} no se decidio ninguna resolucion.
 *
 * <p>Devolver null es valido y significa que esta fabrica no puede con esos argumentos.
 */
public interface RenderedImageFactory {

    /**
     * La imagen concreta.
     *
     * @param paramBlock las fuentes y los parametros de la operacion
     * @param hints preferencias de calidad contra velocidad; se pueden ignorar
     * @return null si esta fabrica no puede con eso
     */
    RenderedImage create(ParameterBlock paramBlock, RenderingHints hints);
}
