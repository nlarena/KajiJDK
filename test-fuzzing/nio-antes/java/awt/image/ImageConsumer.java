package java.awt.image;

import java.util.Hashtable;

/**
 * Whoever receives the pixels of an image as they are produced.
 *
 * <p>It is the other end of {@link ImageProducer}, and together they form the pipe AWT moves images
 * with: the producer pushes, the consumer receives. None of this returns the whole image at once;
 * the idea is that an image arriving over the network can be shown as it comes.
 *
 * <p>The order of the calls matters. First the dimensions and the properties, then the colour model
 * and the hints, then the pixels in one or more batches, and at the end {@link #imageComplete},
 * which is the only one that says whether it went well.
 *
 * <p>The hints of {@link #setHints} are not decoration: a consumer that knows beforehand that the
 * pixels are going to arrive from top to bottom and with whole rows can write straight into its
 * destination without keeping anything, and one that does not know has to be ready to receive them
 * in any order.
 */
public interface ImageConsumer {

    /** The pixels may arrive in any order. */
    int RANDOMPIXELORDER = 1;

    /** The pixels arrive from top to bottom and from left to right. */
    int TOPDOWNLEFTRIGHT = 2;

    /** Each batch brings whole rows. */
    int COMPLETESCANLINES = 4;

    /** Each pixel is delivered exactly once. */
    int SINGLEPASS = 8;

    /** The image has a single frame. */
    int SINGLEFRAME = 16;

    /** The production was aborted. */
    int IMAGEABORTED = 1;

    /** The production failed. */
    int IMAGEERROR = 2;

    /** A frame of a multi-frame image is finished. */
    int SINGLEFRAMEDONE = 3;

    /** The image is complete and there will be no more. */
    int STATICIMAGEDONE = 4;

    /** The size of the image. */
    void setDimensions(int width, int height);

    /** The properties of the image. */
    void setProperties(Hashtable<?, ?> props);

    /** The colour model most of the pixels are going to come with. */
    void setColorModel(ColorModel model);

    /** In which order and in which shape the pixels are going to arrive. */
    void setHints(int hintflags);

    /** A batch of pixels of one byte each. */
    void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize);

    /** A batch of pixels of one `int` each. */
    void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize);

    /**
     * The delivery is finished.
     *
     * @param status `STATICIMAGEDONE`, `SINGLEFRAMEDONE`, `IMAGEERROR` or `IMAGEABORTED`
     */
    void imageComplete(int status);
}
