package java.awt.image;

import java.util.Hashtable;
import java.util.Vector;

/**
 * A producer whose image **is already** in an array in memory.
 *
 * <p>It is the simple end of the pipe: there is nothing to download and nothing to decode, so the
 * delivery is a single call with the whole array. It serves for making an image out of computed
 * pixels.
 *
 * <p>It also serves for the opposite of what its name suggests: with {@link #setAnimated} the same
 * object becomes a **live** image. The consumers are not unsubscribed when it finishes and every
 * {@link #newPixels()} sends them whatever is in the array again, so writing into the array and
 * giving notice is all it takes to animate.
 *
 * <p>{@link #setFullBufferUpdates} decides whether each update sends the whole image or only the
 * rectangle that changed. Sending too much costs bandwidth; sending too little forces the consumer
 * to accept pixels in any order, and that stops a filter such as
 * {@link AreaAveragingScaleFilter} from doing its job. That is why the decision is declared: it
 * changes the hints the consumers receive.
 *
 * <p>The array is not copied. Writing into it changes what the next consumers are going to see,
 * which is exactly what makes the animation possible, and also what makes sharing it between
 * threads without care a problem.
 */
public class MemoryImageSource implements ImageProducer {

    private int width;
    private int height;
    private ColorModel model;
    private Object pixels;
    private int pixeloffset;
    private int pixelscan;
    private Hashtable<?, ?> properties;
    private final Vector<ImageConsumer> theConsumers = new Vector<ImageConsumer>();
    private boolean animating;
    private boolean fullbuffers;

    /** With pixels of one byte and the given colour model. */
    public MemoryImageSource(int w, int h, ColorModel cm, byte[] pix, int off, int scan) {
        this.initialize(w, h, cm, pix, off, scan, null);
    }

    /** Like the previous one, with properties. */
    public MemoryImageSource(int w, int h, ColorModel cm, byte[] pix, int off, int scan,
            Hashtable<?, ?> props) {
        this.initialize(w, h, cm, pix, off, scan, props);
    }

    /** With pixels of one `int` and the given colour model. */
    public MemoryImageSource(int w, int h, ColorModel cm, int[] pix, int off, int scan) {
        this.initialize(w, h, cm, pix, off, scan, null);
    }

    /** Like the previous one, with properties. */
    public MemoryImageSource(int w, int h, ColorModel cm, int[] pix, int off, int scan,
            Hashtable<?, ?> props) {
        this.initialize(w, h, cm, pix, off, scan, props);
    }

    /** With ARGB pixels of eight bits per channel. */
    public MemoryImageSource(int w, int h, int[] pix, int off, int scan) {
        this.initialize(w, h, ColorModel.getRGBdefault(), pix, off, scan, null);
    }

    /** Like the previous one, with properties. */
    public MemoryImageSource(int w, int h, int[] pix, int off, int scan, Hashtable<?, ?> props) {
        this.initialize(w, h, ColorModel.getRGBdefault(), pix, off, scan, props);
    }

    /** Stores everything that describes the image. */
    private void initialize(int w, int h, ColorModel cm, Object pix, int off, int scan,
            Hashtable<?, ?> props) {
        this.width = w;
        this.height = h;
        this.model = cm;
        this.pixels = pix;
        this.pixeloffset = off;
        this.pixelscan = scan;
        this.properties = props;
    }

    /**
     * Adds a consumer and delivers the whole image to it on the spot.
     *
     * <p>If it is not animated, it also unsubscribes it when it finishes: there is not going to be
     * anything more to send it.
     */
    public synchronized void addConsumer(ImageConsumer ic) {
        if (this.theConsumers.contains(ic)) {
            return;
        }
        this.theConsumers.addElement(ic);
        try {
            this.initConsumer(ic);
            this.sendPixels(ic, 0, 0, this.width, this.height);
            if (this.isConsumer(ic)) {
                if (this.animating) {
                    ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
                } else {
                    ic.imageComplete(ImageConsumer.STATICIMAGEDONE);
                    this.removeConsumer(ic);
                }
            }
        } catch (RuntimeException e) {
            if (this.isConsumer(ic)) {
                ic.imageComplete(ImageConsumer.IMAGEERROR);
            }
        }
    }

    /** Whether that consumer is registered. */
    public synchronized boolean isConsumer(ImageConsumer ic) {
        return this.theConsumers.contains(ic);
    }

    /** Removes that consumer. */
    public synchronized void removeConsumer(ImageConsumer ic) {
        this.theConsumers.removeElement(ic);
    }

    /** Registers it and delivers the image to it. */
    public void startProduction(ImageConsumer ic) {
        this.addConsumer(ic);
    }

    /**
     * Sends it the image again from top to bottom.
     *
     * <p>It is free: the pixels are in memory already and they are always sent in that order.
     */
    public void requestTopDownLeftRightResend(ImageConsumer ic) {
        // Nothing is needed: this source already delivers from top to bottom and in one go.
    }

    /**
     * Declares whether the image is going to change over time.
     *
     * <p>It has to be called **before** the first consumer registers: the ones that have received a
     * static image already unsubscribed and are not going to see the changes.
     */
    public synchronized void setAnimated(boolean animated) {
        this.animating = animated;
        if (!this.animating) {
            // The consumers that are left were waiting for more frames; the delivery has to be
            // closed for them before letting them go, or they wait forever.
            int n = this.theConsumers.size();
            for (int i = 0; i < n; i++) {
                ImageConsumer ic = this.theConsumers.elementAt(i);
                ic.imageComplete(ImageConsumer.STATICIMAGEDONE);
            }
            this.theConsumers.removeAllElements();
        }
    }

    /**
     * Declares whether each update sends the whole image.
     *
     * <p>It only has an effect on an animated image.
     */
    public synchronized void setFullBufferUpdates(boolean fullbuffers) {
        if (this.fullbuffers == fullbuffers) {
            return;
        }
        this.fullbuffers = fullbuffers;
        if (this.animating) {
            int n = this.theConsumers.size();
            for (int i = 0; i < n; i++) {
                ImageConsumer ic = this.theConsumers.elementAt(i);
                ic.setHints(fullbuffers
                        ? ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES
                        : ImageConsumer.RANDOMPIXELORDER);
            }
        }
    }

    /** Sends the whole image again. */
    public void newPixels() {
        this.newPixels(0, 0, this.width, this.height, true);
    }

    /** Sends that rectangle again. */
    public synchronized void newPixels(int x, int y, int w, int h) {
        this.newPixels(x, y, w, h, true);
    }

    /**
     * Sends that rectangle again, giving notice or not that a frame was completed.
     *
     * <p>Not giving notice serves for sending several pieces and only afterwards declaring the
     * frame complete, so that the consumer does not draw a half-updated image.
     */
    public synchronized void newPixels(int x, int y, int w, int h, boolean framenotify) {
        if (!this.animating) {
            return;
        }
        int x1 = x;
        int y1 = y;
        int w1 = w;
        int h1 = h;
        if (this.fullbuffers) {
            x1 = 0;
            y1 = 0;
            w1 = this.width;
            h1 = this.height;
        } else {
            if (x1 < 0) {
                w1 = w1 + x1;
                x1 = 0;
            }
            if (x1 + w1 > this.width) {
                w1 = this.width - x1;
            }
            if (y1 < 0) {
                h1 = h1 + y1;
                y1 = 0;
            }
            if (y1 + h1 > this.height) {
                h1 = this.height - y1;
            }
        }
        if ((w1 <= 0 || h1 <= 0) && !framenotify) {
            return;
        }
        int n = this.theConsumers.size();
        for (int i = 0; i < n; i++) {
            ImageConsumer ic = this.theConsumers.elementAt(i);
            if (w1 > 0 && h1 > 0) {
                this.sendPixels(ic, x1, y1, w1, h1);
            }
            if (framenotify && this.isConsumer(ic)) {
                ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
            }
        }
    }

    /** Changes the pixel array for one of bytes and sends the whole image. */
    public synchronized void newPixels(byte[] newpix, ColorModel newmodel, int offset,
            int scansize) {
        this.pixels = newpix;
        this.model = newmodel;
        this.pixeloffset = offset;
        this.pixelscan = scansize;
        this.newPixels();
    }

    /** Changes the pixel array for one of ints and sends the whole image. */
    public synchronized void newPixels(int[] newpix, ColorModel newmodel, int offset,
            int scansize) {
        this.pixels = newpix;
        this.model = newmodel;
        this.pixeloffset = offset;
        this.pixelscan = scansize;
        this.newPixels();
    }

    /** Announces the size, the properties, the model and the hints to the consumer. */
    private void initConsumer(ImageConsumer ic) {
        if (this.isConsumer(ic)) {
            ic.setDimensions(this.width, this.height);
        }
        if (this.isConsumer(ic) && this.properties != null) {
            ic.setProperties(this.properties);
        }
        if (this.isConsumer(ic)) {
            ic.setColorModel(this.model);
        }
        if (this.isConsumer(ic)) {
            int hints;
            if (this.animating) {
                if (this.fullbuffers) {
                    hints = ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES;
                } else {
                    hints = ImageConsumer.RANDOMPIXELORDER;
                }
            } else {
                hints = ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES
                        | ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME;
            }
            ic.setHints(hints);
        }
    }

    /** Sends it a rectangle of pixels, without copying the array. */
    private void sendPixels(ImageConsumer ic, int x, int y, int w, int h) {
        int off = this.pixeloffset + this.pixelscan * y + x;
        int w1 = w < 0 ? this.width - x : w;
        int h1 = h < 0 ? this.height - y : h;
        if (this.pixels instanceof byte[]) {
            ic.setPixels(x, y, w1, h1, this.model, (byte[]) this.pixels, off, this.pixelscan);
        } else {
            ic.setPixels(x, y, w1, h1, this.model, (int[]) this.pixels, off, this.pixelscan);
        }
    }
}
