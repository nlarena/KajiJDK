package java.awt;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Quien mezcla de verdad los píxeles durante una operación de dibujo.
 *
 * <p>Es a {@link Composite} lo que {@link PaintContext} es a {@link Paint}: el `Composite` describe
 * la regla y el contexto la aplica, ya sabiendo con qué formatos de píxel va a trabajar.
 */
public interface CompositeContext {

    /** Suelta los recursos del contexto. */
    void dispose();

    /**
     * Mezcla el origen con el destino y escribe el resultado.
     *
     * <p>`dstOut` puede ser el mismo objeto que `dstIn`, y de hecho suele serlo: componer sobre la
     * superficie es lo normal.
     */
    void compose(Raster src, Raster dstIn, WritableRaster dstOut);
}
