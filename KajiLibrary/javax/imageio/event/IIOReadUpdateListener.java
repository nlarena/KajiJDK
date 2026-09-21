package javax.imageio.event;

import java.awt.image.BufferedImage;
import java.util.EventListener;
import javax.imageio.ImageReader;

/**
 * KajiLibrary's javax.imageio.event.IIOReadUpdateListener -- hands over the half-decoded image.
 *
 * <p>The difference from {@link IIOReadProgressListener} is that that one says <b>how much</b> is
 * left and this one hands over <b>what there already is</b>. It is what allows showing an image
 * that takes shape while it downloads, instead of a bar.
 *
 * <h2>The image that arrives is the real one, and it is still changing</h2>
 *
 * <p>It is the important part and the one done wrong. The {@link BufferedImage} passed is the
 * read's real destination, not a copy: the decoder will keep writing into it as soon as the method
 * returns.
 *
 * <p>Keeping it and using it later gives an image that changes on its own. Drawing it from another
 * thread gives tearing. The only safe thing is to read it right there, or to copy it.
 *
 * <p>The six region parameters --{@code minX}, {@code minY}, {@code width}, {@code height} and the
 * two periods-- say which rectangle changed, so as not to have to redraw everything.
 *
 * <h2>Passes belong to multi-pass formats</h2>
 *
 * <p>{@link #passStarted} and {@link #passComplete} only come from a reader whose format fills the
 * image in several increasingly sharp rounds --a progressive JPEG, an interlaced PNG or GIF. A
 * reader of a single-pass format need not report passes at all.
 */
public interface IIOReadUpdateListener extends EventListener {

    /**
     * A pass begins. See the class note.
     *
     * @param theImage the real destination, which will keep changing
     * @param pass which pass
     * @param minPass the first one that will be done
     * @param maxPass the last one
     * @param bands which bands this pass touches
     */
    void passStarted(ImageReader source, BufferedImage theImage, int pass, int minPass,
                     int maxPass, int minX, int minY, int periodX, int periodY, int[] bands);

    /**
     * That rectangle of the image changed.
     *
     * @param periodX every how many pixels in X it was written, for interlaced passes
     */
    void imageUpdate(ImageReader source, BufferedImage theImage, int minX, int minY, int width,
                     int height, int periodX, int periodY, int[] bands);

    /** The pass finished. */
    void passComplete(ImageReader source, BufferedImage theImage);

    /** Same, for a thumbnail. */
    void thumbnailPassStarted(ImageReader source, BufferedImage theThumbnail, int pass,
                              int minPass, int maxPass, int minX, int minY, int periodX,
                              int periodY, int[] bands);

    /** Same. */
    void thumbnailUpdate(ImageReader source, BufferedImage theThumbnail, int minX, int minY,
                         int width, int height, int periodX, int periodY, int[] bands);

    /** Same. */
    void thumbnailPassComplete(ImageReader source, BufferedImage theThumbnail);
}
