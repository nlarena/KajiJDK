package java.awt.image;

import java.awt.Image;

/**
 * Quien quiere enterarse de que una imagen que se está cargando avanzó.
 *
 * <p>Es la mitad asíncrona del modelo de imágenes de AWT. Una imagen que viene de la red no está
 * entera cuando se la pide, así que `getWidth` puede contestar -1 y avisar después. El observador es
 * a quien se le avisa.
 *
 * <p>El valor de retorno de {@link #imageUpdate} se suele leer al revés: devolver `true` significa
 * <strong>seguí avisándome</strong>, y `false`, que ya no interesa. Un observador que devuelve
 * siempre `true` nunca se da de baja.
 */
public interface ImageObserver {

    /** El ancho ya se conoce. */
    int WIDTH = 1;

    /** El alto ya se conoce. */
    int HEIGHT = 2;

    /** Las propiedades ya se conocen. */
    int PROPERTIES = 4;

    /** Hay más píxeles disponibles que antes. */
    int SOMEBITS = 8;

    /** Se completó un cuadro de una imagen de varios. */
    int FRAMEBITS = 16;

    /** La imagen está completa. */
    int ALLBITS = 32;

    /** Hubo un error y la imagen no se va a poder cargar. */
    int ERROR = 64;

    /** La carga se abortó; puede reintentarse. */
    int ABORT = 128;

    /**
     * Avisa que la imagen avanzó.
     *
     * @param infoflags la combinación de banderas que dice qué cambió
     * @return `true` para seguir recibiendo avisos, `false` para dejar de recibirlos
     */
    boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height);
}
