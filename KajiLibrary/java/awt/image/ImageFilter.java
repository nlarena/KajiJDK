package java.awt.image;

import java.util.Hashtable;

/**
 * A consumer that passes the pixels on to another consumer, changing them on the way.
 *
 * <p>It is at once the receiving end of a pipe and the sender of the next one, and that is why
 * filters chain. As it stands it changes no pixel: every method forwards, and the only thing it
 * adds is its own name to the `filters` property in {@link #setProperties}. It serves as a base and
 * as a null filter.
 *
 * <p>{@link #getFilterInstance} is the piece that makes a filter reusable. A filter describes a
 * transformation, but on being applied it keeps state —the image as it arrives— and that state
 * cannot be shared between two consumers. So the filter one builds is a **mould**: every time it is
 * connected to somebody it is cloned, and the clone is the one that works.
 */
public class ImageFilter implements ImageConsumer, Cloneable {

    /** Who the already filtered pixels are passed to. */
    protected ImageConsumer consumer;

    /** A null filter. */
    public ImageFilter() {
    }

    /**
     * A copy of this filter connected to that consumer.
     *
     * <p>It is what has to be called to use a filter: the original stays as a mould and the clone
     * carries the state of one concrete delivery.
     */
    public ImageFilter getFilterInstance(ImageConsumer ic) {
        ImageFilter instance = (ImageFilter) this.clone();
        instance.consumer = ic;
        return instance;
    }

    /** Forwards the size. */
    public void setDimensions(int width, int height) {
        this.consumer.setDimensions(width, height);
    }

    /**
     * Forwards the properties, adding this filter to the list of the ones the image went through.
     *
     * <p>The `filters` property leaves a record of where it went, which is the only thing left of
     * the chain once the image has arrived.
     */
    public void setProperties(Hashtable<?, ?> props) {
        Hashtable<Object, Object> p = copyProperties(props);
        Object o = p.get("filters");
        if (o == null) {
            p.put("filters", this.toString());
        } else if (o instanceof String) {
            p.put("filters", ((String) o) + this.toString());
        }
        this.consumer.setProperties(p);
    }

    /**
     * A copy of the table, with the keys and the values as `Object`.
     *
     * <p>It is copied entry by entry instead of cloned because the types have to be widened: the
     * one coming in can be of any pair of types and the one going out has to accept the keys the
     * filters add.
     */
    static Hashtable<Object, Object> copyProperties(Hashtable<?, ?> props) {
        Hashtable<Object, Object> p = new Hashtable<Object, Object>();
        java.util.Enumeration<?> e = props.keys();
        while (e.hasMoreElements()) {
            Object k = e.nextElement();
            p.put(k, props.get(k));
        }
        return p;
    }

    /** Forwards the colour model. */
    public void setColorModel(ColorModel model) {
        this.consumer.setColorModel(model);
    }

    /** Forwards the hints. */
    public void setHints(int hints) {
        this.consumer.setHints(hints);
    }

    /** Forwards the pixels of one byte. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize) {
        this.consumer.setPixels(x, y, w, h, model, pixels, off, scansize);
    }

    /** Forwards the pixels of one `int`. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize) {
        this.consumer.setPixels(x, y, w, h, model, pixels, off, scansize);
    }

    /** Forwards the end of the delivery. */
    public void imageComplete(int status) {
        this.consumer.imageComplete(status);
    }

    /**
     * Asks the producer to send everything again from top to bottom.
     *
     * <p>A filter that can deliver in that order even when it receives them out of order has to
     * redefine this and do the redelivery itself instead of passing the request on to the producer.
     */
    public void resendTopDownLeftRight(ImageProducer ip) {
        ip.requestTopDownLeftRightResend(this);
    }

    /** A shallow copy. */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // It cannot happen: this class declares Cloneable.
            throw new InternalError(e.toString());
        }
    }
}
