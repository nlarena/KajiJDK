package javax.imageio.event;

import java.util.EventListener;
import javax.imageio.ImageWriter;

/**
 * KajiLibrary's javax.imageio.event.IIOWriteProgressListener -- follows the progress of a write.
 *
 * <p>The mirror of {@link IIOReadProgressListener}, with one difference: <b>there is no sequence
 * pair</b>. Several images are written one at a time with {@code writeToSequence}, so each call
 * opens and closes its own image pair.
 *
 * <p>{@link #writeAborted} replaces the {@code imageComplete} that would have come, as when reading
 * -- and here it matters more: a file whose write was cancelled is left half written, and has to be
 * deleted. A program that only listens to {@code imageComplete} leaves truncated files behind.
 */
public interface IIOWriteProgressListener extends EventListener {

    /** An image starts being written. */
    void imageStarted(ImageWriter source, int imageIndex);

    /** It is at that percentage, from 0 to 100. */
    void imageProgress(ImageWriter source, float percentageDone);

    /** It finished. */
    void imageComplete(ImageWriter source);

    /** A thumbnail begins. */
    void thumbnailStarted(ImageWriter source, int imageIndex, int thumbnailIndex);

    /** It is at that percentage. */
    void thumbnailProgress(ImageWriter source, float percentageDone);

    /** The thumbnail finished. */
    void thumbnailComplete(ImageWriter source);

    /** It was cancelled. See the class note: the file is left half written. */
    void writeAborted(ImageWriter source);
}
