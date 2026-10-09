package javax.imageio.event;

import java.util.EventListener;
import javax.imageio.ImageWriter;

/**
 * KajiLibrary's javax.imageio.event.IIOWriteWarningListener -- reports a non-fatal problem while
 * writing.
 *
 * <p>The mirror of {@link IIOReadWarningListener}, with one more argument: <b>which</b> image,
 * because a file may hold several.
 *
 * <p>The typical warning when writing is a loss: metadata the target format cannot express, a
 * colour that does not fit in the palette, a precision that gets cut. The file comes out anyway,
 * and poorer than asked for.
 *
 * <p>As when reading, with no listeners registered that is silently lost -- which is worse when
 * writing, because the original may no longer exist.
 */
public interface IIOWriteWarningListener extends EventListener {

    /**
     * Something was lost and it could carry on. See the class note.
     *
     * @param imageIndex which image of the file
     */
    void warningOccurred(ImageWriter source, int imageIndex, String warning);
}
