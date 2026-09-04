package javax.imageio.event;

import java.awt.image.BufferedImage;
import java.util.EventListener;
import javax.imageio.ImageReader;

/**
 * KajiLibrary's javax.imageio.event.IIOReadUpdateListener -- entrega la imagen a medio decodificar.
 *
 * <p>La diferencia con {@link IIOReadProgressListener} es que aquel dice <b>cuanto</b> falta y este
 * entrega <b>lo que ya hay</b>. Es lo que permite mostrar una imagen que se va formando mientras se
 * descarga, en lugar de una barra.
 *
 * <h2>La imagen que llega es la de verdad, y todavia esta cambiando</h2>
 *
 * <p>Es lo importante y lo que se hace mal. El {@link BufferedImage} que se pasa es el destino real de
 * la lectura, no una copia: el decodificador va a seguir escribiendo en el en cuanto el metodo
 * devuelva.
 *
 * <p>Guardarla y usarla despues da una imagen que cambia sola. Dibujarla desde otro hilo da desgarros.
 * Lo unico seguro es leerla ahi mismo, o copiarla.
 *
 * <p>Los seis parametros de region --{@code minX}, {@code minY}, {@code width}, {@code height} y los
 * dos pasos-- dicen que rectangulo cambio, para no tener que redibujar todo.
 *
 * <h2>Las pasadas son de los formatos progresivos</h2>
 *
 * <p>{@link #passStarted} y {@link #passComplete} solo llegan en un JPEG progresivo o un PNG
 * entrelazado, donde la imagen se completa en varias vueltas cada vez mas nitidas. En un formato
 * normal hay una sola pasada.
 */
public interface IIOReadUpdateListener extends EventListener {

    /**
     * Empieza una pasada. Ver la nota de la clase.
     *
     * @param theImage el destino real, que va a seguir cambiando
     * @param pass cual pasada
     * @param minPass la primera que se va a hacer
     * @param maxPass la ultima
     * @param bands que bandas toca esta pasada
     */
    void passStarted(ImageReader source, BufferedImage theImage, int pass, int minPass,
                     int maxPass, int minX, int minY, int periodX, int periodY, int[] bands);

    /**
     * Cambio ese rectangulo de la imagen.
     *
     * @param periodX cada cuantos pixeles en X se escribio, para las pasadas entrelazadas
     */
    void imageUpdate(ImageReader source, BufferedImage theImage, int minX, int minY, int width,
                     int height, int periodX, int periodY, int[] bands);

    /** Termino la pasada. */
    void passComplete(ImageReader source, BufferedImage theImage);

    /** Idem, para una miniatura. */
    void thumbnailPassStarted(ImageReader source, BufferedImage theThumbnail, int pass,
                              int minPass, int maxPass, int minX, int minY, int periodX,
                              int periodY, int[] bands);

    /** Idem. */
    void thumbnailUpdate(ImageReader source, BufferedImage theThumbnail, int minX, int minY,
                         int width, int height, int periodX, int periodY, int[] bands);

    /** Idem. */
    void thumbnailPassComplete(ImageReader source, BufferedImage theThumbnail);
}
