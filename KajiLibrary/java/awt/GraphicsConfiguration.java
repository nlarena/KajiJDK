package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.VolatileImage;

/**
 * Una manera concreta de usar un {@link GraphicsDevice}: con tal profundidad de color y tales
 * capacidades.
 *
 * <p>Sirve sobre todo para pedir imágenes que se dibujen rápido sobre ese dispositivo. Una imagen
 * creada con {@link #createCompatibleImage} tiene el mismo formato de píxel que el destino, así que
 * dibujarla es copiar; una que no lo tenga hay que convertirla en cada dibujado.
 *
 * <p>Las dos transformaciones que expone contestan preguntas distintas.
 * {@link #getDefaultTransform} lleva de coordenadas de usuario a píxeles del dispositivo, y en una
 * pantalla de alta densidad no es la identidad. {@link #getNormalizingTransform} lleva a
 * **milímetros de verdad**: es la que hay que usar para que una raya de 72 unidades mida una pulgada
 * en la pantalla y no 72 píxeles.
 */
public abstract class GraphicsConfiguration {

    /** Para las subclases. */
    protected GraphicsConfiguration() {
    }

    /** El dispositivo al que pertenece. */
    public abstract GraphicsDevice getDevice();

    /**
     * Una imagen opaca del formato de este dispositivo.
     *
     * @throws IllegalArgumentException si el tamaño es vacío
     */
    public BufferedImage createCompatibleImage(int width, int height) {
        ColorModel model = this.getColorModel();
        java.awt.image.WritableRaster raster =
                model.createCompatibleWritableRaster(width, height);
        return new BufferedImage(model, raster, model.isAlphaPremultiplied(), null);
    }

    /**
     * Una imagen del formato de este dispositivo, con la transparencia pedida.
     *
     * @throws IllegalArgumentException si el tamaño es vacío o la transparencia no es una de las
     *     tres
     */
    public BufferedImage createCompatibleImage(int width, int height, int transparency) {
        if (this.getColorModel().getTransparency() == transparency) {
            return this.createCompatibleImage(width, height);
        }
        ColorModel cm = this.getColorModel(transparency);
        if (cm == null) {
            throw new IllegalArgumentException("Unknown transparency: " + transparency);
        }
        java.awt.image.WritableRaster wr = cm.createCompatibleWritableRaster(width, height);
        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
    }

    /**
     * Una imagen volátil opaca del formato de este dispositivo.
     *
     * @throws IllegalArgumentException si el tamaño es vacío
     */
    public VolatileImage createCompatibleVolatileImage(int width, int height) {
        VolatileImage vi = null;
        try {
            vi = this.createCompatibleVolatileImage(width, height, null, Transparency.OPAQUE);
        } catch (AWTException e) {
            // No puede pasar: sin capacidades pedidas no hay nada que no se pueda cumplir.
            throw new InternalError(e.getMessage());
        }
        return vi;
    }

    /**
     * Una imagen volátil con la transparencia pedida.
     *
     * @throws IllegalArgumentException si el tamaño es vacío o la transparencia no es una de las
     *     tres
     */
    public VolatileImage createCompatibleVolatileImage(int width, int height, int transparency) {
        VolatileImage vi = null;
        try {
            vi = this.createCompatibleVolatileImage(width, height, null, transparency);
        } catch (AWTException e) {
            throw new InternalError(e.getMessage());
        }
        return vi;
    }

    /**
     * Una imagen volátil opaca con las capacidades pedidas.
     *
     * @throws AWTException si las capacidades no se pueden cumplir
     * @throws IllegalArgumentException si el tamaño es vacío
     */
    public VolatileImage createCompatibleVolatileImage(int width, int height, ImageCapabilities caps)
            throws AWTException {
        return this.createCompatibleVolatileImage(width, height, caps, Transparency.OPAQUE);
    }

    /**
     * Una imagen volátil con las capacidades y la transparencia pedidas.
     *
     * @throws AWTException si las capacidades no se pueden cumplir
     * @throws IllegalArgumentException si el tamaño es vacío o la transparencia no es una de las
     *     tres
     */
    public VolatileImage createCompatibleVolatileImage(int width, int height,
            ImageCapabilities caps, int transparency) throws AWTException {
        throw new AWTException("Supported image capabilities are not met by this "
                + "graphics configuration: " + this);
    }

    /** El modelo de color de este dispositivo. */
    public abstract ColorModel getColorModel();

    /** El modelo de color de este dispositivo para esa transparencia, o `null` si no la admite. */
    public abstract ColorModel getColorModel(int transparency);

    /** De coordenadas de usuario a píxeles del dispositivo. */
    public abstract AffineTransform getDefaultTransform();

    /** De coordenadas de usuario a medidas físicas reales. */
    public abstract AffineTransform getNormalizingTransform();

    /** El área que ocupa este dispositivo en el espacio de coordenadas virtual. */
    public abstract Rectangle getBounds();

    /** Qué buffers admite. */
    public BufferCapabilities getBufferCapabilities() {
        return new BufferCapabilities(new ImageCapabilities(false),
                new ImageCapabilities(false), null);
    }

    /** Qué capacidades tienen sus imágenes. */
    public ImageCapabilities getImageCapabilities() {
        return new ImageCapabilities(false);
    }

    /** Si admite ventanas con transparencia por píxel. */
    public boolean isTranslucencyCapable() {
        return false;
    }
}
