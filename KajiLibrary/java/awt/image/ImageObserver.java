package java.awt.image;

import java.awt.Image;

/**
 * Whoever wants to hear that an image that is loading has made progress.
 *
 * <p>It is the asynchronous half of AWT's image model. An image that comes from the network is not
 * whole when it is asked for, so `getWidth` may answer -1 and tell later. The observer is the one
 * who is told.
 *
 * <p>The return value of {@link #imageUpdate} is usually read the wrong way round: returning `true`
 * means <strong>keep telling me</strong>, and `false`, that it is no longer of interest. An
 * observer that always returns `true` never unsubscribes.
 */
public interface ImageObserver {

    /** The width is known. */
    int WIDTH = 1;

    /** The height is known. */
    int HEIGHT = 2;

    /** The properties are known. */
    int PROPERTIES = 4;

    /** There are more pixels available than before. */
    int SOMEBITS = 8;

    /** A frame of a multi-frame image is finished. */
    int FRAMEBITS = 16;

    /** The image is complete. */
    int ALLBITS = 32;

    /** There was an error and the image will not be loadable. */
    int ERROR = 64;

    /** The load was aborted; it may be retried. */
    int ABORT = 128;

    /**
     * Tells that the image has made progress.
     *
     * @param infoflags the combination of flags that says what changed
     * @return `true` to go on receiving notices, `false` to stop receiving them
     */
    boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height);
}
