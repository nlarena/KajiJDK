package javax.imageio.event;

import java.util.EventListener;
import javax.imageio.ImageWriter;

/**
 * KajiLibrary's javax.imageio.event.IIOWriteWarningListener -- avisa de un problema no fatal al
 * escribir.
 *
 * <p>El espejo de {@link IIOReadWarningListener}, con un argumento mas: <b>cual</b> imagen, porque un
 * archivo puede llevar varias.
 *
 * <p>La advertencia tipica al escribir es una perdida: metadatos que el formato de destino no puede
 * expresar, un color que no entra en la paleta, una precision que se recorta. El archivo sale igual, y
 * mas pobre de lo que se pidio.
 *
 * <p>Como en la lectura, sin escuchas registrados eso se pierde en silencio -- que es peor al escribir,
 * porque el original puede no estar mas.
 */
public interface IIOWriteWarningListener extends EventListener {

    /**
     * Algo se perdio y se pudo seguir. Ver la nota de la clase.
     *
     * @param imageIndex cual imagen del archivo
     */
    void warningOccurred(ImageWriter source, int imageIndex, String warning);
}
