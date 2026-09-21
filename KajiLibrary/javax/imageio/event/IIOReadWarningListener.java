package javax.imageio.event;

import java.util.EventListener;
import javax.imageio.ImageReader;

/**
 * KajiLibrary's javax.imageio.event.IIOReadWarningListener -- reports a non-fatal problem while
 * reading.
 *
 * <p>A warning is something wrong in the file that the reader <b>could</b> recover from: a corrupt
 * metadata field, a checksum that does not match, an out-of-range value that was clipped. The image
 * comes out anyway.
 *
 * <p>With no listeners registered those warnings <b>are silently lost</b>, and that is this
 * interface's reason to be. A program that decodes files of unknown origin and does not register
 * one never learns that half its images came in broken.
 *
 * <p>When the reader localizes the message (the resource-bundle variant of
 * {@code processWarningOccurred}), it uses the locale set with {@code ImageReader.setLocale}.
 */
public interface IIOReadWarningListener extends EventListener {

    /** Something was wrong and it could carry on. See the class note. */
    void warningOccurred(ImageReader source, String warning);
}
