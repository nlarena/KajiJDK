package java.awt.image;

import java.awt.Image;
import java.util.Hashtable;

/**
 * The bridge back: it turns the asynchronous pipe of {@link ImageProducer} into an array of pixels
 * and a call that waits.
 *
 * <p>Everything else in this package pushes pixels forward. This is what gathers them: it registers
 * as a consumer, lets the image arrive, and {@link #grabPixels()} blocks until it is finished. It
 * is the way to get the pixels out of any `Image`, wherever it comes from.
 *
 * <p>There are two ways of using it and the difference matters. If it is given an array, it writes
 * there and respects the colour model the image brings; if not, it reserves its own when it learns
 * the size. The constructor with `forceRGB` asks besides that everything be converted to ARGB of
 * eight bits per channel, which is what one wants when the colours have to be looked at and not
 * just copied.
 *
 * <p>That conversion can happen **halfway**: as long as every batch comes with the same colour
 * model the raw pixels are kept, and as soon as one arrives with another model everything gathered
 * so far has to be taken to ARGB, because there is no model that serves for both.
 *
 * <p>Finishing without an error does **not** guarantee a complete image, and this note used to say
 * the trap was the other way round. `grabPixels` returns `true` when `FRAMEBITS` or `ALLBITS` is
 * set, so an image that was aborted halfway, one that failed and a wait that timed out all return
 * `false`; what returns `true` without the image being whole is one frame of a multi-frame image,
 * which sets `FRAMEBITS`. To know that everything is there, {@link #getStatus} has to be asked for
 * `ALLBITS`.
 */
public class PixelGrabber implements ImageConsumer {

    private final ImageProducer producer;
    private final int dstX;
    private final int dstY;
    private int dstW;
    private int dstH;
    private ColorModel imageModel;
    private byte[] bytePixels;
    private int[] intPixels;
    private int dstOff;
    private int dstScan;
    private boolean grabbing;
    private int flags;

    private static final int GRABBEDBITS = ImageObserver.FRAMEBITS | ImageObserver.ALLBITS;
    private static final int DONEBITS = GRABBEDBITS | ImageObserver.ERROR;

    /**
     * Gathers a rectangle of an image into the given array, in ARGB.
     *
     * @throws NullPointerException if the image is `null`
     */
    public PixelGrabber(Image img, int x, int y, int w, int h, int[] pix, int off, int scansize) {
        this(img.getSource(), x, y, w, h, pix, off, scansize);
    }

    /**
     * Gathers a rectangle of a producer into the given array, in ARGB.
     *
     * <p>With `pix` as `null` and the measures as -1, the array is reserved when the producer
     * announces the size.
     */
    public PixelGrabber(ImageProducer ip, int x, int y, int w, int h, int[] pix, int off,
            int scansize) {
        this.producer = ip;
        this.dstX = x;
        this.dstY = y;
        this.dstW = w;
        this.dstH = h;
        this.dstOff = off;
        this.dstScan = scansize;
        this.intPixels = pix;
        this.imageModel = ColorModel.getRGBdefault();
    }

    /**
     * Gathers a rectangle of an image into an array of its own.
     *
     * <p>With `forceRGB` as `false` the pixels are kept just as they come and the colour model is
     * left in {@link #getColorModel}; with `true` everything is converted to ARGB.
     *
     * @throws NullPointerException if the image is `null`
     */
    public PixelGrabber(Image img, int x, int y, int w, int h, boolean forceRGB) {
        this.producer = img.getSource();
        this.dstX = x;
        this.dstY = y;
        this.dstW = w;
        this.dstH = h;
        if (forceRGB) {
            this.imageModel = ColorModel.getRGBdefault();
        }
    }

    /** Starts the delivery without waiting for it. */
    public synchronized void startGrabbing() {
        if ((this.flags & DONEBITS) != 0) {
            return;
        }
        if (!this.grabbing) {
            this.grabbing = true;
            this.flags = this.flags & ~ImageObserver.ABORT;
            this.producer.startProduction(this);
        }
    }

    /** Cuts the delivery short. */
    public synchronized void abortGrabbing() {
        this.imageComplete(ImageConsumer.IMAGEABORTED);
    }

    /**
     * Starts the delivery if need be and waits until it finishes.
     *
     * @return `true` if pixels were gathered, which does **not** mean the image is complete
     * @throws InterruptedException if the thread is interrupted while it waits
     */
    public boolean grabPixels() throws InterruptedException {
        return this.grabPixels(0);
    }

    /**
     * Like the previous one, with a deadline.
     *
     * @param ms how long to wait at most, or 0 to wait with no deadline
     * @throws InterruptedException if the thread is interrupted while it waits
     */
    public synchronized boolean grabPixels(long ms) throws InterruptedException {
        if ((this.flags & DONEBITS) != 0) {
            return (this.flags & GRABBEDBITS) != 0;
        }
        long end = ms + System.currentTimeMillis();
        if (!this.grabbing) {
            this.grabbing = true;
            this.flags = this.flags & ~ImageObserver.ABORT;
            this.producer.startProduction(this);
        }
        while (this.grabbing) {
            long timeout;
            if (ms == 0) {
                timeout = 0;
            } else {
                timeout = end - System.currentTimeMillis();
                if (timeout <= 0) {
                    break;
                }
            }
            this.wait(timeout);
        }
        return (this.flags & GRABBEDBITS) != 0;
    }

    /**
     * The {@link ImageObserver} flags that describe how it finished.
     *
     * @deprecated the name does not say that it returns status flags. Use {@link #getStatus}.
     */
    @Deprecated
    public synchronized int status() {
        return this.flags;
    }

    /** The {@link ImageObserver} flags that describe how it finished. */
    public synchronized int getStatus() {
        return this.flags;
    }

    /** The width of what was gathered, or -1 if it is not known yet. */
    public synchronized int getWidth() {
        return this.dstW < 0 ? -1 : this.dstW;
    }

    /** The height of what was gathered, or -1 if it is not known yet. */
    public synchronized int getHeight() {
        return this.dstH < 0 ? -1 : this.dstH;
    }

    /**
     * The array with the pixels: a `byte[]` or an `int[]`.
     *
     * <p>{@link #getColorModel} has to be asked how to read them, unless ARGB was asked for.
     */
    public synchronized Object getPixels() {
        if (this.bytePixels == null) {
            return this.intPixels;
        }
        return this.bytePixels;
    }

    /**
     * The colour model of the gathered pixels.
     *
     * <p>It may not be the one of the image: if the batches came with different models, everything
     * was converted to ARGB and this returns the ARGB model.
     */
    public synchronized ColorModel getColorModel() {
        return this.imageModel;
    }

    /** Records the size and reserves the array if need be. */
    public void setDimensions(int width, int height) {
        if (this.dstW < 0) {
            this.dstW = width - this.dstX;
        }
        if (this.dstH < 0) {
            this.dstH = height - this.dstY;
        }
        if (this.dstW <= 0 || this.dstH <= 0) {
            this.imageComplete(ImageConsumer.STATICIMAGEDONE);
        } else if (this.intPixels == null && this.imageModel == ColorModel.getRGBdefault()) {
            this.intPixels = new int[this.dstW * this.dstH];
            this.dstScan = this.dstW;
            this.dstOff = 0;
        }
        this.flags = this.flags | ImageObserver.WIDTH | ImageObserver.HEIGHT;
    }

    /** It does nothing: this consumer accepts any order. */
    public void setHints(int hints) {
    }

    /** It does nothing: this consumer does not keep properties. */
    public void setProperties(Hashtable<?, ?> props) {
    }

    /**
     * It does nothing.
     *
     * <p>The colour model is not taken from here but from each batch: the producer announces the
     * one it is going to use for most of them, but it may send batches with another.
     */
    public void setColorModel(ColorModel model) {
    }

    /**
     * Takes everything gathered so far to ARGB.
     *
     * <p>It is needed when a batch arrives with a colour model different from that of the previous
     * ones: there is no model that describes both, so everything moves to the only common one.
     */
    private void convertToRGB() {
        int size = this.dstW * this.dstH;
        int[] newpixels = new int[size];
        if (this.bytePixels != null) {
            for (int i = 0; i < size; i++) {
                newpixels[i] = this.imageModel.getRGB(this.bytePixels[i] & 0xFF);
            }
        } else if (this.intPixels != null) {
            for (int i = 0; i < size; i++) {
                newpixels[i] = this.imageModel.getRGB(this.intPixels[i]);
            }
        }
        this.bytePixels = null;
        this.intPixels = newpixels;
        this.dstScan = this.dstW;
        this.dstOff = 0;
        this.imageModel = ColorModel.getRGBdefault();
    }

    /** Crops the batch to the rectangle asked for; returns `null` if nothing is left. */
    private int[] cropBatch(int srcX, int srcY, int srcW, int srcH, int srcOff, int srcScan) {
        int x = srcX;
        int y = srcY;
        int w = srcW;
        int h = srcH;
        int off = srcOff;
        if (y < this.dstY) {
            int diff = this.dstY - y;
            if (diff >= h) {
                return null;
            }
            off = off + srcScan * diff;
            y = y + diff;
            h = h - diff;
        }
        if (y + h > this.dstY + this.dstH) {
            h = this.dstY + this.dstH - y;
            if (h <= 0) {
                return null;
            }
        }
        if (x < this.dstX) {
            int diff = this.dstX - x;
            if (diff >= w) {
                return null;
            }
            off = off + diff;
            x = x + diff;
            w = w - diff;
        }
        if (x + w > this.dstX + this.dstW) {
            w = this.dstX + this.dstW - x;
            if (w <= 0) {
                return null;
            }
        }
        int[] r = new int[5];
        r[0] = x;
        r[1] = y;
        r[2] = w;
        r[3] = h;
        r[4] = off;
        return r;
    }

    /** Stores a batch of pixels of one byte. */
    public void setPixels(int srcX, int srcY, int srcW, int srcH, ColorModel model, byte[] pixels,
            int srcOff, int srcScan) {
        int[] r = this.cropBatch(srcX, srcY, srcW, srcH, srcOff, srcScan);
        if (r == null) {
            return;
        }
        int x = r[0];
        int y = r[1];
        int w = r[2];
        int h = r[3];
        int off = r[4];
        if (this.intPixels == null) {
            if (this.bytePixels == null) {
                this.bytePixels = new byte[this.dstW * this.dstH];
                this.dstScan = this.dstW;
                this.dstOff = 0;
                this.imageModel = model;
            } else if (this.imageModel != model) {
                this.convertToRGB();
            }
        }
        int dstPtr = this.dstOff + (y - this.dstY) * this.dstScan + (x - this.dstX);
        if (this.intPixels == null) {
            int srcPtr = off;
            for (int row = h; row > 0; row--) {
                System.arraycopy(pixels, srcPtr, this.bytePixels, dstPtr, w);
                srcPtr = srcPtr + srcScan;
                dstPtr = dstPtr + this.dstScan;
            }
        } else {
            int dstRem = this.dstScan - w;
            int srcRem = srcScan - w;
            int srcPtr = off;
            for (int row = h; row > 0; row--) {
                for (int col = w; col > 0; col--) {
                    this.intPixels[dstPtr] = model.getRGB(pixels[srcPtr] & 0xFF);
                    dstPtr = dstPtr + 1;
                    srcPtr = srcPtr + 1;
                }
                srcPtr = srcPtr + srcRem;
                dstPtr = dstPtr + dstRem;
            }
        }
        this.flags = this.flags | ImageObserver.SOMEBITS;
    }

    /** Stores a batch of pixels of one `int`. */
    public void setPixels(int srcX, int srcY, int srcW, int srcH, ColorModel model, int[] pixels,
            int srcOff, int srcScan) {
        int[] r = this.cropBatch(srcX, srcY, srcW, srcH, srcOff, srcScan);
        if (r == null) {
            return;
        }
        int x = r[0];
        int y = r[1];
        int w = r[2];
        int h = r[3];
        int off = r[4];
        if (this.intPixels == null) {
            this.convertToRGB();
        }
        boolean convert = this.imageModel != model;
        int dstPtr = this.dstOff + (y - this.dstY) * this.dstScan + (x - this.dstX);
        int dstRem = this.dstScan - w;
        int srcRem = srcScan - w;
        int srcPtr = off;
        for (int row = h; row > 0; row--) {
            for (int col = w; col > 0; col--) {
                if (convert) {
                    this.intPixels[dstPtr] = model.getRGB(pixels[srcPtr]);
                } else {
                    this.intPixels[dstPtr] = pixels[srcPtr];
                }
                dstPtr = dstPtr + 1;
                srcPtr = srcPtr + 1;
            }
            srcPtr = srcPtr + srcRem;
            dstPtr = dstPtr + dstRem;
        }
        this.flags = this.flags | ImageObserver.SOMEBITS;
    }

    /** Records how the delivery finished and wakes whoever is waiting. */
    public synchronized void imageComplete(int status) {
        this.grabbing = false;
        if (status == ImageConsumer.IMAGEABORTED) {
            this.flags = this.flags | ImageObserver.ABORT;
        } else if (status == ImageConsumer.STATICIMAGEDONE) {
            this.flags = this.flags | ImageObserver.ALLBITS;
        } else if (status == ImageConsumer.SINGLEFRAMEDONE) {
            this.flags = this.flags | ImageObserver.FRAMEBITS;
        } else {
            this.flags = this.flags | ImageObserver.ERROR | ImageObserver.ABORT;
        }
        this.producer.removeConsumer(this);
        this.notifyAll();
    }
}
