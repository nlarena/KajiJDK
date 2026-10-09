package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.VolatileImage;

/**
 * A concrete way of using a {@link GraphicsDevice}: with such a colour depth and such capabilities.
 *
 * <p>It serves above all for asking for images that draw fast onto that device. An image created
 * with {@link #createCompatibleImage} has the same pixel format as the destination, so drawing it
 * is copying; one that does not have it has to be converted on every draw.
 *
 * <p>The two transforms it exposes answer different questions. {@link #getDefaultTransform} goes
 * from user coordinates to device pixels, and on a high-density screen it is not the identity.
 * {@link #getNormalizingTransform} goes to **real millimetres**: it is the one to use so that a
 * line of 72 units measures an inch on the screen and not 72 pixels.
 *
 * <p><strong>No volatile image can be created here.</strong> The four-argument {@link
 * #createCompatibleVolatileImage(int, int, ImageCapabilities, int)} always throws, so all four
 * overloads end in an exception; the two that do not declare {@code AWTException} wrap it in an
 * {@code InternalError}.
 */
public abstract class GraphicsConfiguration {

    /** For the subclasses. */
    protected GraphicsConfiguration() {
    }

    /** The device it belongs to. */
    public abstract GraphicsDevice getDevice();

    /**
     * An opaque image of this device's format.
     *
     * @throws IllegalArgumentException if the size is empty
     */
    public BufferedImage createCompatibleImage(int width, int height) {
        ColorModel model = this.getColorModel();
        java.awt.image.WritableRaster raster =
                model.createCompatibleWritableRaster(width, height);
        return new BufferedImage(model, raster, model.isAlphaPremultiplied(), null);
    }

    /**
     * An image of this device's format, with the transparency asked for.
     *
     * @throws IllegalArgumentException if the size is empty or the transparency is not one of the
     *     three
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
     * An opaque volatile image of this device's format.
     *
     * <p>It never returns one here: see the class note.
     *
     * @throws InternalError always, wrapping the {@code AWTException} of the four-argument version
     */
    public VolatileImage createCompatibleVolatileImage(int width, int height) {
        VolatileImage vi = null;
        try {
            vi = this.createCompatibleVolatileImage(width, height, null, Transparency.OPAQUE);
        } catch (AWTException e) {
            // The four-argument version always throws here, so this is the path taken: AWTException
            // is not part of this signature, and an InternalError says what happened without lying.
            throw new InternalError(e.getMessage());
        }
        return vi;
    }

    /**
     * A volatile image with the transparency asked for.
     *
     * <p>It never returns one here either: see the class note.
     *
     * @throws InternalError always, wrapping the {@code AWTException} of the four-argument version
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
     * An opaque volatile image with the capabilities asked for.
     *
     * @throws AWTException always here: without a device there are no capabilities to meet
     */
    public VolatileImage createCompatibleVolatileImage(int width, int height, ImageCapabilities caps)
            throws AWTException {
        return this.createCompatibleVolatileImage(width, height, caps, Transparency.OPAQUE);
    }

    /**
     * A volatile image with the capabilities and the transparency asked for.
     *
     * @throws AWTException always here: without a device there are no capabilities to meet
     */
    public VolatileImage createCompatibleVolatileImage(int width, int height,
            ImageCapabilities caps, int transparency) throws AWTException {
        throw new AWTException("Supported image capabilities are not met by this "
                + "graphics configuration: " + this);
    }

    /** The colour model of this device. */
    public abstract ColorModel getColorModel();

    /**
     * The colour model of this device for that transparency, or `null` if it does not support it.
     */
    public abstract ColorModel getColorModel(int transparency);

    /** From user coordinates to device pixels. */
    public abstract AffineTransform getDefaultTransform();

    /** From user coordinates to real physical measures. */
    public abstract AffineTransform getNormalizingTransform();

    /** The area this device takes up in the virtual coordinate space. */
    public abstract Rectangle getBounds();

    /** Which buffers it supports. */
    public BufferCapabilities getBufferCapabilities() {
        return new BufferCapabilities(new ImageCapabilities(false),
                new ImageCapabilities(false), null);
    }

    /** What capabilities its images have. */
    public ImageCapabilities getImageCapabilities() {
        return new ImageCapabilities(false);
    }

    /** Whether it supports windows with per-pixel translucency. */
    public boolean isTranslucencyCapable() {
        return false;
    }
}
