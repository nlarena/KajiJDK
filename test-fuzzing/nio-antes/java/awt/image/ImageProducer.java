package java.awt.image;

/**
 * The source of the pixels of an image.
 *
 * <p>It is the other end of {@link ImageConsumer}. A producer can have several consumers at once,
 * and each one receives the whole image: registering is not sharing out the work but joining the
 * delivery.
 */
public interface ImageProducer {

    /**
     * Adds a consumer and starts delivering to it.
     *
     * <p>Registering the same consumer twice is not defined and is better avoided.
     */
    void addConsumer(ImageConsumer ic);

    /** Whether that consumer is registered. */
    boolean isConsumer(ImageConsumer ic);

    /** Removes that consumer; if it was not there, nothing happens. */
    void removeConsumer(ImageConsumer ic);

    /** Registers it if need be and starts the delivery. */
    void startProduction(ImageConsumer ic);

    /**
     * Asks for the pixels to be sent again from top to bottom.
     *
     * <p>It is for the consumer that needs that order and did not get it the first time. The
     * producer may ignore it.
     */
    void requestTopDownLeftRightResend(ImageConsumer ic);
}
