package java.awt;

import java.awt.image.ColorModel;
import java.awt.image.Raster;

/**
 * Quien genera los píxeles de un {@link Paint} durante una operación de dibujo.
 *
 * <p>La separación entre `Paint` y su contexto es la que hace que un degradé se pueda describir una
 * vez y dibujar muchas: el `Paint` es la **descripción** —dos puntos y dos colores— y el contexto es
 * la máquina que, para una transformación y un modelo de color concretos, produce los píxeles.
 *
 * <p>Se pide de a rectángulos y no de a píxeles porque casi todo degradé se calcula mucho más barato
 * por filas que punto por punto.
 */
public interface PaintContext {

    /**
     * Suelta los recursos del contexto.
     *
     * <p>Se llama siempre, también cuando el dibujo falló, así que tiene que poder llamarse sobre un
     * contexto que nunca generó un píxel.
     */
    void dispose();

    /** En qué formato vienen los píxeles que genera. */
    ColorModel getColorModel();

    /** Los píxeles de ese rectángulo, en coordenadas de dispositivo. */
    Raster getRaster(int x, int y, int w, int h);
}
