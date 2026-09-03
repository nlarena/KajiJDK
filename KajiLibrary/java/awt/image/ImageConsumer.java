package java.awt.image;

import java.util.Hashtable;

/**
 * Quien recibe los píxeles de una imagen a medida que se producen.
 *
 * <p>Es el otro extremo de {@link ImageProducer}, y juntos forman la tubería con la que AWT mueve
 * imágenes: el productor empuja, el consumidor recibe. Nada de esto devuelve la imagen entera de una
 * vez; la idea es que una imagen que llega por la red se pueda ir mostrando.
 *
 * <p>El orden de las llamadas importa. Primero las dimensiones y las propiedades, después el modelo
 * de color y las pistas, después los píxeles en una o varias tandas, y al final
 * {@link #imageComplete}, que es la única que dice si salió bien.
 *
 * <p>Las pistas de {@link #setHints} no son adorno: un consumidor que sabe de antemano que los
 * píxeles van a llegar de arriba abajo y con las filas completas puede escribir directo en su
 * destino sin guardar nada, y uno que no lo sabe tiene que estar preparado para recibirlos en
 * cualquier orden.
 */
public interface ImageConsumer {

    /** Los píxeles pueden llegar en cualquier orden. */
    int RANDOMPIXELORDER = 1;

    /** Los píxeles llegan de arriba abajo y de izquierda a derecha. */
    int TOPDOWNLEFTRIGHT = 2;

    /** Cada tanda trae filas enteras. */
    int COMPLETESCANLINES = 4;

    /** Cada píxel se entrega una sola vez. */
    int SINGLEPASS = 8;

    /** La imagen tiene un solo cuadro. */
    int SINGLEFRAME = 16;

    /** La producción se abortó. */
    int IMAGEABORTED = 1;

    /** La producción falló. */
    int IMAGEERROR = 2;

    /** Se terminó un cuadro de una imagen de varios. */
    int SINGLEFRAMEDONE = 3;

    /** La imagen está completa y no va a haber más. */
    int STATICIMAGEDONE = 4;

    /** El tamaño de la imagen. */
    void setDimensions(int width, int height);

    /** Las propiedades de la imagen. */
    void setProperties(Hashtable<?, ?> props);

    /** El modelo de color con el que van a venir la mayoría de los píxeles. */
    void setColorModel(ColorModel model);

    /** En qué orden y de qué forma van a llegar los píxeles. */
    void setHints(int hintflags);

    /** Una tanda de píxeles de un byte cada uno. */
    void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize);

    /** Una tanda de píxeles de un `int` cada uno. */
    void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize);

    /**
     * Se terminó la entrega.
     *
     * @param status `STATICIMAGEDONE`, `SINGLEFRAMEDONE`, `IMAGEERROR` o `IMAGEABORTED`
     */
    void imageComplete(int status);
}
