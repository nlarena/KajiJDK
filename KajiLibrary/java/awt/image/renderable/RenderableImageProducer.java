package java.awt.image.renderable;

import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Vector;

/**
 * KajiLibrary's java.awt.image.renderable.RenderableImageProducer -- the bridge to the old AWT.
 *
 * <p>It turns a {@link RenderableImage} into an {@link ImageProducer}, which is what the 1995
 * {@code java.awt.Image} understands. It exists so that a chain of operations can be displayed with
 * the tools that already existed, and that is why the package ships it even though the two models
 * look nothing alike.
 *
 * <p>The two models push the image in opposite directions, and that is the awkwardness: the
 * renderable one is <b>on demand</b> --nobody computes anything until it is asked for-- and the
 * producer is <b>push</b>, it sends the pixels to whoever listens. This class joins the two by
 * rendering everything at once and sending the result line by line.
 *
 * <h2>It runs in the caller's thread</h2>
 *
 * <p>It implements {@link Runnable}, and {@link #startProduction} starts no thread: it calls
 * {@link #run} directly. Rendering can take a long time, and whoever wants it on another thread has
 * to set that up with this same object. This note called that a decision and said the other way
 * would be worse; it is a divergence from the JDK, whose {@code startProduction} starts a thread
 * named "RenderableImageProducer Thread" and returns at once.
 *
 * <p>{@link #requestTopDownLeftRightResend} does nothing, and it is not an omission: production is
 * already top-down and single-pass, so there is nothing to resend differently.
 */
public class RenderableImageProducer implements ImageProducer, Runnable {

    /** The image to produce. */
    private RenderableImage rdblImage;

    /** Which context to render it with; null for the default. */
    private RenderContext rc;

    /** Who are listening. */
    private Vector<ImageConsumer> ics = new Vector<ImageConsumer>();

    /**
     * @param rdblImage the image
     * @param rc the context, or null for {@link RenderableImage#createDefaultRendering}
     */
    public RenderableImageProducer(RenderableImage rdblImage, RenderContext rc) {
        this.rdblImage = rdblImage;
        this.rc = rc;
    }

    /** Changes the context. It affects the next production, not one in progress. */
    public synchronized void setRenderContext(RenderContext rc) {
        this.rc = rc;
    }

    /** Adds a consumer, if it was not there. */
    public synchronized void addConsumer(ImageConsumer ic) {
        if (!this.ics.contains(ic)) {
            this.ics.addElement(ic);
        }
    }

    /** Whether that consumer is registered. */
    public synchronized boolean isConsumer(ImageConsumer ic) {
        return this.ics.contains(ic);
    }

    /** Removes it. If it was not there, it does nothing. */
    public synchronized void removeConsumer(ImageConsumer ic) {
        this.ics.removeElement(ic);
    }

    /**
     * Adds it and produces.
     *
     * <p>It produces for <b>all</b> the registered consumers and not only for this one; see {@link
     * #run}.
     */
    public synchronized void startProduction(ImageConsumer ic) {
        addConsumer(ic);
        // In the caller's thread; see the class note.
        run();
    }

    /** It does nothing: production is already top-down and single-pass. */
    public void requestTopDownLeftRightResend(ImageConsumer ic) {
    }

    /**
     * Renders and sends the result, line by line.
     *
     * <p>Line by line and not all at once: a consumer can draw what arrives as it arrives, and
     * sending it all together would require two copies of the image in memory.
     */
    public void run() {
        RenderedImage rendered;
        if (this.rc != null) {
            rendered = this.rdblImage.createRendering(this.rc);
        } else {
            rendered = this.rdblImage.createDefaultRendering();
        }
        if (rendered == null) {
            // It could not be rendered: the failure is reported instead of staying silent.
            int k = 0;
            while (k < this.ics.size()) {
                this.ics.elementAt(k).imageComplete(ImageConsumer.IMAGEERROR);
                k = k + 1;
            }
            return;
        }
        ColorModel colorModel = rendered.getColorModel();
        Raster raster = rendered.getData();
        SampleModel sampleModel = raster.getSampleModel();
        DataBuffer dataBuffer = raster.getDataBuffer();
        if (colorModel == null) {
            colorModel = ColorModel.getRGBdefault();
        }
        int width = raster.getWidth();
        int height = raster.getHeight();

        int i = 0;
        while (i < this.ics.size()) {
            ImageConsumer ic = this.ics.elementAt(i);
            ic.setDimensions(width, height);
            ic.setHints(ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES
                | ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME);
            i = i + 1;
        }

        int[] line = new int[width];
        int[] pixel = new int[sampleModel.getNumBands()];
        int y = 0;
        while (y < height) {
            int x = 0;
            while (x < width) {
                sampleModel.getPixel(x, y, pixel, dataBuffer);
                line[x] = colorModel.getDataElement(pixel, 0);
                x = x + 1;
            }
            int j = 0;
            while (j < this.ics.size()) {
                this.ics.elementAt(j).setPixels(0, y, width, 1, colorModel, line, 0, width);
                j = j + 1;
            }
            y = y + 1;
        }

        int k = 0;
        while (k < this.ics.size()) {
            this.ics.elementAt(k).imageComplete(ImageConsumer.STATICIMAGEDONE);
            k = k + 1;
        }
    }
}
