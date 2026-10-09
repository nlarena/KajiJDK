package javax.imageio.event;

import java.util.EventListener;
import javax.imageio.ImageReader;

/**
 * KajiLibrary's javax.imageio.event.IIOReadProgressListener -- follows the progress of a read.
 *
 * <p>It exists because decoding a large image takes time, and {@code ImageReader.read} does not
 * return until it finishes. Without this there is no way to draw a progress bar or to know it is
 * still alive.
 *
 * <h2>The three pairs of events</h2>
 *
 * <p>Each pair opens and closes, and they nest:
 *
 * <ul>
 *   <li><b>sequence</b>: only in a read of several images at once. It wraps the others;
 *   <li><b>image</b>: one image. {@link #imageProgress} arrives several times in between, with a
 *       percentage from 0 to 100;
 *   <li><b>thumbnail</b>: the same, for embedded previews.
 * </ul>
 *
 * <p>{@link #readAborted} <b>replaces</b> the {@code complete} that would have come: if someone
 * called {@code ImageReader.abort()}, this one arrives and not that one. A program that only
 * listens to {@code imageComplete} to close its progress bar leaves it open forever on cancel.
 *
 * <p>The notifications arrive on the thread that is reading, not on the UI one. Blocking them
 * slows decoding down.
 */
public interface IIOReadProgressListener extends EventListener {

    /**
     * A read of several images begins.
     *
     * @param minIndex the index of the first one
     */
    void sequenceStarted(ImageReader source, int minIndex);

    /** The sequence finished. */
    void sequenceComplete(ImageReader source);

    /** An image begins. */
    void imageStarted(ImageReader source, int imageIndex);

    /** It is at that percentage, from 0 to 100. */
    void imageProgress(ImageReader source, float percentageDone);

    /** The image finished. */
    void imageComplete(ImageReader source);

    /** A thumbnail begins. */
    void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex);

    /** It is at that percentage. */
    void thumbnailProgress(ImageReader source, float percentageDone);

    /** The thumbnail finished. */
    void thumbnailComplete(ImageReader source);

    /** It was cancelled. See the class note: it comes instead of the {@code complete}. */
    void readAborted(ImageReader source);
}
