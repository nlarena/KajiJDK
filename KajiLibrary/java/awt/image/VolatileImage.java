package java.awt.image;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.Transparency;

/**
 * Una imagen que vive en la memoria del dispositivo de video y que **puede desaparecer**.
 *
 * <p>Ésa es toda la idea. Guardarla ahí la hace muchísimo más rápida de dibujar, pero esa memoria no
 * es de nadie: el sistema se la puede llevar cuando cambia la resolución, cuando se bloquea la
 * pantalla o cuando otro programa la necesita. La imagen no se corrompe, se **vacía**, y hay que
 * volver a dibujarla.
 *
 * <p>De ahí el par de métodos que la definen. {@link #validate} se llama antes de usarla y dice si
 * hay que rehacer algo; {@link #contentsLost} se llama después de dibujarla y dice si lo que se
 * dibujó llegó a destino. Hacen falta los dos porque la memoria se puede perder **durante** el
 * dibujado, no sólo entre dos usos, y el bucle correcto es:
 *
 * <pre>do {
 *     if (vi.validate(gc) == IMAGE_INCOMPATIBLE) vi = crearla(gc);
 *     dibujarEnLaImagen(vi);
 *     dibujarLaImagen(vi);
 * } while (vi.contentsLost());</pre>
 *
 * <p>Es la clase que hace posible el doble buffer sin parpadeo, y también la razón por la que ese
 * código se ve raro la primera vez: el `do/while` no es paranoia, es la única forma de escribir la
 * secuencia sin una ventana en la que se pierda un cuadro.
 */
public abstract class VolatileImage extends Image implements Transparency {

    /** La imagen está intacta y se puede usar. */
    public static final int IMAGE_OK = 0;

    /** Se había perdido y se restauró vacía: hay que volver a dibujarla. */
    public static final int IMAGE_RESTORED = 1;

    /** Ya no sirve para este dispositivo: hay que crear otra. */
    public static final int IMAGE_INCOMPATIBLE = 2;

    /** `OPAQUE`, `BITMASK` o `TRANSLUCENT`. */
    protected int transparency = Transparency.TRANSLUCENT;

    /** Para las subclases. */
    protected VolatileImage() {
    }

    /**
     * Una copia **no volátil** de lo que hay ahora.
     *
     * <p>Es la forma de sacar los píxeles de acá: una {@link BufferedImage} está en memoria común y
     * no se pierde.
     */
    public abstract BufferedImage getSnapshot();

    /** Ancho, en píxeles. */
    public abstract int getWidth();

    /** Alto, en píxeles. */
    public abstract int getHeight();

    /**
     * Un productor con los píxeles de esta imagen.
     *
     * <p>Va por {@link #getSnapshot}: los píxeles que salen son los del momento en que se pidieron,
     * porque la imagen puede cambiar o perderse mientras se los entrega.
     */
    public ImageProducer getSource() {
        return this.getSnapshot().getSource();
    }

    /** Un contexto para dibujar sobre esta imagen. */
    public Graphics getGraphics() {
        return this.createGraphics();
    }

    /** Un contexto para dibujar sobre esta imagen. */
    public abstract Graphics2D createGraphics();

    /**
     * Comprueba el estado de la imagen y la restaura si hace falta.
     *
     * @return {@link #IMAGE_OK}, {@link #IMAGE_RESTORED} —hay que volver a dibujarla— o
     *     {@link #IMAGE_INCOMPATIBLE} —hay que crear otra—
     */
    public abstract int validate(GraphicsConfiguration gc);

    /**
     * Si el contenido se perdió desde la última comprobación.
     *
     * <p>Hay que llamarlo **después** de dibujar: un `false` de antes no dice nada sobre lo que pasó
     * durante el dibujado.
     */
    public abstract boolean contentsLost();

    /** Qué se puede acelerar de esta imagen. */
    public abstract ImageCapabilities getCapabilities();

    /** `OPAQUE`, `BITMASK` o `TRANSLUCENT`. */
    public int getTransparency() {
        return this.transparency;
    }
}
