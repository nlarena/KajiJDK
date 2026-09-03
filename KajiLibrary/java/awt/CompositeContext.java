package java.awt;

/**
 * El estado de un {@code Composite} mientras dura una operacion de dibujo: quien mezcla de verdad.
 *
 * <h2>Superficie parcial</h2>
 *
 * <p>Falta {@code compose(Raster, Raster, WritableRaster)}, que es el metodo que hace el trabajo:
 * sus tres parametros viven en {@code java.awt.image} y ese paquete no existe en KajiLibrary. Queda
 * {@code dispose()}, que si se puede declarar entero.
 */
public interface CompositeContext {

    /** Suelta los recursos del contexto. */
    void dispose();
}
