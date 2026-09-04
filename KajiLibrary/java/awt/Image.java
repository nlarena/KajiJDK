package java.awt;

import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.PixelGrabber;
import java.awt.image.ReplicateScaleFilter;

/**
 * Una imagen, que puede no estar entera todavía.
 *
 * <p>Esa última parte es lo que explica la forma rara de la clase. Cuando se diseñó, una imagen venía
 * por la red mientras la página ya se estaba mostrando, así que preguntarle el ancho tenía que poder
 * contestar "todavía no sé". De ahí que {@link #getWidth} tome un {@link ImageObserver}: devuelve -1
 * si no lo sabe, y le avisa al observador cuando se entera.
 *
 * <p>Una {@link BufferedImage} es el caso en el que esa asincronía no existe: los píxeles ya están
 * en memoria, el observador nunca se usa y el ancho se sabe siempre. Sigue siendo una `Image` porque
 * todo lo que dibuja imágenes está escrito contra esta clase.
 */
public abstract class Image {

    /** Lo que devuelve {@link #getProperty} cuando la propiedad no está definida. */
    public static final Object UndefinedProperty = new Object();

    /** Que el escalado elija el algoritmo. */
    public static final int SCALE_DEFAULT = 1;

    /** Que priorice la velocidad. */
    public static final int SCALE_FAST = 2;

    /** Que priorice la calidad. */
    public static final int SCALE_SMOOTH = 4;

    /** Repetir o saltear píxeles: el más rápido y el más feo. */
    public static final int SCALE_REPLICATE = 8;

    /** Promediar el área que cae en cada píxel: más lento y mucho mejor al achicar. */
    public static final int SCALE_AREA_AVERAGING = 16;

    private static final ImageCapabilities defaultImageCaps = new ImageCapabilities(false);

    /**
     * Cuánto conviene acelerar esta imagen, de 0 a 1.
     *
     * <p>Es una sugerencia sobre memoria escasa: una imagen que se dibuja en cada cuadro merece
     * quedarse en la memoria rápida y una que se dibuja una vez, no.
     */
    protected float accelerationPriority = 0.5f;

    /** Para las subclases. */
    protected Image() {
    }

    /**
     * El ancho, o -1 si todavía no se sabe.
     *
     * <p>El -1 no es un error: es "preguntá de nuevo cuando te avise".
     */
    public abstract int getWidth(ImageObserver observer);

    /** El alto, o -1 si todavía no se sabe. */
    public abstract int getHeight(ImageObserver observer);

    /** De dónde salen los píxeles. */
    public abstract ImageProducer getSource();

    /**
     * Un contexto para dibujar **sobre** esta imagen.
     *
     * @throws UnsupportedOperationException si la imagen no se puede dibujar encima
     */
    public abstract Graphics getGraphics();

    /**
     * Una propiedad de la imagen.
     *
     * @return el valor, `null` si todavía no se sabe, o {@link #UndefinedProperty} si no existe
     */
    public abstract Object getProperty(String name, ImageObserver observer);

    /**
     * La misma imagen a otro tamaño.
     *
     * <p>Con una de las dos medidas negativa se calcula a partir de la otra manteniendo la
     * proporción.
     *
     * <p>A diferencia del JDK, que devuelve una imagen perezosa que se calcula cuando se la dibuja,
     * acá el escalado se hace en el momento y sale una {@link BufferedImage} ya lista. El motivo es
     * que la versión perezosa necesita el sistema de ventanas para armar la imagen, y esta
     * biblioteca no lo tiene; el resultado es el mismo y la diferencia es cuándo se hace el trabajo.
     *
     * @throws IllegalArgumentException si las dos medidas son cero
     */
    public Image getScaledInstance(int width, int height, int hints) {
        ImageFilter filter;
        if ((hints & (SCALE_SMOOTH | SCALE_AREA_AVERAGING)) != 0) {
            filter = new AreaAveragingScaleFilter(width, height);
        } else {
            filter = new ReplicateScaleFilter(width, height);
        }
        ImageProducer prod = new FilteredImageSource(this.getSource(), filter);
        // Con el arreglo en null y las medidas en -1, el recolector reserva el suyo cuando el
        // filtro le anuncia el tamano final, que es lo unico que sabe cuanto mide el resultado.
        PixelGrabber pg = new PixelGrabber(prod, 0, 0, -1, -1, null, 0, 0);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
            return null;
        }
        int w = pg.getWidth();
        int h = pg.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = (int[]) pg.getPixels();
        out.setRGB(0, 0, w, h, pixels, 0, w);
        return out;
    }

    /**
     * Suelta los recursos y obliga a volver a calcular la imagen si se la vuelve a usar.
     *
     * <p>La implementación de acá no hace nada, que es lo correcto para una imagen que ya está
     * entera en memoria.
     */
    public void flush() {
    }

    /**
     * Qué se puede acelerar de esta imagen en esa configuración.
     *
     * @param gc la configuración, o `null` para la del dispositivo por omisión
     */
    public ImageCapabilities getCapabilities(GraphicsConfiguration gc) {
        return defaultImageCaps;
    }

    /**
     * Cambia cuánto conviene acelerar esta imagen.
     *
     * @throws IllegalArgumentException si el valor no está entre 0 y 1
     */
    public void setAccelerationPriority(float priority) {
        if (priority < 0 || priority > 1) {
            throw new IllegalArgumentException(
                    "Priority must be a value between 0 and 1, inclusive");
        }
        this.accelerationPriority = priority;
    }

    /** Cuánto conviene acelerar esta imagen. */
    public float getAccelerationPriority() {
        return this.accelerationPriority;
    }
}
