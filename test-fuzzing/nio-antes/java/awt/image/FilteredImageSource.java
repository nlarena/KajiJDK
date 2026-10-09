package java.awt.image;

import java.util.Hashtable;

/**
 * A producer that takes the pixels of another one and passes them through an {@link ImageFilter}.
 *
 * <p>It is the piece that builds the pipe: from the consumer's side it looks like an ordinary
 * producer, and from the original producer's side like an ordinary consumer. Chaining two is
 * wrapping one in the other.
 *
 * <p>It carries a table from consumer to filter because each consumer needs **its** copy of the
 * filter: the filter keeps the state of one delivery, and two simultaneous deliveries would step on
 * each other. That is why the filter passed to the constructor is a mould and is never used
 * directly.
 */
public class FilteredImageSource implements ImageProducer {

    private final ImageProducer src;
    private final ImageFilter filter;
    private Hashtable<ImageConsumer, ImageFilter> proxies;

    /**
     * With the given producer and filter.
     *
     * @throws NullPointerException if either of the two is missing
     */
    public FilteredImageSource(ImageProducer orig, ImageFilter imgf) {
        if (orig == null || imgf == null) {
            throw new NullPointerException();
        }
        this.src = orig;
        this.filter = imgf;
    }

    /** Adds a consumer, with its own copy of the filter. */
    public synchronized void addConsumer(ImageConsumer ic) {
        if (this.proxies == null) {
            this.proxies = new Hashtable<ImageConsumer, ImageFilter>();
        }
        if (!this.proxies.containsKey(ic)) {
            ImageFilter imgf = this.filter.getFilterInstance(ic);
            this.proxies.put(ic, imgf);
            this.src.addConsumer(imgf);
        }
    }

    /** Whether that consumer is registered. */
    public synchronized boolean isConsumer(ImageConsumer ic) {
        return this.proxies != null && this.proxies.containsKey(ic);
    }

    /** Removes that consumer and its copy of the filter. */
    public synchronized void removeConsumer(ImageConsumer ic) {
        if (this.proxies != null) {
            ImageFilter imgf = this.proxies.get(ic);
            if (imgf != null) {
                this.src.removeConsumer(imgf);
                this.proxies.remove(ic);
                if (this.proxies.isEmpty()) {
                    this.proxies = null;
                }
            }
        }
    }

    /** Registers it if need be and starts the delivery. */
    public synchronized void startProduction(ImageConsumer ic) {
        if (this.proxies == null) {
            this.proxies = new Hashtable<ImageConsumer, ImageFilter>();
        }
        ImageFilter imgf = this.proxies.get(ic);
        if (imgf == null) {
            imgf = this.filter.getFilterInstance(ic);
            this.proxies.put(ic, imgf);
        }
        this.src.startProduction(imgf);
    }

    /**
     * Asks for the redelivery from top to bottom.
     *
     * <p>The request is made to the **filter**, not to the producer: a filter that knows how to
     * reorder on its own resolves it without bothering the source.
     */
    public synchronized void requestTopDownLeftRightResend(ImageConsumer ic) {
        if (this.proxies != null) {
            ImageFilter imgf = this.proxies.get(ic);
            if (imgf != null) {
                imgf.resendTopDownLeftRight(this.src);
            }
        }
    }
}
