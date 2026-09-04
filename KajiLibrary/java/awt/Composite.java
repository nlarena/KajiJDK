package java.awt;

import java.awt.image.ColorModel;

/**
 * Cómo se mezcla lo que se dibuja con lo que ya estaba.
 *
 * <p>Sin composición, dibujar es reemplazar. Con composición, dibujar es una operación entre dos
 * imágenes: la que se está pintando y la que ya estaba. De ahí salen la transparencia, el recorte
 * por alfa y todos los modos de mezcla.
 *
 * <p>El objeto describe la regla; el trabajo lo hace un {@link CompositeContext}, que se pide una
 * vez por operación de dibujo con los formatos de píxel ya conocidos.
 */
public interface Composite {

    /**
     * Arma la máquina que va a mezclar.
     *
     * @param srcColorModel el formato de lo que se dibuja
     * @param dstColorModel el formato de lo que ya estaba
     * @param hints las pistas de calidad
     */
    CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel,
            RenderingHints hints);
}
