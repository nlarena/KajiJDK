package java.awt.image;

import java.util.Vector;

/**
 * The producer that delivers the pixels of a {@link BufferedImage}.
 *
 * <p>It is what lets an image in memory be put into the filter pipe: from the consumer's side there
 * is no telling it apart from an image arriving over the network, except that this one arrives
 * whole and in one go.
 *
 * <p>It delivers in ARGB of eight bits per channel and not in the format of the image. It is one
 * conversion too many when the two coincide, but it is the only thing any consumer knows how to
 * read without asking, and the alternative —delivering in the model of the image— would force every
 * filter to know about indexed and packed colour models.
 *
 * <p>It is not public: the JDK's `java.awt.image` does not declare this class, only
 * {@link BufferedImage#getSource} uses it.
 */
class BufferedImageSource implements ImageProducer {

    private final BufferedImage image;
    private final Vector<ImageConsumer> theConsumers = new Vector<ImageConsumer>();

    /** With the image it is going to deliver. */
    BufferedImageSource(BufferedImage image) {
        this.image = image;
    }

    /** Adds a consumer and delivers the whole image to it on the spot. */
    public synchronized void addConsumer(ImageConsumer ic) {
        if (this.theConsumers.contains(ic)) {
            return;
        }
        this.theConsumers.addElement(ic);
        try {
            this.deliver(ic);
        } finally {
            this.theConsumers.removeElement(ic);
        }
    }

    /** Whether that consumer is receiving right now. */
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

    /** There is no need: this source already delivers from top to bottom. */
    public void requestTopDownLeftRightResend(ImageConsumer ic) {
    }

    /** Sends it the size, the model, the hints and the pixels, row by row. */
    private void deliver(ImageConsumer ic) {
        int w = this.image.getWidth();
        int h = this.image.getHeight();
        ColorModel rgb = ColorModel.getRGBdefault();
        ic.setDimensions(w, h);
        ic.setColorModel(rgb);
        ic.setHints(ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES
                | ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME);
        // Row by row and not the whole image: the temporary array is reused, and a big image does
        // not need a complete copy of itself in memory in order to be delivered.
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            this.image.getRGB(0, y, w, 1, row, 0, w);
            ic.setPixels(0, y, w, 1, rgb, row, 0, w);
        }
        ic.imageComplete(ImageConsumer.STATICIMAGEDONE);
    }
}
